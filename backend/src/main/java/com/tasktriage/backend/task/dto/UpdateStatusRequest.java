package com.tasktriage.backend.task.dto;

import com.tasktriage.backend.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TaskStatus status) {
}
