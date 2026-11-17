package com.eiu.capstone.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Profile("!desktop")
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String model;
    private final int timeoutSeconds;

    public GeminiClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.gemini.model:gemini-3.5-flash}") String model,
            @Value("${app.ai.request-timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(10, timeoutSeconds)))
                .build();
    }

    public String modelId() {
        return model;
    }

    /**
     * Fast BYOK check: GET model metadata (~200ms). Does not call generateContent.
     */
    public void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is required");
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.min(10, timeoutSeconds)))
                    .header("x-goog-api-key", apiKey.trim())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            mapProviderFailure(response.statusCode(), response.body(), true);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini key check timed out. Try again.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini key check interrupted");
        } catch (Exception ex) {
            log.warn("Gemini key validation failed: {}", ex.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not reach Gemini to verify the API key");
        }
    }

    public GeminiResult generateJson(String apiKey, String systemInstruction, Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is required");
        }
        try {
            String userText = objectMapper.writeValueAsString(context);
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0);
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("maxOutputTokens", 1024);
            // Gemini 3.x: keep Conceptual hints fast (default thinking is medium).
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", "minimal"));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))));
            body.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userText)))));
            body.put("generationConfig", generationConfig);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model
                    + ":generateContent";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            mapProviderFailure(response.statusCode(), response.body(), false);

            JsonNode root = objectMapper.readTree(response.body());
            String text = extractText(root);
            if (text == null || text.isBlank()) {
                log.warn("Empty Gemini response model={} finishReason={}",
                        model, root.path("candidates").path(0).path("finishReason").asText(""));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty Gemini response");
            }
            long tokens = root.path("usageMetadata").path("totalTokenCount").asLong(0L);
            if (tokens <= 0) {
                long prompt = root.path("usageMetadata").path("promptTokenCount").asLong(0L);
                long candidates = root.path("usageMetadata").path("candidatesTokenCount").asLong(0L);
                tokens = prompt + candidates;
            }
            return new GeminiResult(text, Math.max(0L, tokens));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini request timed out");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini request interrupted");
        } catch (Exception ex) {
            log.warn("Gemini request failed model={}: {}", model, ex.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini request failed");
        }
    }

    private void mapProviderFailure(int status, String body, boolean keyCheck) {
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 400 || status == 401 || status == 403) {
            if (looksLikeInvalidApiKey(body)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Gemini API key");
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    keyCheck
                            ? "Gemini rejected the API key or model. Check the key and APP_AI_GEMINI_MODEL."
                            : "Gemini rejected the request. Check the API key and model.");
        }
        if (status == 404) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gemini model not found: " + model);
        }
        if (status == 429) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Gemini quota or rate limit exceeded");
        }
        log.warn("Gemini provider error status={} model={} keyCheck={}", status, model, keyCheck);
        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                keyCheck ? "Gemini provider unavailable while checking the API key" : "Gemini provider unavailable");
    }

    private static String extractText(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            // Skip thought parts if present; keep visible text only.
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private static boolean looksLikeInvalidApiKey(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase();
        return lower.contains("api_key_invalid")
                || lower.contains("api key not valid")
                || lower.contains("invalid api key")
                || lower.contains("permission_denied");
    }

    public record GeminiResult(String text, long totalTokenCount) {}
}
