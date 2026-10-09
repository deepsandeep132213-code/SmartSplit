package com.smartsplit.ai;

import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;

import java.lang.reflect.Method;

/**
 * Tests LLMClient's hand-rolled JSON parser without network access.
 * Uses reflection to call package-private parseJsonString via the public extractContent path.
 */
public class LLMClientJsonTest {

    // We test the JSON building by constructing a fake response and parsing it
    private static final String FAKE_RESPONSE = """
            {
              "id": "chatcmpl-abc",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hello, world!"
                  }
                }
              ]
            }
            """;

    private static final String RESPONSE_WITH_ESCAPES = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Line 1\\nLine 2\\t\\\"quoted\\\""
                  }
                }
              ]
            }
            """;

    @Test public void extractContent_simpleResponse_returnsText() throws Exception {
        String result = invokeExtractContent(FAKE_RESPONSE);
        Assertions.assertEquals("Hello, world!", result);
    }

    @Test public void extractContent_withEscapes_handlesBackslashN() throws Exception {
        String result = invokeExtractContent(RESPONSE_WITH_ESCAPES);
        Assertions.assertTrue(result.contains("\n"), "Should contain actual newline");
        Assertions.assertTrue(result.contains("\"quoted\""), "Should contain unescaped quotes");
    }

    @Test public void chatMessage_toMap_hasCorrectKeys() {
        ChatMessage m = ChatMessage.user("hello");
        var map = m.toMap();
        Assertions.assertEquals("user",  map.get("role"));
        Assertions.assertEquals("hello", map.get("content"));
    }

    @Test public void chatMessage_system_hasSystemRole() {
        ChatMessage m = ChatMessage.system("You are helpful.");
        Assertions.assertEquals("system", m.role());
    }

    @Test public void chatMessage_assistant_hasAssistantRole() {
        ChatMessage m = ChatMessage.assistant("Sure!");
        Assertions.assertEquals("assistant", m.role());
    }

    // ── Reflection helper ─────────────────────────────────────────────────

    private String invokeExtractContent(String json) throws Exception {
        Method m = LLMClient.class.getDeclaredMethod("extractContent", String.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(null, json);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
