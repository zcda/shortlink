package org.zcdada.shortlink_zc.admin.agent.trace;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentTraceRecord {

    private String traceId;

    private String user;

    private String sessionId;

    private String question;

    private int questionLen;

    private String answer;

    private int answerLen;

    private long durationMs;

    private String modelUsed;

    private boolean degraded;

    private boolean success;

    private String errorMsg;
}
