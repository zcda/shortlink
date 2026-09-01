package org.zcdada.shortlink_zc.admin.agent.router;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "short-link.agent.model")
public class AgentModelProperties {

    private Map<String, ModelNode> providers;

    /** 连续失败多少次后熔断 */
    private int failureThreshold = 3;

    /** 熔断打开后多少秒进入半开状态 */
    private int openDurationSeconds = 30;

    @Data
    public static class ModelNode {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int timeoutSeconds = 30;
        private int maxTokens = 4096;
    }
}
