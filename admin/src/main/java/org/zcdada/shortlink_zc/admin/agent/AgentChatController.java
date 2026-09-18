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
        String model = request.getModel();
        String modelUsed = (model == null || model.isBlank()) ? ModelRouter.AUTO_MODEL : model.trim();
        log.info("Agent chat: user={} sessionId={} model={} msgLen={} traceId={}",
                username, request.getSessionId(), modelUsed, request.getMessage().length(), traceId);

        Map<String, Object> meta = Map.of(
                "traceId", traceId,
                "model", modelUsed
        );
        Flux<ServerSentEvent<String>> metaEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("meta")
                        .data(JSON.toJSONString(meta))
                        .build()
        );
        //实际请求
        Flux<ServerSentEvent<String>> contentEvents = modelRouter.routeStream(
                AgentConfig.SYSTEM_PROMPT,
                request.getMessage(),
                conversationId,
                Map.of("userInfo", currentUser),
                request.getModel())
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
                    String errorMsg;
                    if (e instanceof IllegalArgumentException) {
                        // 用户选错/未知模型等业务性错误，直接告知
                        errorMsg = e.getMessage();
                    } else {
                        log.error("Agent chat 调用失败: model={} traceId={}", modelUsed, traceId, e);
                        errorMsg = "当前模型调用失败，请稍后重试或切换其他模型";
                    }
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorMsg)
                            .build());
                });
    }
}
