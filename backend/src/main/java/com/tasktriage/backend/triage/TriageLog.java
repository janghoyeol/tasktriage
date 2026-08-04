package com.tasktriage.backend.triage;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Task;
import com.tasktriage.backend.task.Urgency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 분류 판단 기록. 한 Task가 Gate 1 → Gate 2를 거치며 여러 건 쌓일 수 있어 append-only다.
 * Task 쪽엔 이 엔티티로의 컬렉션 필드를 두지 않는다(단방향).
 */
@Entity
@Table(name = "triage_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TriageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriageGate gate;

    @Column(name = "llm_called", nullable = false)
    private boolean llmCalled;

    // Gate 1(규칙 기반)은 confidence 개념이 없어 null
    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_category", length = 30)
    private Category resultCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_urgency", length = 10)
    private Urgency resultUrgency;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TriageLog(
            Task task,
            TriageGate gate,
            boolean llmCalled,
            Double confidenceScore,
            Category resultCategory,
            Urgency resultUrgency) {
        this.task = task;
        this.gate = gate;
        this.llmCalled = llmCalled;
        this.confidenceScore = confidenceScore;
        this.resultCategory = resultCategory;
        this.resultUrgency = resultUrgency;
    }
}
