package org.zcdada.shortlink_zc.admin.agent.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 模型健康熔断器，基于 Redis 实现三态断路器。
 * <ul>
 *   <li>CLOSED: 正常调用，连续失败达到阈值 → OPEN</li>
 *   <li>OPEN: 拒绝调用，等待冷却时间 → HALF_OPEN</li>
 *   <li>HALF_OPEN: 放行一次探测，成功 → CLOSED，失败 → OPEN</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHealthService {

    private final StringRedisTemplate redisTemplate;
    private final AgentModelProperties properties;

    private static final String KEY_PREFIX = "ai-agent:model-health:";

    public boolean allowCall(String modelName) {
        String failKey = KEY_PREFIX + modelName + ":fails";
        String openKey = KEY_PREFIX + modelName + ":open";
        String openFlag = redisTemplate.opsForValue().get(openKey);
        if (openFlag == null) {
            return true;
        }
        long openTime = Long.parseLong(openFlag);
        long elapsed = System.currentTimeMillis() - openTime;
        long openMs = properties.getOpenDurationSeconds() * 1000L;
        if (elapsed >= openMs) {
            redisTemplate.delete(openKey);
            redisTemplate.opsForValue().set(KEY_PREFIX + modelName + ":half-open", "1",
                    properties.getOpenDurationSeconds(), TimeUnit.SECONDS);
            log.info("模型 {} 进入半开探测状态", modelName);
            return true;
        }
        return false;
    }

    public void markSuccess(String modelName) {
        String failKey = KEY_PREFIX + modelName + ":fails";
        String openKey = KEY_PREFIX + modelName + ":open";
        String halfOpenKey = KEY_PREFIX + modelName + ":half-open";
        redisTemplate.delete(failKey);
        redisTemplate.delete(openKey);
        redisTemplate.delete(halfOpenKey);
    }

    public void markFailure(String modelName) {
        String failKey = KEY_PREFIX + modelName + ":fails";
        Long fails = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, properties.getOpenDurationSeconds() * 2L, TimeUnit.SECONDS);
        if (fails != null && fails >= properties.getFailureThreshold()) {
            String openKey = KEY_PREFIX + modelName + ":open";
            redisTemplate.opsForValue().set(openKey, String.valueOf(System.currentTimeMillis()),
                    properties.getOpenDurationSeconds() * 2L, TimeUnit.SECONDS);
            log.warn("模型 {} 熔断已打开，连续失败 {} 次", modelName, fails);
        }
    }
}
