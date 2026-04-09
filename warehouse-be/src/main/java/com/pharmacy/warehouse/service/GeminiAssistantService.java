package com.pharmacy.warehouse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class GeminiAssistantService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${gemini.enabled:false}")
    private boolean enabled;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.request-timeout-ms:15000}")
    private int requestTimeoutMs;

    public GeminiAssistantService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<String> ask(String username, Set<String> roles, String userMessage, String liveContext) {
        if (!enabled || !StringUtils.hasText(apiKey) || !StringUtils.hasText(userMessage)) {
            return Optional.empty();
        }

        try {
            String roleSummary = roles == null || roles.isEmpty()
                    ? "unknown"
                    : String.join(",", roles);

            String prompt = """
                    Bạn là trợ lý kho dược nội bộ. Luôn trả lời bằng tiếng Việt, ngắn gọn, rõ ràng.
                    Bạn chỉ tư vấn dựa trên dữ liệu được cung cấp trong CONTEXT.
                    Nếu thiếu dữ liệu, hãy nói rõ là chưa đủ dữ liệu thay vì suy đoán.
                    Không tự ý thực hiện hành động hệ thống.

                    CONTEXT THỜI GIAN THỰC:
                    %s

                    THÔNG TIN NGƯỜI DÙNG:
                    - username: %s
                    - roles: %s

                    CÂU HỎI:
                    %s
                    """.formatted(liveContext, username == null ? "" : username, roleSummary, userMessage);

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of("temperature", 0.2)
            );

            String requestBody = objectMapper.writeValueAsString(payload);
            List<String> candidates = new ArrayList<>();
            candidates.add(model);
            candidates.add("gemini-2.5-flash");
            candidates.add("gemini-2.5-flash-lite");
            candidates.add("gemini-2.0-flash");

            JsonNode root = null;
            for (String modelCandidate : candidates.stream().distinct().toList()) {
                HttpResponse<String> response = sendGenerateContentRequest(modelCandidate, requestBody);
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    root = objectMapper.readTree(response.body());
                    if (!modelCandidate.equals(model)) {
                        log.info("Gemini model fallback applied. configuredModel={}, activeModel={}", model, modelCandidate);
                    }
                    break;
                }

                if (response.statusCode() == 404) {
                    log.warn("Gemini model '{}' not found. Trying next fallback model.", modelCandidate);
                    continue;
                }

                if (response.statusCode() == 429 || response.statusCode() == 503) {
                    log.warn("Gemini model '{}' temporarily unavailable (status={}). Trying next fallback model.",
                            modelCandidate,
                            response.statusCode());
                    continue;
                }

                log.warn("Gemini API request failed. status={}, body={}", response.statusCode(), truncate(response.body()));
                return Optional.empty();
            }

            if (root == null) {
                log.warn("Gemini API request failed after model fallback attempts.");
                return Optional.empty();
            }

            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return Optional.empty();
            }

            StringBuilder answer = new StringBuilder();
            for (JsonNode part : parts) {
                String text = part.path("text").asText("");
                if (StringUtils.hasText(text)) {
                    if (!answer.isEmpty()) {
                        answer.append("\n");
                    }
                    answer.append(text.trim());
                }
            }

            String content = answer.toString().trim();
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }

            return Optional.of(content);
        } catch (Exception ex) {
            log.warn("Gemini API request error: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private HttpResponse<String> sendGenerateContentRequest(String modelName, String requestBody) throws Exception {
        String endpoint = normalizeBaseUrl(baseUrl)
                + "/models/" + modelName + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(requestTimeoutMs, 1000)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String normalizeBaseUrl(String url) {
        String normalized = url == null ? "" : url.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= 500) {
            return text;
        }
        return text.substring(0, 500) + "...";
    }
}
