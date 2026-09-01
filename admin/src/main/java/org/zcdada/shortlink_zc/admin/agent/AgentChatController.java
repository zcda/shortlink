package org.zcdada.shortlink_zc.admin.agent;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.agent.router.ModelRouter;
import org.zcdada.shortlink_zc.admin.agent.trace.AgentTrace;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserInfoDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentChatController {

    private final ModelRouter modelRouter;

    @AgentTrace
    @PostMapping(value = "/api/short-link/admin/v1/agent/chat",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AgentChatRequest request) {
        UserInfoDTO currentUser = UserContext.getUser();
        String username = currentUser != null ? currentUser.getUsername() : "anonymous";
        String conversationId = username + ":" + request.getSessionId();

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Agent chat: user={} sessionId={} msgLen={} traceId={}",
                username, request.getSessionId(), request.getMessage().length(), traceId);

        Map<String, Object> meta = Map.of(
                "traceId", traceId,
                "model", "deepseek-v4-pro"
        );
        Flux<ServerSentEvent<String>> metaEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("meta")
                        .data(JSON.toJSONString(meta))
                        .build()
        );

        Flux<ServerSentEvent<String>> contentEvents = modelRouter.routeStream(
                AgentConfig.SYSTEM_PROMPT,
                request.getMessage(),
                conversationId,
                Map.of("userInfo", currentUser))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());

        Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build());

        return Flux.concat(metaEvent, contentEvents, doneEvent)
                .onErrorResume(e -> {
                    log.error("Agent chat 所有模型均调用失败: traceId={}", traceId, e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("所有模型暂时不可用，请稍后重试")
                            .build());
                });
    }
}
