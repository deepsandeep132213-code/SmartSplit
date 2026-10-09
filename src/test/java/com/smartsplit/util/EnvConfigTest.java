package com.smartsplit.util;

import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;
import java.util.List;
import java.util.Map;

public class EnvConfigTest {

    @Test public void parse_simpleKv() {
        Map<String, String> m = EnvConfig.parse(List.of("FOO=bar"));
        Assertions.assertEquals("bar", m.get("FOO"));
    }

    @Test public void parse_ignoresComments() {
        Map<String, String> m = EnvConfig.parse(List.of("# comment", "KEY=val"));
        Assertions.assertNull(m.get("# comment"));
        Assertions.assertEquals("val", m.get("KEY"));
    }

    @Test public void parse_ignoresBlankLines() {
        Map<String, String> m = EnvConfig.parse(List.of("", "  ", "K=v"));
        Assertions.assertEquals("v", m.get("K"));
        Assertions.assertEquals(1, m.size());
    }

    @Test public void parse_stripsQuotedValue() {
        Map<String, String> m = EnvConfig.parse(List.of("DB_URL=\"jdbc:mysql://localhost\""));
        Assertions.assertEquals("jdbc:mysql://localhost", m.get("DB_URL"));
    }

    @Test public void parse_singleQuotedValue() {
        Map<String, String> m = EnvConfig.parse(List.of("X='hello world'"));
        Assertions.assertEquals("hello world", m.get("X"));
    }

    @Test public void parse_blankValue_notSet() {
        Map<String, String> m = EnvConfig.parse(List.of("EMPTY="));
        // blank value is treated as not-set by EnvConfig.get()
        Assertions.assertTrue(m.containsKey("EMPTY"));
        Assertions.assertEquals("", m.get("EMPTY"));
    }

    @Test public void parse_inlineComment_stripped() {
        Map<String, String> m = EnvConfig.parse(List.of("K=val # inline comment"));
        // value should be "val" not "val # inline comment"
        Assertions.assertEquals("val", m.get("K").trim());
    }

    @Test public void parse_equalsInValue() {
        Map<String, String> m = EnvConfig.parse(List.of("URL=a=b=c"));
        Assertions.assertEquals("a=b=c", m.get("URL"));
    }
}
