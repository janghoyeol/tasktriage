package com.tasktriage.backend.sla;

import com.tasktriage.backend.task.Urgency;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** urgency가 정해진 시점에 Task.due_at을 계산한다. 기준 시각은 접수 시각(createdAt). */
@Component
@RequiredArgsConstructor
public class SLACalculator {

    private final SLARuleRepository slaRuleRepository;

    public Instant calculateDueAt(Instant baseTime, Urgency urgency) {
        SLARule rule = slaRuleRepository
                .findByUrgency(urgency)
                .orElseThrow(() -> new IllegalStateException("No SLA rule configured for urgency: " + urgency));

        return baseTime.plus(rule.getResponseTimeHours(), ChronoUnit.HOURS);
    }
}
