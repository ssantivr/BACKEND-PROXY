package com.workshop.proxy.metrics;

import com.workshop.proxy.model.LogEntry;
import com.workshop.proxy.model.LogPage;
import com.workshop.proxy.model.LogStatus;
import com.workshop.proxy.model.ServiceMetrics;
import com.workshop.proxy.repository.LogRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private final LogRepository logRepository = new LogRepository();
    private final MetricsService metricsService = new MetricsService(logRepository);

    private void addLog(String service, long durationMs, LogStatus status, Instant timestamp) {
        logRepository.add(new LogEntry("id", service, "op", durationMs, status, timestamp, List.of(), null, null, null, false));
    }

    @Test
    void calculatesSummaryPerService() {
        addLog("payments", 10, LogStatus.SUCCESS, NOW);
        addLog("payments", 20, LogStatus.SUCCESS, NOW);
        addLog("payments", 30, LogStatus.SUCCESS, NOW);
        addLog("payments", 40, LogStatus.ERROR, NOW);

        List<ServiceMetrics> summary = metricsService.summary();
        ServiceMetrics payments = summary.get(2);

        assertEquals(3, summary.size());
        assertEquals(0, summary.get(0).totalCalls());
        assertEquals(4, payments.totalCalls());
        assertEquals(25.0, payments.errorRate());
        assertEquals(75.0, payments.successRate());
        assertEquals(25.0, payments.averageResponseTimeMs());
    }

    @Test
    void filtersAndPaginatesLogs() {
        addLog("orders", 1, LogStatus.SUCCESS, NOW.minusSeconds(100));
        addLog("orders", 2, LogStatus.ERROR, NOW);
        addLog("orders", 3, LogStatus.ERROR, NOW);
        addLog("orders", 4, LogStatus.ERROR, NOW);
        addLog("payments", 5, LogStatus.ERROR, NOW);

        LogPage page = metricsService.findLogs("orders", LogStatus.ERROR, NOW.minusSeconds(10), NOW, 1, 2);

        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.content().size());
        assertEquals(2, page.content().get(0).durationMs());
    }

    @Test
    void repositoryKeepsNewest1000Entries() {
        for (int i = 0; i < 1005; i++) {
            addLog("orders", i, LogStatus.SUCCESS, NOW);
        }

        List<LogEntry> logs = logRepository.findAll();
        assertEquals(1000, logs.size());
        assertEquals(1004, logs.get(0).durationMs());
    }
}
