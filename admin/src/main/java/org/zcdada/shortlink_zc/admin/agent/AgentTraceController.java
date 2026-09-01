package org.zcdada.shortlink_zc.admin.agent;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AgentTraceController {

    private final StringRedisTemplate redisTemplate;

    private static final String TRACE_KEY = "ai-agent:traces";

    @GetMapping("/api/short-link/admin/v1/agent/traces")
    public List<Object> getTraces() {
        List<String> jsons = redisTemplate.opsForList().range(TRACE_KEY, 0, 19);
        if (jsons == null || jsons.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> traces = new ArrayList<>();
        for (String json : jsons) {
            try {
                traces.add(JSON.parse(json));
            } catch (Exception ignored) {
            }
        }
        return traces;
    }

    @GetMapping("/api/short-link/admin/v1/agent/health")
    public java.util.Map<String, Object> getHealth() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        // 统计今日调用次数
        List<String> jsons = redisTemplate.opsForList().range(TRACE_KEY, 0, -1);
        long total = jsons != null ? jsons.size() : 0;
        long success = 0;
        long fail = 0;
        if (jsons != null) {
            for (String json : jsons) {
                if (json.contains("\"success\":true")) success++;
                else if (json.contains("\"success\":false")) fail++;
            }
        }
        result.put("totalCalls", total);
        result.put("success", success);
        result.put("fail", fail);
        return result;
    }
}
