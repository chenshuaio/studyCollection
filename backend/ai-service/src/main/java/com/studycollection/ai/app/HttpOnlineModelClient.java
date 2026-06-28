package com.studycollection.ai.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpOnlineModelClient implements OnlineModelClient {
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;

    public HttpOnlineModelClient() {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                System.getenv("STUDY_COLLECTION_AI_ENDPOINT"),
                System.getenv("STUDY_COLLECTION_AI_API_KEY")
        );
    }

    HttpOnlineModelClient(HttpClient httpClient, String endpoint, String apiKey) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    @Override
    public String generateAdvice(String summary) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("STUDY_COLLECTION_AI_ENDPOINT 未配置");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"summary\":\"" + escapeJson(summary) + "\"}"));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("在线模型响应异常：" + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("在线模型调用失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("在线模型调用被中断", exception);
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
