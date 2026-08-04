package com.tasktriage.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class TaskStateMachineTest {

    private static final Set<String> ALLOWED_PAIRS = Set.of(
            "SUBMITTED->TRIAGED",
            "TRIAGED->QUEUED",
            "TRIAGED->NEEDS_REVIEW",
            "NEEDS_REVIEW->QUEUED",
            "NEEDS_REVIEW->ARCHIVED",
            "QUEUED->IN_PROGRESS",
            "QUEUED->ARCHIVED",
            "IN_PROGRESS->DONE",
            "IN_PROGRESS->QUEUED",
            "IN_PROGRESS->ARCHIVED",
            "DONE->ARCHIVED");

    private final TaskStateMachine stateMachine = new TaskStateMachine();

    @ParameterizedTest
    @CsvSource({
        "SUBMITTED, TRIAGED",
        "TRIAGED, QUEUED",
        "TRIAGED, NEEDS_REVIEW",
        "NEEDS_REVIEW, QUEUED",
        "NEEDS_REVIEW, ARCHIVED",
        "QUEUED, IN_PROGRESS",
        "QUEUED, ARCHIVED",
        "IN_PROGRESS, DONE",
        "IN_PROGRESS, QUEUED",
        "IN_PROGRESS, ARCHIVED",
        "DONE, ARCHIVED",
    })
    void allowsDefinedTransitions(TaskStatus from, TaskStatus to) {
        assertThat(stateMachine.isAllowed(from, to)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("everyOtherPair")
    void rejectsEverythingElse(TaskStatus from, TaskStatus to) {
        assertThat(stateMachine.isAllowed(from, to)).isFalse();
    }

    @Test
    void archivedIsTerminal() {
        for (TaskStatus target : TaskStatus.values()) {
            assertThat(stateMachine.isAllowed(TaskStatus.ARCHIVED, target)).isFalse();
        }
    }

    @Test
    void validateThrowsOnDisallowedTransition() {
        assertThatThrownBy(() -> stateMachine.validate(TaskStatus.DONE, TaskStatus.SUBMITTED))
                .isInstanceOf(InvalidTaskTransitionException.class)
                .hasMessageContaining("DONE")
                .hasMessageContaining("SUBMITTED");
    }

    @Test
    void validateDoesNotThrowOnAllowedTransition() {
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.QUEUED, TaskStatus.IN_PROGRESS));
    }

    /** 정의된 허용 전이(ALLOWED_PAIRS)를 제외한 모든 (from, to) 조합 — 전부 거부되어야 정상이다. */
    static Stream<Arguments> everyOtherPair() {
        return Stream.of(TaskStatus.values())
                .flatMap(from -> Stream.of(TaskStatus.values())
                        .filter(to -> from != to)
                        .filter(to -> !ALLOWED_PAIRS.contains(from + "->" + to))
                        .map(to -> Arguments.of(from, to)));
    }
}
