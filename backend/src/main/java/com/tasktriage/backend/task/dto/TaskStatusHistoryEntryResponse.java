package com.tasktriage.backend.task.dto;

import com.tasktriage.backend.task.TaskStatus;
import java.time.Instant;

public record TaskStatusHistoryEntryResponse(TaskStatus fromStatus, TaskStatus toStatus, Instant changedAt) {
}
