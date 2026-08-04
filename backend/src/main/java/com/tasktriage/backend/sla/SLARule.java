package com.tasktriage.backend.sla;

import com.tasktriage.backend.task.Urgency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * urgency별 SLA 응답 기한 정책. Task/User와 FK로 엮이지 않는 독립 설정 테이블 —
 * Task.due_at을 계산할 때 urgency로 조회해서 참고하는 용도로만 쓰인다.
 */
@Entity
@Table(name = "sla_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SLARule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private Urgency urgency;

    @Column(name = "response_time_hours", nullable = false)
    private Integer responseTimeHours;

    public SLARule(Urgency urgency, Integer responseTimeHours) {
        this.urgency = urgency;
        this.responseTimeHours = responseTimeHours;
    }
}
