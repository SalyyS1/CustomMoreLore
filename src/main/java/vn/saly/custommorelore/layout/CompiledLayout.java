package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.FormatProfile;
import vn.saly.custommorelore.config.FormatPolicies;
import java.util.List;

/** Immutable AST and profile metadata used by every item build. */
public final class CompiledLayout {
    private final FormatProfile profile;
    private final List<LayoutNode> nodes;
    private final List<String> diagnostics;
    private final FormatPolicies policies;
    public CompiledLayout(FormatProfile profile, List<LayoutNode> nodes, List<String> diagnostics) {
        this(profile, nodes, diagnostics, FormatPolicies.defaults());
    }
    public CompiledLayout(FormatProfile profile, List<LayoutNode> nodes, List<String> diagnostics, FormatPolicies policies) {
        this.profile = profile;
        this.nodes = List.copyOf(nodes);
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        this.policies = policies == null ? FormatPolicies.defaults() : policies;
    }
    public FormatProfile profile() { return profile; }
    public List<LayoutNode> nodes() { return nodes; }
    public List<String> diagnostics() { return diagnostics; }
    public FormatPolicies policies() { return policies; }
    public boolean valid() { return diagnostics.stream().noneMatch(s -> s.startsWith("ERROR")); }
}
