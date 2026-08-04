package com.tasktriage.backend.task;

import com.tasktriage.backend.task.dto.CreateTaskRequest;
import com.tasktriage.backend.task.dto.TaskDetailResponse;
import com.tasktriage.backend.task.dto.TaskListResponse;
import com.tasktriage.backend.task.dto.TaskMapper;
import com.tasktriage.backend.task.dto.TaskResponse;
import com.tasktriage.backend.task.dto.UpdateStatusRequest;
import com.tasktriage.backend.triage.TriageLogRepository;
import com.tasktriage.backend.triage.TriagePipeline;
import com.tasktriage.backend.triage.dto.TriageLogResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final TriageLogRepository triageLogRepository;
    private final TriagePipeline triagePipeline;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            Authentication authentication, @Valid @RequestBody CreateTaskRequest request) {
        Task created = taskService.createTask(authentication.getName(), request);

        try {
            triagePipeline.triage(created.getId());
        } catch (RuntimeException e) {
            // 분류 파이프라인이 실패해도(예: Gate 2 서비스 다운) 작업 등록 자체는 유지한다.
            // Task는 SUBMITTED 상태로 남고, 이후 재시도나 수동 분류로 이어질 수 있다.
            log.warn("Triage pipeline failed for task {}", created.getId(), e);
        }

        Task result = taskService.getOrThrow(created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.toResponse(result));
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
