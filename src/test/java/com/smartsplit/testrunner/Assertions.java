package com.smartsplit.testrunner;

import java.math.BigDecimal;
import java.util.Objects;

/** Assertion helpers used in tests. */
public final class Assertions {

    private Assertions() {}

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            fail("Expected <" + expected + "> but got <" + actual + ">");
    }

    public static void assertEquals(int expected, int actual) {
        if (expected != actual)
            fail("Expected <" + expected + "> but got <" + actual + ">");
    }

    /** BigDecimal comparison ignoring scale (e.g. 1.0 == 1.00). */
    public static void assertDecimalEquals(BigDecimal expected, BigDecimal actual) {
        if (expected == null || actual == null || expected.compareTo(actual) != 0)
            fail("Expected <" + expected + "> but got <" + actual + ">");
    }

    public static void assertTrue(boolean condition) {
        if (!condition) fail("Expected true but got false");
    }

    public static void assertTrue(boolean condition, String msg) {
        if (!condition) fail(msg);
    }

    public static void assertFalse(boolean condition) {
        if (condition) fail("Expected false but got true");
    }

    public static void assertFalse(boolean condition, String msg) {
        if (condition) fail(msg);
    }

    public static void assertNotNull(Object obj) {
        if (obj == null) fail("Expected non-null but got null");
    }

    public static void assertNull(Object obj) {
        if (obj != null) fail("Expected null but got <" + obj + ">");
    }

    public static <T extends Throwable> T assertThrows(Class<T> type, ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            fail("Expected " + type.getSimpleName() + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        fail("Expected " + type.getSimpleName() + " but no exception was thrown");
        throw new AssertionError("unreachable");
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    @FunctionalInterface
    public interface ThrowingRunnable { void run() throws Throwable; }
}
