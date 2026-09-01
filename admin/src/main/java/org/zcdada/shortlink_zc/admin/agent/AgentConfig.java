package org.zcdada.shortlink_zc.admin.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;
import org.zcdada.shortlink_zc.admin.agent.trace.AgentTraceAspect;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableAsync
@Slf4j
public class AgentConfig {

    static final String SYSTEM_PROMPT = """
        你是短链接管理助手，可以帮用户完成以下操作：
        - 创建短链接：将长 URL 转换为短链接
        - 查询短链接列表：查看某个分组下的短链接
        - 查看访问统计：查看单个短链或整体的访问数据
        - 管理回收站：查看已删除的短链接
        - 查看分组信息：列出用户的所有分组

        规则：
        1. 创建短链接时，必须先确认用户提供了完整 URL（包含 https://）
        2. 如果用户没有指定分组名称，先调用 getGroupList 获取可用分组
        3. 统计数据用通俗易懂的语言解读，不要直接输出原始 JSON
        4. 操作成功时简要告知结果（如短链接地址），失败时说明原因
        5. 始终用中文回复，语气友好简洁
        6. 如果用户的请求超出你的能力范围，礼貌说明并建议替代方案
        7. 列表类数据用 Markdown 表格展示，增强可读性
        """;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .filter((request, next) -> next.exchange(request)
                        .flatMap(response -> {
                            if (response.statusCode().is5xxServerError()) {
                                return response.createError();
                            }
                            return Mono.just(response);
                        })
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                                .maxBackoff(Duration.ofSeconds(15))
                                .doBeforeRetry(signal -> log.warn("DeepSeek API retry {}/{}, error: {}",
                                        signal.totalRetries() + 1, 3,
                                        signal.failure() != null ? signal.failure().getMessage() : "unknown"))));
    }

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate,
                                  @Value("${short-link.agent.session-ttl-days:7}") int ttlDays) {
        return new RedisChatMemory(redisTemplate, ttlDays);
    }

    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, AgentTools agentTools, MessageChatMemoryAdvisor chatMemoryAdvisor) {
        return builder
                .defaultTools(agentTools)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
    }

    @Bean
    public AgentTraceAspect agentTraceAspect(StringRedisTemplate redisTemplate) {
        return new AgentTraceAspect(redisTemplate);
    }
}
