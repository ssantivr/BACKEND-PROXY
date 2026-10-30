package com.workshop.proxy.model;

import java.time.Instant;
import java.util.List;

public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        LogStatus status,
        Instant timestamp,
        List<Object> params,
        Object response,
        String errorMessage,
        String stackSummary,
        boolean cacheHit) {
}
