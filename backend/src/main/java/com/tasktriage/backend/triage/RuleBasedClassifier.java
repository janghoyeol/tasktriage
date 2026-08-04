package com.tasktriage.backend.triage;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Urgency;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Gate 1: 키워드/정규식 기반 1차 분류. LLM을 호출하지 않는다.
 *
 * category는 키워드가 하나도 안 걸리면 애매한 것으로 보고 Optional.empty()를 반환해
 * Gate 2(LLM)로 넘긴다. urgency는 명시적 신호(URGENT/HIGH/LOW 키워드)가 없으면 MEDIUM으로
 * 기본 처리한다 — "특별히 급하다는 말이 없다"는 것 자체가 판단 근거로 충분하다고 보고,
 * urgency 신호 없음만으로 Gate 2를 호출하지는 않는다.
 */
@Component
public class RuleBasedClassifier {

    public record Result(Category category, Urgency urgency) {
    }

    private record CategoryRule(Category category, Pattern pattern) {
    }

    private record UrgencyRule(Urgency urgency, Pattern pattern) {
    }

    private static final List<CategoryRule> CATEGORY_RULES = List.of(
            new CategoryRule(
                    Category.BUG,
                    compile("bug|error|crash(ed|es)?|broken|exception|500 error|not working|"
                            + "doesn'?t work|fails?|failing")),
            new CategoryRule(
                    Category.BILLING,
                    compile("invoice|charged?|payment|refund|billing|subscription|receipt")),
            new CategoryRule(
                    Category.FEATURE_REQUEST,
                    compile("feature request|please add|would be nice|can we have|new feature|"
                            + "could you add")),
            new CategoryRule(
                    Category.SUPPORT,
                    compile("how do i|how to|help me|question|can you explain|not sure how")));

    private static final List<UrgencyRule> URGENCY_RULES = List.of(
            new UrgencyRule(
                    Urgency.URGENT,
                    compile("urgent|asap|immediately|critical|production( is)? down|emergency|"
                            + "right now|blocking everyone")),
            new UrgencyRule(Urgency.HIGH, compile("important|high priority|soon|blocking")),
            new UrgencyRule(Urgency.LOW, compile("no rush|whenever|low priority|not urgent")));

    private static Pattern compile(String alternatives) {
        return Pattern.compile("\\b(" + alternatives + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    public Optional<Result> classify(String title, String description) {
        String text = title + " " + (description == null ? "" : description);

        Category category = matchCategory(text);
        if (category == null) {
            return Optional.empty();
        }

        Urgency urgency = matchUrgency(text).orElse(Urgency.MEDIUM);
        return Optional.of(new Result(category, urgency));
    }

    private Category matchCategory(String text) {
        return CATEGORY_RULES.stream()
                .filter(rule -> rule.pattern().matcher(text).find())
                .map(CategoryRule::category)
                .findFirst()
                .orElse(null);
    }

    private Optional<Urgency> matchUrgency(String text) {
        return URGENCY_RULES.stream()
                .filter(rule -> rule.pattern().matcher(text).find())
                .map(UrgencyRule::urgency)
                .findFirst();
    }
}
