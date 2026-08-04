package com.tasktriage.backend.triage.dto;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Urgency;
import com.tasktriage.backend.triage.TriageGate;
import com.tasktriage.backend.triage.TriageLog;
import java.time.Instant;

public record TriageLogResponse(
        Long id,
        TriageGate gate,
        boolean llmCalled,
        Double confidenceScore,
        Category resultCategory,
        Urgency resultUrgency,
        Instant createdAt) {

    public static TriageLogResponse from(TriageLog log) {
        return new TriageLogResponse(
                log.getId(),
                log.getGate(),
                log.isLlmCalled(),
                log.getConfidenceScore(),
                log.getResultCategory(),
                log.getResultUrgency(),
                log.getCreatedAt());
    }
}
