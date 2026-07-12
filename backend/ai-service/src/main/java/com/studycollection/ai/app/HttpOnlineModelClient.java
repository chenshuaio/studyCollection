package com.studycollection.ai.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpOnlineModelClient implements OnlineModelClient {
    private final HttpClient httpClient;
    private final AiSettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final Duration requestTimeout;

    public HttpOnlineModelClient() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new AiSettingsService(
                        new InMemoryAiModelSettingsRepository(),
                        new AiEnvironmentConfig(
                                System.getenv("STUDY_COLLECTION_AI_ENDPOINT"),
                                System.getenv("STUDY_COLLECTION_AI_MODEL"),
                                System.getenv("STUDY_COLLECTION_AI_API_KEY")
                        )
                )
        );
    }

    HttpOnlineModelClient(HttpClient httpClient, AiSettingsService settingsService) {
        this(httpClient, settingsService, new ObjectMapper(), Duration.ofSeconds(20));
    }

    HttpOnlineModelClient(
            HttpClient httpClient,
            AiSettingsService settingsService,
            Duration requestTimeout
    ) {
        this(httpClient, settingsService, new ObjectMapper(), requestTimeout);
    }

    HttpOnlineModelClient(
            HttpClient httpClient,
            AiSettingsService settingsService,
            ObjectMapper objectMapper,
            Duration requestTimeout
    ) {
        this.httpClient = httpClient;
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public String generateAdvice(String summary) {
        AiModelSettings settings = settingsService.current();
        if (settings.endpoint() == null || settings.endpoint().isBlank()) {
            throw new IllegalStateException("在线模型端点未配置");
        }
        if (settings.modelName() == null || settings.modelName().isBlank()) {
            throw new IllegalStateException("在线模型名称未配置");
        }
        if (!settingsService.apiKeyConfigured()) {
            throw new IllegalStateException("在线模型 API 密钥未配置");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.endpoint()))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + settingsService.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(settings.modelName(), summary)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("在线模型响应异常：" + response.statusCode());
            }
            return responseContent(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("在线模型调用失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("在线模型调用被中断", exception);
        }
    }

    @Override
    public String provider() {
        return settingsService.current().provider();
    }

    @Override
    public String model() {
        return settingsService.current().modelName();
    }

    private String requestBody(String modelName, String summary) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是 Java 学习分析助手。请根据学习表现给出具体、简洁、可执行的中文建议。");
        messages.addObject()
                .put("role", "user")
                .put("content", summary == null ? "" : summary);
        root.put("temperature", 0.2);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("在线模型请求生成失败", exception);
        }
    }

    private String responseContent(String body) {
        try {
            JsonNode content = objectMapper.readTree(body)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new IllegalStateException("在线模型响应缺少文本内容");
            }
            return content.asText().trim();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("在线模型响应解析失败", exception);
        }
    }
}
