package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Process-scoped immutable snapshot. Failed reloads never replace the active snapshot. */
public final class ProfileRegistry {
    public record Snapshot(FormatConfig config, Map<String, CompiledLayout> layouts, long revision) {
        public Snapshot { layouts = Map.copyOf(layouts); }
        public CompiledLayout layout(String profileId) { return layouts.get(profileId); }
    }
    public record ReloadResult(boolean activated, Snapshot snapshot, List<String> diagnostics) {}
    private final AtomicReference<Snapshot> active = new AtomicReference<>();
    private final FormatValidator validator = new FormatValidator();
    private long revision;

    public Snapshot active() { return active.get(); }
    public synchronized ReloadResult reload(FormatConfig candidate) {
        return reload(candidate, Set.of(), Set.of());
    }
    public synchronized ReloadResult reload(FormatConfig candidate, Set<String> knownTokens, Set<String> rawValueTokens) {
        if (candidate == null) return failed("candidate config is null");
        List<FormatValidator.Diagnostic> diagnostics = validator.validate(candidate, knownTokens, rawValueTokens);
        boolean errors = diagnostics.stream().anyMatch(d -> d.severity() == FormatValidator.Severity.ERROR);
        if (errors) return failed(diagnostics.stream().map(Object::toString).toList());
        Map<String, CompiledLayout> compiled = new LayoutCompiler(candidate.aliases(), candidate.policies(), candidate.source()).compileAll(candidate);
        List<String> compileErrors = compiled.values().stream().flatMap(c -> c.diagnostics().stream()).filter(d -> d.startsWith("ERROR")).toList();
        if (!compileErrors.isEmpty()) return failed(compileErrors);
        Snapshot next = new Snapshot(candidate, compiled, ++revision);
        active.set(next);
        return new ReloadResult(true, next, List.of());
    }
    private ReloadResult failed(String... diagnostics) { return new ReloadResult(false, active.get(), List.of(diagnostics)); }
    private ReloadResult failed(List<String> diagnostics) { return new ReloadResult(false, active.get(), List.copyOf(diagnostics)); }
}
