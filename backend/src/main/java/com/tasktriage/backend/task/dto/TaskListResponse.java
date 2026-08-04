package com.tasktriage.backend.task.dto;

import java.util.List;

public record TaskListResponse(List<TaskResponse> items, int page, int size, long totalElements) {
}
