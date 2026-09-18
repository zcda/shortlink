package org.zcdada.shortlink_zc.admin.agent.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zcdada.shortlink_zc.admin.agent.AgentTools;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ModelRouter {

    /** 用户不指定模型时的行为：主模型优先，失败后按熔断状态依次尝试备选 */
    public static final String AUTO_MODEL = "auto";

    /** 主模型(deepseek)面向用户的 id，与备选一样可被显式选择 */
    public static final String PRIMARY_MODEL_ID = "deepseek";

    private final ChatClient primaryClient;
    private final Map<String, ChatClient> fallbackClients;
    /** 可被用户显式选择的全部模型：deepseek(主) + 各备选，key 即面向用户的模型 id */
    private final Map<String, ChatClient> selectableClients;
    /** 模型 id → 底层真实模型名（用于展示） */
    private final Map<String, String> selectableModelNames;
    private final ModelHealthService healthService;

    public ModelRouter(ChatClient.Builder chatClientBuilder,
                       AgentTools agentTools,
                       MessageChatMemoryAdvisor chatMemoryAdvisor,
                       ModelHealthService healthService,
                       AgentModelProperties properties,
                       @Value("${spring.ai.openai.chat.options.model:deepseek}") String primaryModelName) {
        this.healthService = healthService;

        this.primaryClient = chatClientBuilder
                .defaultTools(agentTools)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
        this.fallbackClients = new LinkedHashMap<>();
        this.selectableModelNames = new LinkedHashMap<>();
        this.selectableModelNames.put(PRIMARY_MODEL_ID, primaryModelName);

        if (properties.getProviders() != null) {
            // 按 order 升序注册，fallbackClients 的迭代顺序即备选尝试顺序（相同 order 保持配置书写顺序）
            properties.getProviders().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                            Comparator.comparingInt(AgentModelProperties.ModelNode::getOrder)))
                    .forEach(entry -> {
                        String name = entry.getKey();
                        AgentModelProperties.ModelNode cfg = entry.getValue();
                        boolean hasBaseUrl = StringUtils.hasText(cfg.getBaseUrl());
                        boolean hasApiKey = StringUtils.hasText(cfg.getApiKey());
                        if (!hasBaseUrl || (!hasApiKey && cfg.isApiKeyRequired())) {
                            log.info("备选模型 {} 配置不完整{}，跳过",
                                    name, cfg.isApiKeyRequired() ? "（缺少 base-url 或 api-key）" : "（缺少 base-url）");
                            return;
                        }
                        try {
                            // 本地模型（Ollama 等）不鉴权，缺 api-key 时填占位符即可通过 OpenAI 协议客户端校验
                            String apiKey = hasApiKey ? cfg.getApiKey() : "local-model";
                            OpenAiApi api = OpenAiApi.builder()
                                    .baseUrl(cfg.getBaseUrl())
                                    .apiKey(apiKey)
                                    .build();
                            OpenAiChatOptions options = OpenAiChatOptions.builder()
                                    .model(cfg.getModel())
                                    .maxTokens(cfg.getMaxTokens())
                                    .build();
                            ChatModel model = OpenAiChatModel.builder()
                                    .openAiApi(api)
                                    .defaultOptions(options)
                                    .build();
                            ChatClient client = ChatClient.builder(model)
                                    .defaultTools(agentTools)
                                    .defaultAdvisors(chatMemoryAdvisor)
                                    .build();
                            fallbackClients.put(name, client);
                            selectableModelNames.put(name, cfg.getModel());
                            log.info("注册备选模型: {} [{}] (order={})", name, cfg.getModel(), cfg.getOrder());
                        } catch (Exception e) {
                            log.error("备选模型 {} 初始化失败: {}", name, e.getMessage());
                        }
                    });
        }
        // 可选模型全集 = 主模型 + 备选（顺序：主模型在前，备选按 order 已排序）
        this.selectableClients = new LinkedHashMap<>();
        this.selectableClients.put(PRIMARY_MODEL_ID, primaryClient);
        this.selectableClients.putAll(fallbackClients);
        log.info("可选模型列表: {}", selectableClients.keySet());
    }

    public Flux<String> routeStream(String systemPrompt, String userMessage,
                                    String conversationId, Map<String, Object> toolContext) {
        return routeStream(systemPrompt, userMessage, conversationId, toolContext, null);
    }

    /**
     * 路由入口。
     * model 为空或 "auto"：主模型优先，失败按熔断顺序降级到备选；
     * model 指定具体 id（deepseek/qwen/ollama...）：只调用该模型，失败直接报错（不自动降级）。
     */
    public Flux<String> routeStream(String systemPrompt, String userMessage,
                                    String conversationId, Map<String, Object> toolContext,
                                    String model) {
        String target = StringUtils.hasText(model) ? model.trim() : AUTO_MODEL;
        if (!AUTO_MODEL.equals(target)) {
            ChatClient client = selectableClients.get(target);
            if (client == null) {
                return Flux.error(new IllegalArgumentException(
                        "未知的模型: " + target + "，可选: " + selectableClients.keySet()));
            }
            log.info("Agent 用户指定模型: {} ({})", target, selectableModelNames.get(target));
            return tryStream(client, target, systemPrompt, userMessage, conversationId, toolContext);
        }
        return tryStream(primaryClient, PRIMARY_MODEL_ID, systemPrompt, userMessage, conversationId, toolContext)
                .onErrorResume(e -> tryFallbacks(systemPrompt, userMessage, conversationId, toolContext, e));
    }

    /** 返回可选模型列表（含实时熔断状态），供前端渲染选择器 */
    public List<Map<String, Object>> listModels() {
        List<Map<String, Object>> list = new ArrayList<>();
        selectableClients.forEach((id, client) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("name", PRIMARY_MODEL_ID.equals(id) ? "DeepSeek" : id);
            item.put("model", selectableModelNames.get(id));
            item.put("state", healthService.getState(id));
            list.add(item);
        });
        return list;
    }

    private Flux<String> tryFallbacks(String systemPrompt, String userMessage,
                                       String conversationId, Map<String, Object> toolContext,
                                       Throwable primaryError) {
        if (fallbackClients.isEmpty()) {
            return Flux.error(primaryError);
        }
        // 记录最近一次真实错误，用于全部失败后的最终提示
        AtomicReference<Throwable> lastError = new AtomicReference<>(primaryError);
        return Flux.fromIterable(fallbackClients.entrySet())
                .concatMap(entry -> {
                    if (!healthService.allowCall(entry.getKey())) {
                        log.warn("备选模型 {} 已熔断，跳过", entry.getKey());
                        return Flux.<String>empty();
                    }
                    log.info("切换到备选模型: {}", entry.getKey());
                    return tryStream(entry.getValue(), entry.getKey(), systemPrompt,
                            userMessage, conversationId, toolContext)
                            // 当前备选调用失败 → 记录错误并向下一个备选轮转，而不是弄死整条链
                            .onErrorResume(e -> {
                                lastError.set(e);
                                log.warn("备选模型 {} 调用失败，尝试下一个: {}",
                                        entry.getKey(), e.getMessage());
                                return Flux.empty();
                            });
                })
                .switchIfEmpty(Flux.defer(() -> Flux.error(
                        new RuntimeException("所有备选模型均不可用", lastError.get()))));
    }

    private Flux<String> tryStream(ChatClient client, String modelName,
                                    String systemPrompt, String userMessage,
                                    String conversationId, Map<String, Object> toolContext) {
        return client.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(toolContext)
                .stream()
                .content()
                // 注意：不要对单个流式分片做 Markdown 正则改写。
                // 分片常是半行/半表格，逐片改写会破坏表格等结构（历史上为此出现过渲染错乱）。
                // 完整回复由前端在流结束后用 marked 整体渲染。
                .doOnNext(chunk -> healthService.markSuccess(modelName))
                .doOnError(e -> {
                    log.error("模型 {} 调用失败: {}", modelName, e.getMessage());
                    healthService.markFailure(modelName);
                });
    }
}
