package com.trval.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trval.common.ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class DeepSeekClient {

    private static final String DEFAULT_MODEL = "deepseek-flash";
    private static final Set<String> MODELS = Set.of("deepseek-flash", "deepseek-v4-pro");

    private final RestClient restClient;
    private final HttpClient streamingClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.streamingClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.deepseek.com")
                .requestFactory(requestFactory)
                .build();
    }

    public String complete(String apiKey, String model, String systemPrompt, String userPrompt) {
        String selected = normalize(model);
        Map<String, Object> body = requestBody(selected, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), false);

        String raw;
        try {
            raw = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new ApiException(502, errorMessage(exception));
        } catch (RestClientException exception) {
            throw new ApiException(504, "大模型响应超时");
        }

        return readContent(raw);
    }

    public void stream(String apiKey, String model, List<Map<String, String>> messages, Consumer<String> onDelta) {
        Map<String, Object> body = requestBody(normalize(model), messages, true);
        body.put("thinking", Map.of("type", "disabled"));
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (IOException exception) {
            throw new ApiException(500, "大模型请求失败");
        }

        HttpResponse<java.util.stream.Stream<String>> response;
        try {
            response = streamingClient.send(request, HttpResponse.BodyHandlers.ofLines());
        } catch (IOException exception) {
            throw new ApiException(504, "大模型响应超时");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(504, "大模型响应超时");
        }

        if (response.statusCode() != 200) {
            String raw = response.body().collect(Collectors.joining("\n"));
            throw new ApiException(502, errorText(response.statusCode(), raw));
        }

        response.body().forEach(line -> emitDelta(line, onDelta));
    }

    private void emitDelta(String line, Consumer<String> onDelta) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }
        try {
            String content = objectMapper.readTree(data)
                    .path("choices").path(0).path("delta").path("content").asText("");
            if (!content.isEmpty()) {
                onDelta.accept(content);
            }
        } catch (IOException exception) {
            throw new ApiException(502, "大模型返回的内容无法识别");
        }
    }

    private String normalize(String model) {
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private Map<String, Object> requestBody(String model, List<Map<String, String>> messages, boolean stream) {
        if (!MODELS.contains(model)) {
            throw new ApiException(400, "不支持的模型");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", stream);
        body.put("messages", messages);
        applyModelOptions(model, body);
        return body;
    }

    private void applyModelOptions(String model, Map<String, Object> body) {
        switch (model) {
            case "deepseek-flash", "deepseek-v4-pro" -> {
                // 这两个模型共用同一套请求。某个模型需要 thinking、reasoning_effort 等参数时加在这里。
            }
            default -> throw new ApiException(400, "不支持的模型");
        }
    }

    private String readContent(String raw) {
        try {
            JsonNode message = objectMapper.readTree(raw).path("choices").path(0).path("message");
            String content = message.path("content").asText("");
            if (content.isBlank()) {
                content = message.path("reasoning_content").asText("");
            }
            if (content.isBlank()) {
                throw new ApiException(502, "大模型没有返回内容");
            }
            return content;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(502, "大模型返回的内容无法识别");
        }
    }

    private String errorMessage(RestClientResponseException exception) {
        return errorText(exception.getStatusCode().value(), exception.getResponseBodyAsString());
    }

    private String errorText(int status, String raw) {
        if (status == 401) {
            return "API Key 无效";
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            String message = node.path("message").asText("");
            if (message.isBlank()) {
                message = node.path("error").path("message").asText("");
            }
            if (!message.isBlank()) {
                return message.length() > 80 ? message.substring(0, 80) : message;
            }
        } catch (Exception ignored) {
            return "大模型请求失败";
        }
        return "大模型请求失败";
    }
}
