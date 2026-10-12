package com.knowledgeapp.qa.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLlmClientTest {

    private HttpServer server;
    private final AtomicReference<String> authHeader = new AtomicReference<>();
    private final AtomicReference<String> contentLength = new AtomicReference<>();
    private final AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"answer_status\\\":\\\"grounded\\\"}\"}}]}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentLength.set(exchange.getRequestHeaders().getFirst("Content-Length"));
            requestBody.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private OpenAiCompatibleLlmClient client() {
        return new OpenAiCompatibleLlmClient(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/", "test-model", "sk-test", Duration.ofSeconds(5));
    }

    @Test
    void sendsChatCompletionWithAuthAndContentLength() {
        String reply = client().complete(List.of(LlmMessage.system("规则"), LlmMessage.user("问题")));

        assertThat(reply).isEqualTo("{\"answer_status\":\"grounded\"}");
        assertThat(authHeader.get()).isEqualTo("Bearer sk-test");
        assertThat(contentLength.get()).isNotNull();
        JsonNode body = requestBody.get();
        assertThat(body.path("model").asText()).isEqualTo("test-model");
        assertThat(body.path("temperature").asInt()).isZero();
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(body.path("messages").get(1).path("content").asText()).isEqualTo("问题");
    }

    @Test
    void providerErrorsBecomeLlmExceptionWithoutLeakingKey() {
        status = 401;
        responseBody = "{\"error\":\"invalid key sk-test\"}";

        assertThatThrownBy(() -> client().complete(List.of(LlmMessage.user("q"))))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining("sk-test");
    }

    @Test
    void missingContentIsAnError() {
        responseBody = "{\"choices\":[]}";
        assertThatThrownBy(() -> client().complete(List.of(LlmMessage.user("q"))))
                .isInstanceOf(LlmException.class);
    }
}
