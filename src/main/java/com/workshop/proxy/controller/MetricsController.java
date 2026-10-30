package com.workshop.proxy.controller;

import com.workshop.proxy.metrics.LoadSimulator;
import com.workshop.proxy.metrics.MetricsService;
import com.workshop.proxy.model.LoadSummary;
import com.workshop.proxy.model.LogPage;
import com.workshop.proxy.model.LogStatus;
import com.workshop.proxy.model.ServiceMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final LoadSimulator loadSimulator;

    public MetricsController(MetricsService metricsService, LoadSimulator loadSimulator) {
        this.metricsService = metricsService;
        this.loadSimulator = loadSimulator;
    }

    @GetMapping("/summary")
    public List<ServiceMetrics> summary() {
        return metricsService.summary();
    }

    @GetMapping("/logs")
    public LogPage logs(@RequestParam(required = false) String service,
                        @RequestParam(required = false) LogStatus status,
                        @RequestParam(required = false) Instant from,
                        @RequestParam(required = false) Instant to,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
        return metricsService.findLogs(service, status, from, to, page, size);
    }

    @PostMapping("/simulate-load")
    public LoadSummary simulateLoad() {
        return loadSimulator.simulate();
    }
}
