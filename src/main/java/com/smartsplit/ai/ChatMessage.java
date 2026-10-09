package com.smartsplit.ai;

/**
 * Immutable chat message with a role ("system", "user", "assistant") and text content.
 */
public record ChatMessage(String role, String content) {
    public static ChatMessage system(String content)    { return new ChatMessage("system",    content); }
    public static ChatMessage user(String content)      { return new ChatMessage("user",       content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant",  content); }

    /** Converts to the Map form expected by LLMClient. */
    public java.util.Map<String, String> toMap() {
        return java.util.Map.of("role", role, "content", content);
    }
}
