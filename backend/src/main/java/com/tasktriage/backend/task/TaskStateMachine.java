package com.tasktriage.backend.task;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Task.status가 허용하는 전이 규칙표. 여기 없는 조합은 전부 금지된 전이다.
 *
 * SUBMITTED   → TRIAGED               (Gate 1/2 분류 완료)
 * TRIAGED     → QUEUED, NEEDS_REVIEW  (confidence에 따라 자동 큐잉 or 사람 확인 대기)
 * NEEDS_REVIEW→ QUEUED, ARCHIVED      (사람이 확인 후 큐잉 or 반려)
 * QUEUED      → IN_PROGRESS, ARCHIVED
 * IN_PROGRESS → DONE, QUEUED, ARCHIVED (QUEUED로 되돌아가는 건 작업 보류/재배정 케이스)
 * DONE        → ARCHIVED
 * ARCHIVED    → (없음, 종단 상태)
 */
@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private static Map<TaskStatus, Set<TaskStatus>> buildTransitions() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        transitions.put(TaskStatus.SUBMITTED, EnumSet.of(TaskStatus.TRIAGED));
        transitions.put(TaskStatus.TRIAGED, EnumSet.of(TaskStatus.QUEUED, TaskStatus.NEEDS_REVIEW));
        transitions.put(TaskStatus.NEEDS_REVIEW, EnumSet.of(TaskStatus.QUEUED, TaskStatus.ARCHIVED));
        transitions.put(TaskStatus.QUEUED, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.ARCHIVED));
        transitions.put(
                TaskStatus.IN_PROGRESS,
                EnumSet.of(TaskStatus.DONE, TaskStatus.QUEUED, TaskStatus.ARCHIVED));
        transitions.put(TaskStatus.DONE, EnumSet.of(TaskStatus.ARCHIVED));
        transitions.put(TaskStatus.ARCHIVED, EnumSet.noneOf(TaskStatus.class));
        return transitions;
    }

    public boolean isAllowed(TaskStatus from, TaskStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TaskStatus.class)).contains(to);
    }

    public void validate(TaskStatus from, TaskStatus to) {
        if (!isAllowed(from, to)) {
            throw new InvalidTaskTransitionException(from, to);
        }
    }
}
