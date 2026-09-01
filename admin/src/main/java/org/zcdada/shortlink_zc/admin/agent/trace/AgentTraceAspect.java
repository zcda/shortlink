package org.zcdada.shortlink_zc.admin.agent.trace;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Agent 调用全链路追踪切面。
 * 记录每次 Agent 调用的用户、问题、耗时、成功/失败等信息，
 * 存入 Redis List 并输出结构化日志。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AgentTraceAspect {

    private final StringRedisTemplate redisTemplate;

    private static final String TRACE_KEY = "ai-agent:traces";

    private static final int MAX_TRACES = 200;

    @Around("@annotation(org.zcdada.shortlink_zc.admin.agent.trace.AgentTrace)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        long start = System.currentTimeMillis();
        boolean[] failed = {false};
        StringBuilder errorMsg = new StringBuilder();

        try {
            Object result = joinPoint.proceed();

            if (result instanceof Flux<?> flux) {
                return flux
                        .doOnComplete(() -> recordTrace(start, traceId, false, null))
                        .doOnError(e -> {
                            failed[0] = true;
                            if (e.getMessage() != null) {
                                errorMsg.append(e.getMessage());
                            }
                            recordTrace(start, traceId, true, errorMsg.toString());
                        });
            }

            recordTrace(start, traceId, false, null);
            return result;

        } catch (Exception e) {
            recordTrace(start, traceId, true, e.getMessage());
            throw e;
        }
    }

    private void recordTrace(long start, String traceId, boolean failed, String error) {
        long duration = System.currentTimeMillis() - start;
        String status = failed ? "FAIL" : "OK";

        log.info("[AgentTrace] traceId={} duration={}ms status={} err={}",
                traceId, duration, status, error);

        try {
            AgentTraceRecord record = AgentTraceRecord.builder()
                    .traceId(traceId)
                    .durationMs(duration)
                    .success(!failed)
                    .errorMsg(error)
                    .build();
            String json = JSON.toJSONString(record);
            redisTemplate.opsForList().leftPush(TRACE_KEY, json);
            redisTemplate.opsForList().trim(TRACE_KEY, 0, MAX_TRACES - 1);
            redisTemplate.expire(TRACE_KEY, 7, TimeUnit.DAYS);
        } catch (Exception ignored) {
        }
    }
}
