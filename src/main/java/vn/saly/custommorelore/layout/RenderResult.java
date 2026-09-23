package vn.saly.custommorelore.layout;
import java.util.List;
public record RenderResult(List<String> lines, boolean nativeFallback, List<String> diagnostics) {
    public RenderResult { lines = List.copyOf(lines == null ? List.of() : lines); diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics); }
    public static RenderResult nativeLore(LoreBuildContext context, String reason) { return new RenderResult(context == null ? List.of() : context.nativeLore(), true, List.of(reason)); }
}
