package com.tasktriage.backend.reporting;

import com.tasktriage.backend.reporting.dto.AdminMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReportingService reportingService;

    @GetMapping("/metrics")
    public AdminMetricsResponse getMetrics() {
        return reportingService.getAdminMetrics();
    }
}
