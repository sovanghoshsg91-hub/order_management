package com.platform.shared.util;

import java.util.UUID;

public class CorrelationIdUtil {

    private CorrelationIdUtil() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String getOrGenerate(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return generate();
        }
        return correlationId;
    }
}