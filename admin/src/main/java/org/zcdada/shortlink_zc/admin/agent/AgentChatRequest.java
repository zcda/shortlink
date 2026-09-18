package org.zcdada.shortlink_zc.admin.agent;

import lombok.Data;

@Data
public class AgentChatRequest {

    private String message;

    private String sessionId;

    /** 指定使用的模型 id：auto / deepseek / qwen / ollama；不传或 auto = 主模型优先、失败自动降级 */
    private String model;
}
