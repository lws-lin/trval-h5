package com.trval.chat;

import com.trval.common.ApiException;
import com.trval.llm.DeepSeekClient;
import com.trval.llm.PromptDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class ChatService {

    private static final String MARKER = "[[a2ui]]";

    private final DeepSeekClient deepSeekClient;
    private final PromptDocument promptDocument;

    public ChatService(DeepSeekClient deepSeekClient, PromptDocument promptDocument) {
        this.deepSeekClient = deepSeekClient;
        this.promptDocument = promptDocument;
    }

    public void stream(ChatRequest request, String apiKey, String model, Consumer<String> onDelta) {
        deepSeekClient.stream(apiKey, model, messages(request), onDelta);
    }

    private List<Map<String, String>> messages(ChatRequest request) {
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new ApiException(400, "请输入内容");
        }
        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", systemPrompt(request.collected())));
        int start = Math.max(0, request.messages().size() - 20);
        for (int index = start; index < request.messages().size(); index++) {
            ChatMessage message = request.messages().get(index);
            if (message == null || message.content() == null || message.role() == null) {
                continue;
            }
            String role = message.role().trim();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String content = message.content().trim();
            int marker = content.indexOf(MARKER);
            if (marker >= 0) {
                content = content.substring(0, marker).trim();
            }
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > 2000) {
                content = content.substring(0, 2000);
            }
            payload.add(Map.of("role", role, "content", content));
        }
        if (payload.size() == 1) {
            throw new ApiException(400, "请输入内容");
        }
        return payload;
    }

    private String systemPrompt(TripContext collected) {
        TripContext context = collected == null
                ? new TripContext(null, null, null, null, null)
                : collected;
        return promptDocument.chatPrompt() + """

                目前已经确认。不是「未提供」的项，必须原样写入本轮 collected：
                出发地：%s
                目的地：%s
                预算：%s
                天数：%s
                其他需求：%s
                """.formatted(
                value(context.departText()),
                value(context.destText()),
                value(context.budget()),
                value(context.days()),
                value(context.extra())
        );
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "未提供" : text.trim();
    }
}
