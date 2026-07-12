package com.studycollection.ai.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HttpOnlineModelClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsOpenAiCompatibleRequestAndExtractsAssistantContent() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, """
                    {"choices":[{"message":{"role":"assistant","content":"优先复习 JVM 内存模型。"}}]}
                    """);
        });
        HttpOnlineModelClient client = client("secret-123");

        String advice = client.generateAdvice("JVM 正确率 20%");

        assertThat(advice).isEqualTo("优先复习 JVM 内存模型。");
        assertThat(authorization.get()).isEqualTo("Bearer secret-123");
        assertThat(requestBody.get())
                .contains("\"model\":\"qwen-plus\"")
                .contains("\"role\":\"system\"")
                .contains("\"role\":\"user\"")
                .contains("JVM 正确率 20%")
                .contains("\"temperature\":0.2");
    }

    @Test
    void rejectsNonSuccessAndMalformedResponsesWithoutReturningResponseBody() throws Exception {
        startServer(exchange -> respond(exchange, 429, "secret remote response"));

        assertThatIllegalStateException()
                .isThrownBy(() -> client("secret-123").generateAdvice("摘要"))
                .withMessageContaining("429")
                .withMessageNotContaining("secret remote response");

        server.stop(0);
        startServer(exchange -> respond(exchange, 200, "{not-json}"));

        assertThatIllegalStateException()
                .isThrownBy(() -> client("secret-123").generateAdvice("摘要"))
                .withMessageContaining("解析");
    }

    @Test
    void requiresEndpointModelAndApiKey() {
        AiSettingsService service = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig("", "", "")
        );
        HttpOnlineModelClient client = new HttpOnlineModelClient(HttpClient.newHttpClient(), service);

        assertThatIllegalStateException()
                .isThrownBy(() -> client.generateAdvice("摘要"))
                .withMessageContaining("端点");
    }

    @Test
    void rejectsMissingAssistantContent() throws Exception {
        startServer(exchange -> respond(exchange, 200, "{\"choices\":[{\"message\":{}}]}"));

        assertThatIllegalStateException()
                .isThrownBy(() -> client("secret-123").generateAdvice("摘要"))
                .withMessageContaining("缺少文本内容");
    }

    @Test
    void timesOutSlowModelCalls() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"迟到的响应\"}}]}");
        });
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        AiSettingsService service = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig(endpoint, "qwen-plus", "secret-123")
        );
        HttpOnlineModelClient client = new HttpOnlineModelClient(
                HttpClient.newHttpClient(),
                service,
                Duration.ofMillis(50)
        );

        assertThatIllegalStateException()
                .isThrownBy(() -> client.generateAdvice("摘要"))
                .withMessageContaining("调用失败");
    }

    private HttpOnlineModelClient client(String apiKey) {
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        AiSettingsService service = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig(endpoint, "qwen-plus", apiKey)
        );
        return new HttpOnlineModelClient(HttpClient.newHttpClient(), service);
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> handler.handle(exchange));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
