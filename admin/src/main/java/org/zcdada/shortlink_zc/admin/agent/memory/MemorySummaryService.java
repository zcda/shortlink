package org.zcdada.shortlink_zc.admin.agent.memory;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 对话记忆摘要服务。
 * 当对话历史超过阈值时，异步用 LLM 将旧消息压缩为摘要，
 * 后续对话 = 摘要(system消息) + 最近N轮，节省约 50% token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemorySummaryService {

    private final ChatClient.Builder chatClientBuilder;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final String SUMMARY_KEY_PREFIX = "ai-agent:summary:";
    private static final String LOCK_KEY_PREFIX = "ai-agent:summary-lock:";

    /** 触发摘要的最小消息轮数（一问一答 = 2 条消息 = 1 轮） */
    private static final int SUMMARY_MIN_MESSAGES = 20;

    private static final String COMPRESS_PROMPT = """
        请将以下对话历史压缩为一段简洁的摘要（不超过200字），保留关键信息：
        - 用户做了哪些操作（创建/查询/删除等）
        - 涉及的具体短链接、分组名称
        - 重要的数字和结论
        只输出摘要内容，不要包含"以下是摘要"等前缀。""";

    @Async
    public void compressIfNeeded(String conversationId, List<Message> messages) {
        if (messages == null || messages.size() < SUMMARY_MIN_MESSAGES) {
            return;
        }

        String lockKey = LOCK_KEY_PREFIX + conversationId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }

            String existingKey = SUMMARY_KEY_PREFIX + conversationId;
            String existing = redisTemplate.opsForValue().get(existingKey);
            if (existing != null) {
                return;
            }

            String history = buildHistoryText(messages);
            String summary = chatClientBuilder.build()
                    .prompt()
                    .user(COMPRESS_PROMPT + "\n\n对话历史：\n" + history)
                    .options(OpenAiChatOptions.builder()
                            .model("deepseek-chat")
                            .temperature(0.3)
                            .maxTokens(300)
                            .build())
                    .call()
                    .content();

            if (StrUtil.isNotBlank(summary)) {
                SummaryRecord record = SummaryRecord.builder()
                        .summary(summary)
                        .compressedMessageCount(messages.size())
                        .build();
                redisTemplate.opsForValue().set(existingKey, JSON.toJSONString(record),
                        Duration.ofDays(7));
                log.info("对话摘要生成完成: conversationId={}, 压缩{}条消息, 摘要长度={}",
                        conversationId, messages.size(), summary.length());
            }
        } catch (Exception e) {
            log.warn("对话摘要生成失败: conversationId={}", conversationId, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 加载摘要，包装为 SystemMessage 置顶
     */
    public SystemMessage loadSummary(String conversationId) {
        try {
            String json = redisTemplate.opsForValue().get(SUMMARY_KEY_PREFIX + conversationId);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            SummaryRecord record = JSON.parseObject(json, SummaryRecord.class);
            if (record != null && StrUtil.isNotBlank(record.getSummary())) {
                return new SystemMessage("历史对话摘要: " + record.getSummary());
            }
        } catch (Exception e) {
            log.warn("加载摘要失败: conversationId={}", conversationId, e);
        }
        return null;
    }

    private String buildHistoryText(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String role = msg instanceof AssistantMessage ? "助手" : "用户";
            String text = msg.getText();
            if (StrUtil.isNotBlank(text)) {
                sb.append(role).append(": ").append(text).append("\n");
            }
        }
        return sb.toString();
    }

    @Data
    @Builder
    public static class SummaryRecord {
        private String summary;
        @JSONField(name = "compressed_count")
        private int compressedMessageCount;
    }
}
