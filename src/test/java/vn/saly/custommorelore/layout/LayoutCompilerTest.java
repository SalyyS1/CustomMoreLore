package vn.saly.custommorelore.layout;

import org.junit.jupiter.api.Test;
import vn.saly.custommorelore.config.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LayoutCompilerTest {
    private static FormatProfile profile(List<Object> layout) {
        return new FormatProfile("p", FormatProfile.Bucket.DEFAULT, null, 0,
            FormatProfile.TextMode.LITERAL, FormatProfile.NativeLoreMode.REPLACE, layout, Map.of(), null);
    }
    @Test void preservesLiteralWhitespaceAndParsesTokens() {
        CompiledLayout compiled = new LayoutCompiler().compile(profile(List.of("#damage#     #speed#")));
        assertTrue(compiled.valid());
        LayoutCompiler.InlineNode node = assertInstanceOf(LayoutCompiler.InlineNode.class, compiled.nodes().get(0));
        assertEquals(3, node.children().size());
        assertEquals("     ", ((LiteralNode) node.children().get(1)).text());
    }
    @Test void blockMustOccupyLine() {
        CompiledLayout compiled = new LayoutCompiler().compile(profile(List.of("<gray>#abilities#")));
        assertTrue(compiled.nodes().get(0) instanceof NativeFallbackNode);
        assertFalse(compiled.diagnostics().isEmpty());
    }
    @Test void structuredRowCompilesParts() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("parts", List.of(Map.of("token", "damage"), Map.of("token", "speed", "render", "value")));
        row.put("join", " | ");
        CompiledLayout compiled = new LayoutCompiler(Map.of("damage", "attack-damage"), FormatPolicies.defaults())
            .compile(profile(List.of(Map.of("row", row))));
        RowNode n = assertInstanceOf(RowNode.class, compiled.nodes().get(0));
        assertEquals("attack-damage", ((ScalarTokenNode) n.parts().get(0).get(0)).token());
        assertEquals(TokenMode.VALUE, ((ScalarTokenNode) n.parts().get(1).get(0)).mode());
    }
    @Test void acceptsMythicLibAndPlaceholderTokenForms() {
        CompiledLayout compiled = new LayoutCompiler().compile(profile(List.of("{player_name} %player_name% [level]")));
        LayoutCompiler.InlineNode node = assertInstanceOf(LayoutCompiler.InlineNode.class, compiled.nodes().get(0));
        assertEquals(5, node.children().size());
        assertEquals("{player_name}", ((ScalarTokenNode) node.children().get(0)).token());
        assertEquals("%player_name%", ((ScalarTokenNode) node.children().get(2)).token());
        assertEquals("[level]", ((ScalarTokenNode) node.children().get(4)).token());
    }
}
