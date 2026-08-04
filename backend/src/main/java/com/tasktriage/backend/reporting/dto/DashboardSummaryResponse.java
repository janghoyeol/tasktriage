package com.tasktriage.backend.reporting.dto;

import java.util.Map;

public record DashboardSummaryResponse(Map<String, Integer> countsByStatus, int slaApproaching, int slaBreached) {
}
