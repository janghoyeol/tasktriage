package com.tasktriage.backend.task.dto;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.TaskSource;
import com.tasktriage.backend.task.TaskStatus;
import com.tasktriage.backend.task.Urgency;
import java.time.Instant;

public record TaskResponse(
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
        Instant updatedAt) {
}
