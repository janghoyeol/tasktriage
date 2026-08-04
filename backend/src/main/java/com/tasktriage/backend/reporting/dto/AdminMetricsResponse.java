package com.tasktriage.backend.reporting.dto;

public record AdminMetricsResponse(
        double gate1FilteredRate, double gate2CallRate, double avgConfidenceScore, double estimatedCostSavingUsd) {
}
