package com.tasktriage.backend.sla;

import com.tasktriage.backend.task.Urgency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SLARuleRepository extends JpaRepository<SLARule, Long> {

    Optional<SLARule> findByUrgency(Urgency urgency);
}
