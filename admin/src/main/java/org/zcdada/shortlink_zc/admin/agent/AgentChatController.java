package org.zcdada.shortlink_zc.admin.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserInfoDTO;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentChatController {

    private final ChatClient chatClient;

    @PostMapping(value = "/api/short-link/admin/v1/agent/chat",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AgentChatRequest request) {
        UserInfoDTO currentUser = UserContext.getUser();
        String username = currentUser != null ? currentUser.getUsername() : "anonymous";
        String conversationId = username + ":" + request.getSessionId();
        log.info("Agent chat: user={}, sessionId={}, msgLen={}",
                username, request.getSessionId(), request.getMessage().length());

        return chatClient.prompt()
                .system(AgentConfig.SYSTEM_PROMPT)
                .user(request.getMessage())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userInfo", currentUser))
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()))
                .onErrorResume(e -> {
                    log.error("Agent chat stream error", e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("服务繁忙，请稍后重试")
                            .build());
                });
    }
}
