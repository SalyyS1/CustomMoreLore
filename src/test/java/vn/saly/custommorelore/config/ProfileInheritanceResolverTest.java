package vn.saly.custommorelore.config;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileInheritanceResolverTest {
    @Test void childOverridesRowByExplicitId() {
        Map<String, Map<String, ?>> profiles = new LinkedHashMap<>();
        profiles.put("base", Map.of("rows", List.of(Map.of("id", "damage", "text", "a"), Map.of("id", "speed", "text", "b"))));
        profiles.put("child", Map.of("parent", "base", "rows", List.of(Map.of("id", "damage", "text", "c"))));
        List<?> rows = (List<?>) new ProfileInheritanceResolver().resolve("child", profiles).get("rows");
        assertEquals("c", ((Map<?, ?>) rows.get(0)).get("text")); assertEquals(2, rows.size());
    }
    @Test void cyclesAreRejected() {
        Map<String, Map<String, ?>> profiles = Map.of("a", Map.of("parent", "b"), "b", Map.of("parent", "a"));
        assertThrows(IllegalArgumentException.class, () -> new ProfileInheritanceResolver().resolveAll(profiles));
    }
    @Test void extendsIsAcceptedAsReadableAlias() {
        Map<String, Map<String, ?>> profiles = Map.of("base", Map.of("priority", 2), "child", Map.of("extends", "base"));
        assertEquals(2, new ProfileInheritanceResolver().resolve("child", profiles).get("priority"));
    }
}
