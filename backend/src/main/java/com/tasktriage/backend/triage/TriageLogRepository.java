package com.tasktriage.backend.triage;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TriageLogRepository extends JpaRepository<TriageLog, Long> {

    List<TriageLog> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    long countByGate(TriageGate gate);

    long countByLlmCalledTrue();

    @Query("SELECT AVG(t.confidenceScore) FROM TriageLog t WHERE t.confidenceScore IS NOT NULL")
    Double averageConfidenceScore();
}
