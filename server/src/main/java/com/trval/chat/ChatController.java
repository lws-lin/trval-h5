package com.trval.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trval.common.ApiException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "chat-stream");
        thread.setDaemon(true);
        return thread;
    });

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Model", required = false) String model
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String apiKey = apiKey(authorization);
        executor.execute(() -> stream(emitter, request, apiKey, model));
        return emitter;
    }

    private void stream(SseEmitter emitter, ChatRequest request, String apiKey, String model) {
        try {
            if (apiKey.isBlank()) {
                send(emitter, Map.of("error", "请先填写 API Key"));
                emitter.complete();
                return;
            }
            chatService.stream(request, apiKey, model, delta -> send(emitter, Map.of("delta", delta)));
            send(emitter, Map.of("done", true));
            emitter.complete();
        } catch (ApiException exception) {
            finishWithError(emitter, exception.getMessage());
        } catch (Exception exception) {
            finishWithError(emitter, "回复失败了，请再试一次");
        }
    }

    private void finishWithError(SseEmitter emitter, String message) {
        try {
            send(emitter, Map.of("error", message));
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    private void send(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new ApiException(500, "回复失败了，请再试一次");
        }
    }

    private String apiKey(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }
}
