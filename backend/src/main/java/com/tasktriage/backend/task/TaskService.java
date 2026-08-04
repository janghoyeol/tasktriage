package com.tasktriage.backend.task;

import com.tasktriage.backend.task.dto.CreateTaskRequest;
import com.tasktriage.backend.user.User;
import com.tasktriage.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final TaskStateMachine taskStateMachine;
    private final UserRepository userRepository;

    @Transactional
    public Task createTask(String ownerEmail, CreateTaskRequest request) {
        User owner = userRepository
                .findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + ownerEmail));

        Task task = new Task(request.title(), request.description(), request.source(), owner);
        Task saved = taskRepository.save(task);
        // 최초 상태(SUBMITTED)도 이력에 남긴다 — fromStatus는 "이전 상태 없음"을 뜻하는 null.
        taskStatusHistoryRepository.save(new TaskStatusHistory(saved, null, TaskStatus.SUBMITTED));

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasks(TaskStatus status, Urgency urgency, Pageable pageable) {
        return taskRepository.findAll(TaskSpecifications.withFilters(status, urgency), pageable);
    }

    @Transactional
    public Task changeStatus(Long taskId, TaskStatus newStatus) {
        Task task = getOrThrow(taskId);
        TaskStatus previousStatus = task.getStatus();

        taskStateMachine.validate(previousStatus, newStatus);

        task.changeStatus(newStatus);
        taskStatusHistoryRepository.save(new TaskStatusHistory(task, previousStatus, newStatus));

        return task;
    }

    public Task getOrThrow(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
