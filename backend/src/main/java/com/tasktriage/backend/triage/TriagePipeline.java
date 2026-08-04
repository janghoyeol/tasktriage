package com.tasktriage.backend.triage;

import com.tasktriage.backend.sla.SLACalculator;
import com.tasktriage.backend.task.Task;
import com.tasktriage.backend.task.TaskService;
import com.tasktriage.backend.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task 하나를 Gate 1 → (필요하면) Gate 2 순서로 분류하고, 결과에 따라 상태를 전이시킨다.
 *
 * task를 파라미터로 직접 받지 않고 taskId로만 받는 이유: 이 메서드가 자기 트랜잭션 안에서
 * Task를 새로 조회해야, 그 안에서 일어나는 변경(applyClassification 등)이 이 트랜잭션이
 * 커밋될 때 정상적으로 flush된다. 다른 트랜잭션에서 이미 끝난(detached) 엔티티를 받아서
 * 건드리면 변경사항이 저장되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class TriagePipeline {

    private final RuleBasedClassifier ruleBasedClassifier;
    private final Gate2Client gate2Client;
    private final TriageLogRepository triageLogRepository;
    private final TaskService taskService;
    private final SLACalculator slaCalculator;

    @Value("${app.triage.confidence-threshold}")
    private double confidenceThreshold;

    @Transactional
    public void triage(Long taskId) {
        Task task = taskService.getOrThrow(taskId);

        ruleBasedClassifier
                .classify(task.getTitle(), task.getDescription())
                .ifPresentOrElse(result -> applyGate1(task, result), () -> applyGate2(task));
    }

    private void applyGate1(Task task, RuleBasedClassifier.Result result) {
        task.applyClassification(result.category(), result.urgency());
        task.scheduleDueAt(slaCalculator.calculateDueAt(task.getCreatedAt(), result.urgency()));
        triageLogRepository.save(
                new TriageLog(task, TriageGate.RULE_BASED, false, null, result.category(), result.urgency()));

        taskService.changeStatus(task.getId(), TaskStatus.TRIAGED);
        taskService.changeStatus(task.getId(), TaskStatus.QUEUED);
    }

    private void applyGate2(Task task) {
        Gate2Client.ClassifyResponse response = gate2Client.classify(task.getTitle(), task.getDescription());

        task.applyClassification(response.category(), response.urgency());
        task.scheduleDueAt(slaCalculator.calculateDueAt(task.getCreatedAt(), response.urgency()));
        triageLogRepository.save(new TriageLog(
                task, TriageGate.LLM, true, response.confidence(), response.category(), response.urgency()));

        taskService.changeStatus(task.getId(), TaskStatus.TRIAGED);

        TaskStatus nextStatus =
                response.confidence() >= confidenceThreshold ? TaskStatus.QUEUED : TaskStatus.NEEDS_REVIEW;
        taskService.changeStatus(task.getId(), nextStatus);
    }
}
