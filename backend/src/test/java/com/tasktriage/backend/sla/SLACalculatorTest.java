package com.tasktriage.backend.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tasktriage.backend.task.Urgency;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SLACalculatorTest {

    @Mock
    private SLARuleRepository slaRuleRepository;

    private SLACalculator slaCalculator;

    @BeforeEach
    void setUp() {
        slaCalculator = new SLACalculator(slaRuleRepository);
    }

    @Test
    void addsResponseTimeHoursToBaseTime() {
        Instant baseTime = Instant.parse("2026-08-04T00:00:00Z");
        when(slaRuleRepository.findByUrgency(Urgency.URGENT))
                .thenReturn(Optional.of(new SLARule(Urgency.URGENT, 2)));

        Instant dueAt = slaCalculator.calculateDueAt(baseTime, Urgency.URGENT);

        assertThat(dueAt).isEqualTo(baseTime.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void throwsWhenNoRuleConfiguredForUrgency() {
        Instant baseTime = Instant.parse("2026-08-04T00:00:00Z");
        when(slaRuleRepository.findByUrgency(Urgency.LOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaCalculator.calculateDueAt(baseTime, Urgency.LOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOW");
    }
}
