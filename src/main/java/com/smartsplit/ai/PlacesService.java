package com.smartsplit.ai;

import com.smartsplit.util.EnvConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the Google Places Nearby Search API and returns real, API-sourced results.
 *
 * The API key is read from GOOGLE_PLACES_API_KEY (never hardcoded).
 * Claude's spec: "Do not allow the LLM to invent place names, ratings, addresses, or distances."
 * This service enforces that by fetching only from the official API; the caller must not
 * substitute invented data if the API is unavailable.
 *
 * API docs: https://developers.google.com/maps/documentation/places/web-service/search-nearby
 */
public class PlacesService {

    private static final String BASE_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final int    TIMEOUT_SECONDS = 10;
    private static final int    DEFAULT_RADIUS  = 2000; // metres

    private final String    apiKey;
    private final HttpClient http;

    public PlacesService(EnvConfig config) throws AIException {
        this.apiKey = config.get("GOOGLE_PLACES_API_KEY").orElseThrow(() ->
                new AIException("GOOGLE_PLACES_API_KEY is not set. Add it to your .env file."));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Returns up to {@code maxResults} nearby places of the given type.
     *
     * @param lat        user latitude
     * @param lng        user longitude
     * @param type       Google Places type, e.g. "restaurant", "cafe", "atm"
     * @param keyword    additional keyword, e.g. "pizza" (may be empty)
     * @param maxResults maximum results to return (1–20)
     */
    public List<PlaceResult> search(double lat, double lng,
                                    String type, String keyword,
                                    int maxResults) throws AIException {
        String url = buildUrl(lat, lng, type, keyword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AIException("Places API error " + response.statusCode());
            }
            return parseResults(response.body(), Math.min(maxResults, 20));
        } catch (IOException e) {
            throw new AIException("Network error reaching Google Places: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIException("Places request interrupted.", e);
        }
    }

    // ── URL builder ───────────────────────────────────────────────────────

    private String buildUrl(double lat, double lng, String type, String keyword) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?location=").append(lat).append(",").append(lng);
        sb.append("&radius=").append(DEFAULT_RADIUS);
        sb.append("&key=").append(encode(apiKey));
        if (type != null && !type.isBlank())
            sb.append("&type=").append(encode(type.trim()));
        if (keyword != null && !keyword.isBlank())
            sb.append("&keyword=").append(encode(keyword.trim()));
        return sb.toString();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ── Minimal JSON parser for Places API response ───────────────────────
    // The response is well-structured; we extract the fields we need without
    // pulling in a JSON library.

    private List<PlaceResult> parseResults(String json, int max) throws AIException {
        String status = extractField(json, "\"status\"");
        if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
            String errMsg = extractFieldOrDefault(json, "\"error_message\"", status);
            throw new AIException("Google Places API returned status " + status + ": " + errMsg);
        }

        List<PlaceResult> results = new ArrayList<>();
        int resultsStart = json.indexOf("\"results\"");
        if (resultsStart < 0) return results;

        int arrayStart = json.indexOf("[", resultsStart);
        if (arrayStart < 0) return results;

        // Walk through objects inside the results array
        int i = arrayStart + 1;
        int depth = 0;
        int objStart = -1;
        int count = 0;

        while (i < json.length() && count < max) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = json.substring(objStart, i + 1);
                    PlaceResult place = parsePlace(obj);
                    if (place != null) { results.add(place); count++; }
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
            i++;
        }
        return results;
    }

    private PlaceResult parsePlace(String obj) {
        try {
            String name    = extractFieldOrDefault(obj, "\"name\"", "Unknown");
            String address = extractFieldOrDefault(obj, "\"vicinity\"", "Address unavailable");
            String rating  = extractFieldOrDefault(obj, "\"rating\"", "N/A");
            String total   = extractFieldOrDefault(obj, "\"user_ratings_total\"", "0");

            // open_now is inside geometry.opening_hours
            String openNow = "Unknown";
            int ohIdx = obj.indexOf("\"opening_hours\"");
            if (ohIdx >= 0) {
                String ohStr = obj.substring(ohIdx);
                int onIdx = ohStr.indexOf("\"open_now\"");
                if (onIdx >= 0) {
                    int colon = ohStr.indexOf(":", onIdx) + 1;
                    int end   = ohStr.indexOf(",", colon);
                    if (end < 0) end = ohStr.indexOf("}", colon);
                    if (end > colon) {
                        String val = ohStr.substring(colon, end).trim();
                        openNow = "true".equals(val) ? "Open now" : "Closed";
                    }
                }
            }

            // location lat/lng
            double lat = 0, lng = 0;
            int locIdx = obj.indexOf("\"location\"");
            if (locIdx >= 0) {
                String locStr = obj.substring(locIdx);
                lat = parseDouble(locStr, "\"lat\"");
                lng = parseDouble(locStr, "\"lng\"");
            }

            return new PlaceResult(name, address, rating, total, openNow, lat, lng);
        } catch (Exception e) {
            return null; // skip malformed entry
        }
    }

    /** Extracts the string value of the first occurrence of key in json. */
    private static String extractField(String json, String key) throws AIException {
        int idx = json.indexOf(key);
        if (idx < 0) throw new AIException("Field " + key + " not found in API response.");
        int colon = json.indexOf(":", idx) + 1;
        // May be string or number
        int i = colon;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (json.charAt(i) == '"') {
            // string
            return parseJsonString(json, i);
        }
        // number or bool
        int end = i;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(i, end).trim();
    }

    private static String extractFieldOrDefault(String json, String key, String def) {
        try { return extractField(json, key); } catch (Exception e) { return def; }
    }

    private static double parseDouble(String json, String key) {
        try { return Double.parseDouble(extractField(json, key)); } catch (Exception e) { return 0; }
    }

    private static String parseJsonString(String json, int pos) {
        int i = pos;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return "";
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\' && i < json.length()) {
                char esc = json.charAt(i++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> { sb.append('\\'); sb.append(esc); }
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
