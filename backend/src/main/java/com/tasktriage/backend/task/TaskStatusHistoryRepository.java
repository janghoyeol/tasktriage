package com.tasktriage.backend.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Long> {

    List<TaskStatusHistory> findByTaskIdOrderByChangedAtAsc(Long taskId);
}
