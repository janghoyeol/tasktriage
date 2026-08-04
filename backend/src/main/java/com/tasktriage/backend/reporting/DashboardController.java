package com.tasktriage.backend.reporting;

import com.tasktriage.backend.reporting.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportingService reportingService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return reportingService.getDashboardSummary();
    }
}
