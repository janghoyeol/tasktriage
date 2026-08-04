package com.tasktriage.backend.task.dto;

import com.tasktriage.backend.task.Task;
import com.tasktriage.backend.task.TaskStatusHistory;
import java.util.List;
import org.springframework.data.domain.Page;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getUrgency(),
                task.getStatus(),
                task.getSource(),
                task.getDueAt(),
                task.getOwner().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    public static TaskDetailResponse toDetailResponse(Task task, List<TaskStatusHistory> history) {
        List<TaskStatusHistoryEntryResponse> entries = history.stream()
                .map(h -> new TaskStatusHistoryEntryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedAt()))
                .toList();

        return new TaskDetailResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getUrgency(),
                task.getStatus(),
                task.getSource(),
                task.getDueAt(),
                task.getOwner().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                entries);
    }

    public static TaskListResponse toListResponse(Page<Task> page) {
        return new TaskListResponse(
                page.getContent().stream().map(TaskMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
}
