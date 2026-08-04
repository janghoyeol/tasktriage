package com.tasktriage.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Urgency;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleBasedClassifierTest {

    private final RuleBasedClassifier classifier = new RuleBasedClassifier();

    @Test
    void classifiesClearBugWithExplicitUrgency() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("Production is down", "Login throws a 500 error, this is critical");

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo(Category.BUG);
        assertThat(result.get().urgency()).isEqualTo(Urgency.URGENT);
    }

    @Test
    void defaultsToMediumUrgencyWhenNoExplicitSignal() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("App crashes on startup", "Happens every time I open it");

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo(Category.BUG);
        assertThat(result.get().urgency()).isEqualTo(Urgency.MEDIUM);
    }

    @Test
    void classifiesBillingRequest() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("Invoice #4521 charged twice", "Need this refunded, no rush though");

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo(Category.BILLING);
        assertThat(result.get().urgency()).isEqualTo(Urgency.LOW);
    }

    @Test
    void classifiesFeatureRequest() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("Please add dark mode", "Would be nice to have this soon");

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo(Category.FEATURE_REQUEST);
        assertThat(result.get().urgency()).isEqualTo(Urgency.HIGH);
    }

    @Test
    void classifiesSupportQuestion() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("How do I export my data?", null);

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo(Category.SUPPORT);
        assertThat(result.get().urgency()).isEqualTo(Urgency.MEDIUM);
    }

    @Test
    void returnsEmptyWhenCategoryIsAmbiguous() {
        Optional<RuleBasedClassifier.Result> result =
                classifier.classify("Something about the dashboard", "Not sure what's going on with it");

        assertThat(result).isEmpty();
    }

    @Test
    void handlesNullDescriptionWithoutError() {
        Optional<RuleBasedClassifier.Result> result = classifier.classify("Random title with no keywords", null);

        assertThat(result).isEmpty();
    }
}
