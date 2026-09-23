package vn.saly.custommorelore.layout;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {
    private final FragmentProvider provider = (token, mode, context) -> token.equals("damage") ? ProviderResult.zero("test") : ProviderResult.unknown("missing context");
    private final LoreBuildContext context = LoreBuildContext.of("item", "SWORD", List.of(), provider);
    @Test void zeroIsPresentForHas() {
        Condition c = ConditionEvaluator.parse("has:damage");
        assertEquals(ConditionEvaluator.Truth.TRUE, new ConditionEvaluator().evaluate(c, context));
    }
    @Test void allAndAnyUseUnknownLogic() {
        Condition c = ConditionEvaluator.parse(Map.of("all", List.of("has:damage", "has:other")));
        assertEquals(ConditionEvaluator.Truth.UNKNOWN, new ConditionEvaluator().evaluate(c, context));
        Condition any = ConditionEvaluator.parse(Map.of("any", List.of("has:other", "type:SWORD")));
        assertEquals(ConditionEvaluator.Truth.TRUE, new ConditionEvaluator().evaluate(any, context));
    }
}
