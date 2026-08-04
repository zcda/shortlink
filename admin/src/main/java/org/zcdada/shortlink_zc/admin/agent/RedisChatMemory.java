package org.zcdada.shortlink_zc.admin.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 ChatMemory 实现，带 TTL 自动过期与消息窗口限制。
 */
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "ai-agent:chat-memory:";

    /**
     * 每个会话最多保留的消息条数，防止历史无限膨胀导致 token 超限
     */
    private static final int MAX_MESSAGES = 40;

    private final StringRedisTemplate redisTemplate;

    private final int ttlDays;

    public RedisChatMemory(StringRedisTemplate redisTemplate, int ttlDays) {
        this.redisTemplate = redisTemplate;
        this.ttlDays = ttlDays;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        for (Message message : messages) {
            // 工具调用消息没有文本内容，跳过避免存入 null
            String text = message.getText();
            if (StrUtil.isBlank(text)) {
                continue;
            }
            String json = JSON.toJSONString(toStorage(message));
            redisTemplate.opsForList().rightPush(key, json);
        }
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> jsons = redisTemplate.opsForList().range(key, 0, -1);
        if (jsons == null || jsons.isEmpty()) {
            return Collections.emptyList();
        }
        List<Message> messages = new ArrayList<>();
        for (String json : jsons) {
            StorageMsg sm = JSON.parseObject(json, StorageMsg.class);
            if (sm != null && StrUtil.isNotBlank(sm.getContent())) {
                if ("USER".equals(sm.getType())) {
                    messages.add(new UserMessage(sm.getContent()));
                } else {
                    messages.add(new AssistantMessage(sm.getContent()));
                }
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private StorageMsg toStorage(Message message) {
        String type = message.getMessageType() != null ? message.getMessageType().name() : "USER";
        return new StorageMsg(type, message.getText());
    }

    public static class StorageMsg {
        private String type;
        private String content;

        public StorageMsg() {}

        public StorageMsg(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
