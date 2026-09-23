package vn.saly.custommorelore.layout;

import org.junit.jupiter.api.Test;
import vn.saly.custommorelore.config.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LoreRendererTest {
    private static final FragmentProvider PROVIDER = (token, mode, context) -> switch (token) {
        case "damage" -> ProviderResult.present("10");
        case "speed" -> ProviderResult.zero("test");
        case "abilities" -> ProviderResult.lines(List.of("Ability A", "Ability B"), "test");
        case "missing" -> ProviderResult.missing();
        default -> ProviderResult.unknown("not available");
    };
    private static LoreBuildContext context(List<String> nativeLore) { return LoreBuildContext.of("i", "SWORD", nativeLore, PROVIDER); }
    private static FormatProfile profile(List<Object> layout) { return new FormatProfile("p", FormatProfile.Bucket.DEFAULT, null, 0, FormatProfile.TextMode.LITERAL, FormatProfile.NativeLoreMode.REPLACE, layout, Map.of(), null); }
    @Test void rendersZeroAndPreservesSpaces() {
        CompiledLayout c = new LayoutCompiler().compile(profile(List.of("#damage#     #speed#")));
        RenderResult result = new LoreRenderer().render(context(List.of("native")), c);
        assertEquals(List.of("10     0"), result.lines());
    }
    @Test void removesMissingRowPartsWithoutDanglingJoin() {
        Map<String, Object> row = Map.of("parts", List.of(Map.of("token", "damage"), Map.of("token", "missing")), "join", " | ");
        CompiledLayout c = new LayoutCompiler().compile(profile(List.of(Map.of("row", row))));
        assertEquals(List.of("10"), new LoreRenderer().render(context(List.of("native")), c).lines());
    }
    @Test void blocksStayMultiline() {
        CompiledLayout c = new LayoutCompiler().compile(profile(List.of("#abilities#")));
        assertEquals(List.of("Ability A", "Ability B"), new LoreRenderer().render(context(List.of("native")), c).lines());
    }
    @Test void unknownNativeFallsBack() {
        CompiledLayout c = new LayoutCompiler().compile(profile(List.of("#unknown#")));
        RenderResult result = new LoreRenderer().render(context(List.of("native")), c);
        assertTrue(result.nativeFallback());
        assertEquals(List.of("native"), result.lines());
    }
}
