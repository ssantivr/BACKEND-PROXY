package com.workshop.proxy.model;

public record ServiceMetrics(String serviceId, long totalCalls, double successRate, double errorRate, double averageResponseTimeMs) {
}
