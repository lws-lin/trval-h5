package com.trval.llm;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptDocument {

    private final String chatPrompt;
    private final String planPrompt;

    public PromptDocument() {
        String text = read("/prompts/travel-planner.md");
        String shared = section(text, "共同规则");
        this.chatPrompt = shared + "\n\n" + section(text, "对话");
        this.planPrompt = shared + "\n\n" + section(text, "行程生成");
    }

    public String chatPrompt() {
        return chatPrompt;
    }

    public String planPrompt() {
        return planPrompt;
    }

    private static String read(String path) {
        try (var input = PromptDocument.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("找不到提示词文档: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取提示词文档失败", exception);
        }
    }

    private static String section(String text, String title) {
        String marker = "## " + title;
        int start = text.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("提示词文档缺少章节: " + title);
        }
        int bodyStart = text.indexOf('\n', start);
        if (bodyStart < 0) {
            throw new IllegalStateException("提示词文档章节为空: " + title);
        }
        int next = text.indexOf("\n## ", bodyStart);
        String body = (next < 0 ? text.substring(bodyStart + 1) : text.substring(bodyStart + 1, next)).trim();
        if (body.isEmpty()) {
            throw new IllegalStateException("提示词文档章节为空: " + title);
        }
        return body;
    }
}
