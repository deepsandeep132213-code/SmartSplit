package com.smartsplit.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads config values from real environment variables first, then from a local {@code .env} file.
 * Blank values count as not set. The {@code .env} file is ignored by Git.
 */
public final class EnvConfig {

    private final Map<String, String> environment;
    private final Map<String, String> fileValues;

    public EnvConfig(Map<String, String> environment, Map<String, String> fileValues) {
        this.environment = Map.copyOf(environment);
        this.fileValues = Map.copyOf(fileValues);
    }

    public static EnvConfig load() {
        return load(Path.of(".env"));
    }

    static EnvConfig load(Path envFile) {
        Map<String, String> fileValues = new HashMap<>();
        if (Files.isRegularFile(envFile)) {
            try {
                fileValues = parse(Files.readAllLines(envFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Warning: could not read " + envFile + " (" + e.getMessage() + ").");
            }
        }
        return new EnvConfig(System.getenv(), fileValues);
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 &&
                    ((value.startsWith("\"") && value.endsWith("\"")) ||
                     (value.startsWith("'")  && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            } else {
                // Strip inline comment: unquoted value ends at first ' #' (space-hash)
                int commentIdx = value.indexOf(" #");
                if (commentIdx >= 0) value = value.substring(0, commentIdx).trim();
            }
            values.put(key, value);
        }
        return values;
    }

    public Optional<String> get(String key) {
        String fromEnv = environment.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) return Optional.of(fromEnv);
        String fromFile = fileValues.get(key);
        if (fromFile != null && !fromFile.isBlank()) return Optional.of(fromFile);
        return Optional.empty();
    }
}
