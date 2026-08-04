package org.zcdada.shortlink_zc.admin.agent;

import lombok.Data;

@Data
public class AgentChatRequest {

    private String message;

    private String sessionId;
}
