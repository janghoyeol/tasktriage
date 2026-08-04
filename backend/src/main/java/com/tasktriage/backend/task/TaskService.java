package com.tasktriage.backend.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final TaskStateMachine taskStateMachine;

    @Transactional
    public Task changeStatus(Long taskId, TaskStatus newStatus) {
        Task task = getOrThrow(taskId);
        TaskStatus previousStatus = task.getStatus();

        taskStateMachine.validate(previousStatus, newStatus);

        task.changeStatus(newStatus);
        taskStatusHistoryRepository.save(new TaskStatusHistory(task, previousStatus, newStatus));

        return task;
    }

    Task getOrThrow(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
