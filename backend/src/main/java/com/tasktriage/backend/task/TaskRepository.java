package com.tasktriage.backend.task;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM Task t GROUP BY t.status")
    List<StatusCount> countGroupedByStatus();

    long countByStatusNotInAndDueAtLessThan(List<TaskStatus> excludedStatuses, Instant threshold);

    long countByStatusNotInAndDueAtBetween(List<TaskStatus> excludedStatuses, Instant from, Instant to);

    interface StatusCount {
        TaskStatus getStatus();

        long getCount();
    }
}
