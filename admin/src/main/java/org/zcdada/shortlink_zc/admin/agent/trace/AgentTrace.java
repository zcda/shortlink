package org.zcdada.shortlink_zc.admin.agent.trace;

import java.lang.annotation.*;

/**
 * 标记 Agent 调用方法，自动记录全链路追踪信息。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentTrace {
}
