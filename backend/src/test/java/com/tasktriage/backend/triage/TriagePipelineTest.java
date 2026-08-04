package com.tasktriage.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tasktriage.backend.sla.SLACalculator;
import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Task;
import com.tasktriage.backend.task.TaskService;
import com.tasktriage.backend.task.TaskSource;
import com.tasktriage.backend.task.TaskStatus;
import com.tasktriage.backend.task.Urgency;
import com.tasktriage.backend.user.User;
import com.tasktriage.backend.user.UserRole;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TriagePipelineTest {

    @Mock
    private RuleBasedClassifier ruleBasedClassifier;

    @Mock
    private Gate2Client gate2Client;

    @Mock
    private TriageLogRepository triageLogRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private SLACalculator slaCalculator;

    private TriagePipeline triagePipeline;
    private Task task;

    @BeforeEach
    void setUp() {
        triagePipeline =
                new TriagePipeline(ruleBasedClassifier, gate2Client, triageLogRepository, taskService, slaCalculator);
        ReflectionTestUtils.setField(triagePipeline, "confidenceThreshold", 0.7);

        User owner = new User("Jane", "jane@example.com", "hashed", UserRole.OWNER);
        task = new Task("Production is down", "500 error, critical", TaskSource.EMAIL, owner);
        ReflectionTestUtils.setField(task, "id", 1L);
        ReflectionTestUtils.setField(task, "createdAt", Instant.parse("2026-08-04T00:00:00Z"));

        when(taskService.getOrThrow(1L)).thenReturn(task);
    }

    @Test
    void gate1ConfidentResultSkipsGate2EntirelyAndQueues() {
        when(ruleBasedClassifier.classify(task.getTitle(), task.getDescription()))
                .thenReturn(Optional.of(new RuleBasedClassifier.Result(Category.BUG, Urgency.URGENT)));
        when(slaCalculator.calculateDueAt(any(), eq(Urgency.URGENT)))
                .thenReturn(Instant.parse("2026-08-04T02:00:00Z"));

        triagePipeline.triage(1L);

        // 핵심 검증: Gate 1이 확신하면 Gate 2(LLM)는 아예 호출되지 않는다 — 비용 절감의 근거.
        verify(gate2Client, never()).classify(any(), any());
        verify(triageLogRepository)
                .save(argThat(log -> log.getGate() == TriageGate.RULE_BASED && !log.isLlmCalled()));
        verify(taskService).changeStatus(1L, TaskStatus.TRIAGED);
        verify(taskService).changeStatus(1L, TaskStatus.QUEUED);
        assertThat(task.getCategory()).isEqualTo(Category.BUG);
        assertThat(task.getDueAt()).isEqualTo(Instant.parse("2026-08-04T02:00:00Z"));
    }

    @Test
    void gate1AmbiguousCallsGate2AndQueuesWhenConfidenceAboveThreshold() {
        when(ruleBasedClassifier.classify(any(), any())).thenReturn(Optional.empty());
        when(gate2Client.classify(task.getTitle(), task.getDescription()))
                .thenReturn(new Gate2Client.ClassifyResponse(Category.SUPPORT, Urgency.MEDIUM, 0.9, "reasoning"));

        triagePipeline.triage(1L);

        verify(triageLogRepository).save(argThat(log -> log.getGate() == TriageGate.LLM && log.isLlmCalled()));
        verify(taskService).changeStatus(1L, TaskStatus.TRIAGED);
        verify(taskService).changeStatus(1L, TaskStatus.QUEUED);
    }

    @Test
    void gate2LowConfidenceRoutesToNeedsReview() {
        when(ruleBasedClassifier.classify(any(), any())).thenReturn(Optional.empty());
        when(gate2Client.classify(any(), any()))
                .thenReturn(new Gate2Client.ClassifyResponse(Category.OTHER, Urgency.LOW, 0.4, "reasoning"));

        triagePipeline.triage(1L);

        verify(taskService).changeStatus(1L, TaskStatus.TRIAGED);
        verify(taskService).changeStatus(1L, TaskStatus.NEEDS_REVIEW);
        verify(taskService, never()).changeStatus(1L, TaskStatus.QUEUED);
    }
}
