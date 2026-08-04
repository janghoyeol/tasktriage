package com.tasktriage.backend.reporting;

import com.tasktriage.backend.reporting.dto.AdminMetricsResponse;
import com.tasktriage.backend.reporting.dto.DashboardSummaryResponse;
import com.tasktriage.backend.task.TaskRepository;
import com.tasktriage.backend.task.TaskStatus;
import com.tasktriage.backend.triage.TriageGate;
import com.tasktriage.backend.triage.TriageLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private static final List<TaskStatus> INACTIVE_STATUSES = List.of(TaskStatus.DONE, TaskStatus.ARCHIVED);

    private final TaskRepository taskRepository;
    private final TriageLogRepository triageLogRepository;

    @Value("${app.triage.avg-cost-per-llm-call-usd}")
    private double avgCostPerLlmCall;

    @Value("${app.triage.sla-approaching-hours}")
    private long slaApproachingHours;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        Map<String, Integer> countsByStatus = new LinkedHashMap<>();
        for (TaskRepository.StatusCount statusCount : taskRepository.countGroupedByStatus()) {
            countsByStatus.put(statusCount.getStatus().name(), (int) statusCount.getCount());
        }

        Instant now = Instant.now();
        Instant approachingBy = now.plus(slaApproachingHours, ChronoUnit.HOURS);

        long slaBreached = taskRepository.countByStatusNotInAndDueAtLessThan(INACTIVE_STATUSES, now);
        long slaApproaching = taskRepository.countByStatusNotInAndDueAtBetween(INACTIVE_STATUSES, now, approachingBy);

        return new DashboardSummaryResponse(countsByStatus, (int) slaApproaching, (int) slaBreached);
    }

    @Transactional(readOnly = true)
    public AdminMetricsResponse getAdminMetrics() {
        long ruleBasedCount = triageLogRepository.countByGate(TriageGate.RULE_BASED);
        long llmCount = triageLogRepository.countByLlmCalledTrue();
        long totalCount = ruleBasedCount + llmCount;

        if (totalCount == 0) {
            return new AdminMetricsResponse(0.0, 0.0, 0.0, 0.0);
        }

        double gate1FilteredRate = (double) ruleBasedCount / totalCount;
        double gate2CallRate = (double) llmCount / totalCount;
        double avgConfidence = Optional.ofNullable(triageLogRepository.averageConfidenceScore()).orElse(0.0);
        // 모든 요청을 LLM으로 처리했다면 들었을 비용 대비, Gate 1이 걸러낸 만큼 절감했다고 추정한다.
        double estimatedCostSaving = ruleBasedCount * avgCostPerLlmCall;

        return new AdminMetricsResponse(gate1FilteredRate, gate2CallRate, avgConfidence, estimatedCostSaving);
    }
}
