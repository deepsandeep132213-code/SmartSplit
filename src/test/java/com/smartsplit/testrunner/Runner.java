package com.smartsplit.testrunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal reflection-based test runner.
 * Scans all classes passed on the command line for @Test methods, runs them,
 * and prints a JUnit-style summary.
 *
 * Usage (after compiling both src and test):
 *   java -cp classes com.smartsplit.testrunner.Runner \
 *        com.smartsplit.util.MoneyTest \
 *        com.smartsplit.strategy.EqualSplitStrategyTest ...
 */
public class Runner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: Runner <TestClass1> [TestClass2 ...]");
            System.exit(1);
        }

        int passed = 0, failed = 0;
        List<String> failures = new ArrayList<>();

        for (String className : args) {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(Test.class)) continue;
                String testName = clazz.getSimpleName() + "#" + m.getName();
                try {
                    m.invoke(instance);
                    System.out.println("  PASS  " + testName);
                    passed++;
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    Throwable cause = ite.getCause();
                    String msg = testName + " — " + cause.getMessage();
                    System.out.println("  FAIL  " + msg);
                    cause.printStackTrace(System.out);
                    failures.add(msg);
                    failed++;
                }
            }
        }

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            failures.forEach(f -> System.out.println("  ✗ " + f));
            System.exit(1);
        }
    }
}
