package org.zcdada.shortlink_zc.admin.agent.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zcdada.shortlink_zc.admin.agent.AgentTools;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ModelRouter {

    private final ChatClient primaryClient;
    private final Map<String, ChatClient> fallbackClients;
    private final ModelHealthService healthService;

    public ModelRouter(ChatClient.Builder chatClientBuilder,
                       AgentTools agentTools,
                       MessageChatMemoryAdvisor chatMemoryAdvisor,
                       ModelHealthService healthService,
                       AgentModelProperties properties) {
        this.healthService = healthService;

        this.primaryClient = chatClientBuilder
                .defaultTools(agentTools)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
        this.fallbackClients = new LinkedHashMap<>();

        if (properties.getProviders() != null) {
            properties.getProviders().forEach((name, cfg) -> {
                if (!StringUtils.hasText(cfg.getBaseUrl()) || !StringUtils.hasText(cfg.getApiKey())) {
                    log.info("备选模型 {} 配置不完整，跳过", name);
                    return;
                }
                try {
                    OpenAiApi api = OpenAiApi.builder()
                            .baseUrl(cfg.getBaseUrl())
                            .apiKey(cfg.getApiKey())
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
                    log.info("注册备选模型: {} [{}]", name, cfg.getModel());
                } catch (Exception e) {
                    log.error("备选模型 {} 初始化失败: {}", name, e.getMessage());
                }
            });
        }
    }

    public Flux<String> routeStream(String systemPrompt, String userMessage,
                                    String conversationId, Map<String, Object> toolContext) {
        return tryStream(primaryClient, "primary", systemPrompt, userMessage, conversationId, toolContext)
                .onErrorResume(e -> tryFallbacks(systemPrompt, userMessage, conversationId, toolContext, e));
    }

    private Flux<String> tryFallbacks(String systemPrompt, String userMessage,
                                       String conversationId, Map<String, Object> toolContext,
                                       Throwable primaryError) {
        if (fallbackClients.isEmpty()) {
            return Flux.error(primaryError);
        }
        return Flux.fromIterable(fallbackClients.entrySet())
                .concatMap(entry -> {
                    if (!healthService.allowCall(entry.getKey())) {
                        log.warn("备选模型 {} 已熔断，跳过", entry.getKey());
                        return Flux.<String>empty();
                    }
                    log.info("切换到备选模型: {}", entry.getKey());
                    return tryStream(entry.getValue(), entry.getKey(), systemPrompt,
                            userMessage, conversationId, toolContext);
                })
                .switchIfEmpty(Flux.error(new RuntimeException("所有备选模型均已熔断")));
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
                .map(ModelRouter::normalizeChunk)
                .doOnNext(chunk -> healthService.markSuccess(modelName))
                .doOnError(e -> {
                    log.error("模型 {} 调用失败: {}", modelName, e.getMessage());
                    healthService.markFailure(modelName);
                });
    }

    /**
     * Fix common LLM formatting issues: missing newlines before markdown structural elements.
     * Applied per-chunk since patterns are short and always within a single token batch.
     */
    static String normalizeChunk(String chunk) {
        // Horizontal rule must be on its own line: 😄---🤖 → 😄\n\n---\n\n🤖
        String fixed = chunk.replaceAll("([^\\n|])---(?=[^\\n|])", "$1\n\n---\n\n");
        // Heading without space after # markers: ###text → ### text
        fixed = fixed.replaceAll("(#{1,6})(\\S)", "$1 $2");
        // Ensure newline before headings; exclude # itself to avoid double-matching
        fixed = fixed.replaceAll("([^#\\n])(#{1,6}\\s)", "$1\n\n$2");
        // Table row separator: |cell1|cell2||cell3|cell4| → |cell1|cell2|\n|cell3|cell4|
        fixed = fixed.replaceAll("\\|\\|", "|\n|");
        return fixed;
    }
}
