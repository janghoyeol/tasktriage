package com.tasktriage.backend.task;

import com.tasktriage.backend.user.User;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    // category/urgency는 등록 시점엔 비어있고 Gate 1/2 분류가 끝나야 채워진다.
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskSource source;

    @Column(name = "due_at")
    private Instant dueAt;

    // Task(N) : User(1) 관계에서 Task가 FK(owner_id)를 들고 있으므로 연관관계의 주인이다.
    // fetch=LAZY를 명시한 이유: @ManyToOne의 JPA 기본값은 EAGER라서 명시하지 않으면 Task를
    // 조회할 때마다 User까지 즉시 JOIN해서 가져온다. Task 목록처럼 User 정보가 당장 필요 없는
    // 화면에서도 매번 JOIN이 붙는 걸 피하려고 LAZY로 바꿔둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Task(String title, String description, TaskSource source, User owner) {
        this.title = title;
        this.description = description;
        this.source = source;
        this.owner = owner;
        this.status = TaskStatus.SUBMITTED;
    }

    public void applyClassification(Category category, Urgency urgency) {
        this.category = category;
        this.urgency = urgency;
    }

    public void scheduleDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    /**
     * 상태를 강제로 바꾸기만 한다 — "이 전이가 허용되는지"는 검증하지 않는다.
     * 전이 규칙(허용/불허 조합)은 상태 머신을 다루는 서비스 레이어(다음 작업)에서
     * 검증한 뒤에만 이 메서드를 호출하도록 설계한다.
     */
    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
    }
}
