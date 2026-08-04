package com.tasktriage.backend.task;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> withFilters(TaskStatus status, Urgency urgency) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (urgency != null) {
                predicates.add(cb.equal(root.get("urgency"), urgency));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
