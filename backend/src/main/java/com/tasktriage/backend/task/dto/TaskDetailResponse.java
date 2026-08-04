package com.tasktriage.backend.task.dto;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.TaskSource;
import com.tasktriage.backend.task.TaskStatus;
import com.tasktriage.backend.task.Urgency;
import java.time.Instant;
import java.util.List;

/** OpenAPI의 TaskResponse allOf 확장을 그대로 평탄화한 모양 — TaskResponse 필드 전부 + statusHistory. */
public record TaskDetailResponse(
        Long id,
        String title,
        String description,
        Category category,
        Urgency urgency,
        TaskStatus status,
        TaskSource source,
        Instant dueAt,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt,
        List<TaskStatusHistoryEntryResponse> statusHistory) {
}
