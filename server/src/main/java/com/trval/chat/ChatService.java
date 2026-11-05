package com.trval.chat;

import com.trval.common.ApiException;
import com.trval.llm.DeepSeekClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class ChatService {

    private static final String MARKER = "[[a2ui]]";

    private static final String SYSTEM_PROMPT = """
            你是旅游规划助手，只处理旅行相关的问题：出发地、目的地、天数、预算、交通、住宿、景点、行程节奏和同行人。
            如果用户问的不是旅游规划，用一两句话说明你只能帮忙规划旅行，并请对方说出出发地或目的地。不要回答无关问题，不要顺着无关话题展开。
            你的目标是确认这些信息：出发地、目的地、预算、天数。用户提到的其他偏好，例如人群、节奏、必去景点、饮食和住宿标准，记到其他需求里。
            每次只追问还缺少的一两项。不要编造用户没说过的信息，也不要重复追问已经确认的内容。
            先用自然语言回复。回复结束后另起一行，输出且只输出一次下面的标记和 JSON，不要使用 Markdown 代码块：
            [[a2ui]]{"suggestions":["短句1","短句2"],"collected":{"departText":"","destText":"","budget":"","days":"","extra":""}}
            suggestions 是用户可能直接点选的下一句回复，最多 4 条，每条不超过 16 个字。
            页面上的「已确认的行程信息」只展示 collected，不会从自然语言里提取。
            每一轮的 collected 必须带上全部已确认字段，包括之前几轮已经确认的，不能只写本轮新增的那一项。
            字段对应关系：出发地是 departText，目的地是 destText，预算是 budget，天数是 days，其他需求是 extra。
            还没确认的字段用空字符串。预算和天数只保留数字。
            例如已经确认北京出发、上海、3 天，本轮还不知道预算，则必须输出：
            [[a2ui]]{"suggestions":["预算3000","预算5000"],"collected":{"departText":"北京","destText":"上海","budget":"","days":"3","extra":""},"plan":{"summary":"从北京前往上海的3日行程。","schedule":[{"day":1,"title":"抵达上海","content":"抵达后安顿，并在住处附近散步。"}]}}
            出发地和目的地都已确认后必须带上 plan。summary 是 80 字以内概要。schedule 按天数列出，每项有 day、title、content。
            plan 必须和这段自然语言里的每天安排一致，不要另写一套。天数未确认时按 3 天写，最多 15 天。出发地或目的地未确认时不要输出 plan。
            """;

    private final DeepSeekClient deepSeekClient;

    public ChatService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
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
        return SYSTEM_PROMPT + """

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
