package com.workshop.proxy.metrics;

import com.workshop.proxy.model.LogEntry;
import com.workshop.proxy.model.LogPage;
import com.workshop.proxy.model.LogStatus;
import com.workshop.proxy.model.ServiceMetrics;
import com.workshop.proxy.repository.LogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MetricsService {

    private static final List<String> SERVICE_IDS = List.of("inventory", "orders", "payments");
    private static final int MAX_PAGE_SIZE = 100;

    private final LogRepository logRepository;

    public MetricsService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<ServiceMetrics> summary() {
        List<LogEntry> logs = logRepository.findAll();
        return SERVICE_IDS.stream().map(serviceId -> metricsFor(serviceId, logs)).toList();
    }

    public LogPage findLogs(String service, LogStatus status, Instant from, Instant to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<LogEntry> filtered = logRepository.findAll().stream()
                .filter(log -> service == null || service.isBlank() || log.serviceId().equals(service))
                .filter(log -> status == null || log.status() == status)
                .filter(log -> from == null || !log.timestamp().isBefore(from))
                .filter(log -> to == null || !log.timestamp().isAfter(to))
                .toList();
        List<LogEntry> content = filtered.stream().skip((long) safePage * safeSize).limit(safeSize).toList();
        int totalPages = (int) Math.ceil((double) filtered.size() / safeSize);
        return new LogPage(content, safePage, safeSize, filtered.size(), totalPages);
    }

    private ServiceMetrics metricsFor(String serviceId, List<LogEntry> logs) {
        List<LogEntry> serviceLogs = logs.stream().filter(log -> log.serviceId().equals(serviceId)).toList();
        long total = serviceLogs.size();
        if (total == 0) {
            return new ServiceMetrics(serviceId, 0, 0, 0, 0);
        }
        long errors = serviceLogs.stream().filter(log -> log.status() == LogStatus.ERROR).count();
        double errorRate = percentage(errors, total);
        double average = serviceLogs.stream().mapToLong(LogEntry::durationMs).average().orElse(0);
        return new ServiceMetrics(serviceId, total, 100 - errorRate, errorRate, Math.round(average * 100) / 100.0);
    }

    private double percentage(long part, long total) {
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}
