package com.tasktriage.backend.task;

import com.tasktriage.backend.task.dto.CreateTaskRequest;
import com.tasktriage.backend.task.dto.TaskDetailResponse;
import com.tasktriage.backend.task.dto.TaskListResponse;
import com.tasktriage.backend.task.dto.TaskMapper;
import com.tasktriage.backend.task.dto.TaskResponse;
import com.tasktriage.backend.task.dto.UpdateStatusRequest;
import com.tasktriage.backend.triage.TriageLogRepository;
import com.tasktriage.backend.triage.dto.TriageLogResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final TriageLogRepository triageLogRepository;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            Authentication authentication, @Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.toResponse(task));
    }

    @GetMapping
    public TaskListResponse listTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Urgency urgency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Task> result = taskService.listTasks(status, urgency, PageRequest.of(page, size));
        return TaskMapper.toListResponse(result);
    }

    @GetMapping("/{id}")
    public TaskDetailResponse getTask(@PathVariable Long id) {
        Task task = taskService.getOrThrow(id);
        List<TaskStatusHistory> history = taskStatusHistoryRepository.findByTaskIdOrderByChangedAtAsc(id);
        return TaskMapper.toDetailResponse(task, history);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        Task task = taskService.changeStatus(id, request.status());
        return TaskMapper.toResponse(task);
    }

    @GetMapping("/{id}/triage-log")
    public List<TriageLogResponse> getTriageLog(@PathVariable Long id) {
        taskService.getOrThrow(id);
        return triageLogRepository.findByTaskIdOrderByCreatedAtAsc(id).stream()
                .map(TriageLogResponse::from)
                .toList();
    }
}
