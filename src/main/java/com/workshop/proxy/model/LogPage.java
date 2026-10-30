package com.workshop.proxy.model;

import java.util.List;

public record LogPage(List<LogEntry> content, int page, int size, long totalElements, int totalPages) {
}
