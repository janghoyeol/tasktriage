package com.tasktriage.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tasktriage.backend.task.dto.CreateTaskRequest;
import com.tasktriage.backend.user.User;
import com.tasktriage.backend.user.UserRepository;
import com.tasktriage.backend.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskStatusHistoryRepository taskStatusHistoryRepository;

    @Mock
    private TaskStateMachine taskStateMachine;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService =
                new TaskService(taskRepository, taskStatusHistoryRepository, taskStateMachine, userRepository);
    }

    @Test
    void createTaskAssignsAuthenticatedUserAsOwnerAndRecordsInitialHistory() {
        User owner = new User("Jane", "jane@example.com", "hashed", UserRole.OWNER);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTaskRequest request = new CreateTaskRequest("Fix login", "desc", TaskSource.EMAIL);
        Task task = taskService.createTask("jane@example.com", request);

        assertThat(task.getOwner()).isEqualTo(owner);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
        verify(taskStatusHistoryRepository).save(any(TaskStatusHistory.class));
    }

    @Test
    void createTaskThrowsWhenAuthenticatedUserMissing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        CreateTaskRequest request = new CreateTaskRequest("Fix login", "desc", TaskSource.EMAIL);

        assertThatThrownBy(() -> taskService.createTask("ghost@example.com", request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changeStatusAppliesValidTransitionAndRecordsHistory() {
        Task task = newTaskWithId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.changeStatus(1L, TaskStatus.TRIAGED);

        verify(taskStateMachine).validate(TaskStatus.SUBMITTED, TaskStatus.TRIAGED);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TRIAGED);
        verify(taskStatusHistoryRepository).save(any(TaskStatusHistory.class));
    }

    @Test
    void changeStatusLeavesTaskUntouchedWhenTransitionRejected() {
        Task task = newTaskWithId(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new InvalidTaskTransitionException(TaskStatus.SUBMITTED, TaskStatus.DONE))
                .when(taskStateMachine)
                .validate(TaskStatus.SUBMITTED, TaskStatus.DONE);

        assertThatThrownBy(() -> taskService.changeStatus(1L, TaskStatus.DONE))
                .isInstanceOf(InvalidTaskTransitionException.class);

        verify(taskStatusHistoryRepository, never()).save(any());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
    }

    @Test
    void getOrThrowThrowsWhenTaskMissing() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getOrThrow(999L)).isInstanceOf(TaskNotFoundException.class);
    }

    private Task newTaskWithId(Long id) {
        User owner = new User("Jane", "jane@example.com", "hashed", UserRole.OWNER);
        Task task = new Task("Fix login", "desc", TaskSource.EMAIL, owner);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
