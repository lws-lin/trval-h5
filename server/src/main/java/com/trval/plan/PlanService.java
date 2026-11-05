package com.trval.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trval.common.ApiException;
import com.trval.llm.DeepSeekClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanService {

    private static final String SYSTEM_PROMPT = """
            你是旅行规划助手。根据用户给出的出发地、目的地、预算和天数，生成一份可以执行的行程。
            只返回 JSON，不要使用 Markdown，不要附加解释。
            JSON 结构如下：
            {"summary":"80字以内的行程概要","schedule":[{"day":1,"title":"12字以内的当天主题","content":"当天的交通、活动和用餐安排，120字以内"}]}
            schedule 的条数必须等于要求的天数，day 从 1 开始连续递增。
            天数大于 1 时，最后一天安排返程。
            预算要体现在交通、住宿或活动的取舍上。如果提供了其他需求，行程里要体现。使用中文。
            """;

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public PlanService(DeepSeekClient deepSeekClient, ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
    }

    public PlanResult create(PlanRequest request, String apiKey, String model) {
        int count = normalizeDays(request.days());
        String budget = request.budget() == null || request.budget().isBlank()
                ? "未填写"
                : request.budget().trim() + " 元";
        String extra = request.extra() == null || request.extra().isBlank()
                ? "未填写"
                : request.extra().trim();
        String userPrompt = """
                出发地：%s
                目的地：%s
                预算：%s
                天数：%d
                其他需求：%s
                """.formatted(request.departText(), request.destText(), budget, count, extra);
        String content = deepSeekClient.complete(apiKey, model, SYSTEM_PROMPT, userPrompt);
        return toResult(request, content);
    }

    private PlanResult toResult(PlanRequest request, String content) {
        JsonNode root = readJson(content);
        String summary = root.path("summary").asText("").trim();
        JsonNode scheduleNode = root.path("schedule");
        if (summary.isEmpty() || !scheduleNode.isArray()) {
            throw new ApiException(502, "大模型返回的行程无法识别");
        }

        List<ScheduleDay> schedule = new ArrayList<>();
        int index = 0;
        for (JsonNode item : scheduleNode) {
            index++;
            String title = item.path("title").asText("").trim();
            String dayContent = item.path("content").asText("").trim();
            if (title.isEmpty() || dayContent.isEmpty()) {
                continue;
            }
            int day = item.path("day").asInt(index);
            schedule.add(new ScheduleDay(day, title, dayContent));
        }
        if (schedule.isEmpty()) {
            throw new ApiException(502, "大模型返回的行程无法识别");
        }

        return new PlanResult(
                request.departText(),
                request.destText(),
                request.budget(),
                request.days(),
                summary,
                schedule
        );
    }

    private JsonNode readJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                text = text.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ApiException(502, "大模型返回的行程无法识别");
        }
        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception exception) {
            throw new ApiException(502, "大模型返回的行程无法识别");
        }
    }

    private int normalizeDays(String days) {
        int value = 3;
        if (days != null && !days.isBlank()) {
            try {
                value = Integer.parseInt(days.trim());
            } catch (NumberFormatException ignored) {
                value = 3;
            }
        }
        return Math.min(Math.max(value, 1), 15);
    }
}
