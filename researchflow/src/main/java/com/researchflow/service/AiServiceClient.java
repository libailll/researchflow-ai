package com.researchflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.researchflow.common.ErrorCode;
import com.researchflow.config.AiServiceProperties;
import com.researchflow.dto.AiChatDTO;
import com.researchflow.dto.SemanticSearchDTO;
import com.researchflow.exception.BusinessException;
import com.researchflow.vo.AiChatVO;
import com.researchflow.vo.AiStreamResult;
import com.researchflow.vo.SemanticSearchResultVO;
import com.researchflow.vo.WeeklyReportAiResultVO;
import com.researchflow.vo.DocumentSummaryAiResultVO;
import com.researchflow.vo.ProjectRiskReportAiResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Slf4j
@Component
public class AiServiceClient {

    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiServiceClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AiChatVO chat(Long projectId, Long userId, AiChatDTO request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request("/ai/chat", projectId, userId, request),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), AiChatVO.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    public AiStreamResult stream(Long projectId, Long userId, AiChatDTO request, OutputStream outputStream) {
        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        List<SemanticSearchResultVO> sources = List.of();
        String model = null;
        boolean completed = false;
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request("/ai/chat/stream", projectId, userId, request),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                ensureSuccess(response.statusCode(), body);
            }
            try (InputStream input = response.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    JsonNode event = objectMapper.readTree(data);
                    String type = event.path("type").asText();
                    if ("content".equals(type)) {
                        content.append(event.path("content").asText(""));
                    } else if ("reasoning".equals(type)) {
                        reasoning.append(event.path("content").asText(""));
                    } else if ("sources".equals(type) && event.has("sources")) {
                        sources = objectMapper.convertValue(event.get("sources"), new TypeReference<>() { });
                    } else if ("done".equals(type)) {
                        completed = true;
                        model = event.path("model").asText(model);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeStreamError(outputStream, "AI 请求已中断");
        } catch (Exception e) {
            log.error("AI streaming request failed: projectId={}", projectId, e);
            writeStreamError(outputStream, ErrorCode.AI_SERVICE_UNAVAILABLE.getMessage());
        }
        return new AiStreamResult(
                content.toString(),
                reasoning.isEmpty() ? null : reasoning.toString(),
                sources,
                model,
                completed
        );
    }

    public List<SemanticSearchResultVO> search(Long projectId, SemanticSearchDTO request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", projectId);
        payload.put("query", request.query());
        payload.put("topK", request.safeTopK());
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + "/ai/search"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", properties.internalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), new TypeReference<>() { });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    public WeeklyReportAiResultVO generateWeeklyReport(Map<String, Object> payload) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + "/ai/weekly-report"))
                    .timeout(Duration.ofSeconds(150))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", properties.internalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), WeeklyReportAiResultVO.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    public DocumentSummaryAiResultVO generateDocumentSummary(Map<String, Object> payload) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + "/ai/document-summary"))
                    .timeout(Duration.ofSeconds(240))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", properties.internalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), DocumentSummaryAiResultVO.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    public ProjectRiskReportAiResultVO generateProjectRiskReport(Map<String, Object> payload) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + "/ai/project-risk-report"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", properties.internalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());
            return objectMapper.readValue(response.body(), ProjectRiskReportAiResultVO.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        }
    }

    private HttpRequest request(String path, Long projectId, Long userId, AiChatDTO request) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", projectId);
        payload.put("userId", userId);
        payload.put("message", request.message());
        payload.put("history", request.safeHistory());
        return HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl().replaceAll("/$", "") + path))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", properties.internalToken())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String message = null;
        try {
            JsonNode detail = objectMapper.readTree(body).get("detail");
            if (detail != null && detail.isTextual()) {
                message = detail.asText();
            }
        } catch (Exception ignored) {
            // Do not expose unknown upstream response bodies.
        }
        log.warn("AI service returned status {}: {}", statusCode, message);
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    private BusinessException unavailable(Exception cause) {
        log.error("AI service request failed", cause);
        return new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    private void writeStreamError(OutputStream outputStream, String message) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "error", "message", message));
            outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ignored) {
            log.debug("Client disconnected before stream error could be written");
        }
    }
}
