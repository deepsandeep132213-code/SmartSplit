package com.smartsplit.ai;

import com.smartsplit.util.EnvConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for the OpenAI-compatible chat-completion API.
 *
 * API key is read from the environment variable OPENAI_API_KEY (never hardcoded).
 * Model defaults to gpt-4o-mini but is overridable via OPENAI_MODEL env var.
 *
 * Uses the JDK 11+ HttpClient — no external dependencies.
 */
public class LLMClient {

    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL   = "gpt-4o-mini";
    private static final int    TIMEOUT_SECONDS = 30;

    private final String    apiKey;
    private final String    model;
    private final String    apiUrl;
    private final HttpClient http;

    public LLMClient(EnvConfig config) throws AIException {
        this.apiKey = config.get("OPENAI_API_KEY").orElseThrow(() ->
                new AIException("OPENAI_API_KEY is not set. Add it to your .env file or environment."));
        this.model  = config.get("OPENAI_MODEL").orElse(DEFAULT_MODEL);
        this.apiUrl = config.get("OPENAI_API_URL").orElse(DEFAULT_API_URL);
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Sends a list of messages (role/content pairs) and returns the assistant reply text.
     * @param messages ordered list of {"role": "user"|"assistant"|"system", "content": "..."}
     */
    public String chat(List<Map<String, String>> messages) throws AIException {
        String body = buildRequestJson(messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AIException("API error " + response.statusCode() + ": " + extractError(response.body()));
            }
            return extractContent(response.body());
        } catch (IOException e) {
            throw new AIException("Network error reaching the AI service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIException("AI request was interrupted.", e);
        }
    }

    // ── Minimal JSON builder (no external library) ────────────────────────

    private String buildRequestJson(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":").append(jsonString(model));
        sb.append(",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> m = messages.get(i);
            sb.append("{\"role\":").append(jsonString(m.get("role")));
            sb.append(",\"content\":").append(jsonString(m.get("content")));
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ── Minimal JSON parser (no external library) ─────────────────────────

    /** Extracts choices[0].message.content from a chat-completion response. */
    private static String extractContent(String json) throws AIException {
        // Find "content" field after "message"
        int messageIdx = json.indexOf("\"message\"");
        if (messageIdx < 0) throw new AIException("Unexpected API response format.");
        int contentIdx = json.indexOf("\"content\"", messageIdx);
        if (contentIdx < 0) throw new AIException("No content in API response.");
        int colon = json.indexOf(":", contentIdx) + 1;
        return parseJsonString(json, colon);
    }

    /** Extracts the error message from an error response body. */
    private static String extractError(String json) {
        try {
            int msgIdx = json.indexOf("\"message\"");
            if (msgIdx < 0) return json;
            int colon = json.indexOf(":", msgIdx) + 1;
            return parseJsonString(json, colon);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * Parses a JSON string value starting at {@code pos} (skips whitespace, then reads quoted string).
     * Handles \" and \\ escapes; unicode escapes are passed through as-is for display purposes.
     */
    private static String parseJsonString(String json, int pos) throws AIException {
        int i = pos;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"')
            throw new AIException("Expected JSON string at position " + pos);
        i++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char ch = json.charAt(i++);
            if (ch == '"') return sb.toString();
            if (ch == '\\' && i < json.length()) {
                char esc = json.charAt(i++);
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append('\\'); sb.append(esc); }
                }
            } else {
                sb.append(ch);
            }
        }
        throw new AIException("Unterminated JSON string in API response.");
    }

    /** Encodes a Java string as a JSON string literal. */
    private static String jsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
