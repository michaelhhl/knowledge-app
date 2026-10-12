package com.knowledgeapp.qa.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** OpenAI Chat Completions compatible endpoint (OpenAI, DeepSeek, Ollama, ...). */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String model;

    public OpenAiCompatibleLlmClient(String baseUrl, String model, String apiKey, Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : Duration.ofSeconds(90);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(effectiveTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String complete(List<LlmMessage> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "messages", messages.stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList());
        try {
            // Serialized up front so the request carries Content-Length; some compatible servers reject chunked bodies.
            byte[] payload = OBJECT_MAPPER.writeValueAsBytes(body);
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = response == null ? null : response.at("/choices/0/message/content");
            if (content == null || content.isMissingNode() || !content.isTextual()) {
                throw new LlmException("模型响应缺少内容。");
            }
            return content.asText();
        } catch (RestClientResponseException ex) {
            throw new LlmException("模型服务返回错误（HTTP " + ex.getStatusCode().value() + "）。", ex);
        } catch (RestClientException ex) {
            throw new LlmException("无法连接模型服务。", ex);
        } catch (JsonProcessingException ex) {
            throw new LlmException("无法构造模型请求。", ex);
        }
    }
}
