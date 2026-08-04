package com.tasktriage.backend.triage;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageLogRepository extends JpaRepository<TriageLog, Long> {

    List<TriageLog> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
