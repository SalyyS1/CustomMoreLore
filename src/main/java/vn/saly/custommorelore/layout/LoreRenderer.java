package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.FormatProfile;
import vn.saly.custommorelore.config.FormatPolicies;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.*;

/** Renders an immutable layout against one build context. */
public final class LoreRenderer {
    private record NodeResult(List<String> lines, String text, boolean visible, boolean nativeFallback, boolean error) {
        static NodeResult text(String value) { return new NodeResult(List.of(), value, value != null && !value.isEmpty(), false, false); }
        static NodeResult lines(List<String> value) { return new NodeResult(List.copyOf(value), "", !value.isEmpty(), false, false); }
        static NodeResult hidden() { return text(""); }
    }
    private final ConditionEvaluator conditions;
    public LoreRenderer() { this.conditions = new ConditionEvaluator(); }
    public LoreRenderer(ConditionEvaluator conditions) { this.conditions = conditions == null ? new ConditionEvaluator() : conditions; }
    public RenderResult render(LoreBuildContext context, CompiledLayout layout) {
        if (context == null || layout == null || !layout.valid()) return RenderResult.nativeLore(context, "invalid or missing compiled layout");
        FragmentResolver resolver = new FragmentResolver(layout.policies());
        // Compiler diagnostics may be non-fatal (for example inline block native fallback), while policies are carried by resolver below.
        List<String> diagnostics = new ArrayList<>(layout.diagnostics());
        List<String> output = new ArrayList<>();
        for (LayoutNode node : layout.nodes()) {
            NodeResult result = renderNode(node, context, layout.profile(), layout.policies(), resolver, diagnostics);
            if (result.nativeFallback() || result.error()) return RenderResult.nativeLore(context, "provider state requires native fallback");
            if (!result.lines().isEmpty()) output.addAll(result.lines());
            else if (result.visible()) output.add(result.text());
        }
        List<String> nativeLore = context.nativeLore() == null ? List.of() : context.nativeLore();
        if (layout.profile().nativeLoreMode() == FormatProfile.NativeLoreMode.PREPEND) {
            List<String> result = new ArrayList<>(nativeLore); result.addAll(output); output = result;
        } else if (layout.profile().nativeLoreMode() == FormatProfile.NativeLoreMode.APPEND) {
            output.addAll(nativeLore);
        }
        return new RenderResult(output, false, diagnostics);
    }
    public RenderResult render(CompiledLayout layout, LoreBuildContext context) { return render(context, layout); }
    private NodeResult renderNode(LayoutNode node, LoreBuildContext context, FormatProfile profile, FormatPolicies policies,
                                  FragmentResolver resolver, List<String> diagnostics) {
        if (node instanceof NativeFallbackNode) return new NodeResult(List.of(), "", false, true, false);
        if (node instanceof LiteralNode literal) return NodeResult.text(staticText(literal.text(), profile.textMode()));
        if (node instanceof SeparatorNode separator) return NodeResult.text(staticText(separator.text(), profile.textMode()));
        if (node instanceof ScalarTokenNode token) {
            FragmentResolver.Resolution resolution = resolver.resolve(token.token(), token.mode(), context);
            if (resolution.nativeFallback() || resolution.error()) return new NodeResult(List.of(), "", false, resolution.nativeFallback(), resolution.error());
            ProviderResult result = resolution.result();
            if (!resolution.visible()) return NodeResult.hidden();
            return NodeResult.text(result.state() == ProviderResult.State.ZERO ? "0" : result.text());
        }
        if (node instanceof BlockTokenNode block) {
            if ("native-lore".equals(block.token())) return NodeResult.lines(context.nativeLore() == null ? List.of() : context.nativeLore());
            FragmentResolver.Resolution resolution = resolver.resolve(block.token(), TokenMode.FRAGMENT, context);
            if (resolution.nativeFallback() || resolution.error()) return new NodeResult(List.of(), "", false, resolution.nativeFallback(), resolution.error());
            if (!resolution.visible()) return NodeResult.hidden();
            ProviderResult result = resolution.result();
            return result.lines().isEmpty() ? NodeResult.lines(List.of(result.text())) : NodeResult.lines(result.lines());
        }
        if (node instanceof LayoutCompiler.InlineNode inline) {
            StringBuilder text = new StringBuilder();
            for (LayoutNode child : inline.children()) {
                NodeResult part = renderNode(child, context, profile, policies, resolver, diagnostics);
                if (part.nativeFallback() || part.error()) return part;
                if (!part.lines().isEmpty()) return new NodeResult(List.of(), "", false, true, false);
                text.append(part.text());
            }
            return NodeResult.text(text.toString());
        }
        if (node instanceof RowNode row) {
            List<String> parts = new ArrayList<>();
            for (List<LayoutNode> partNodes : row.parts()) {
                NodeResult part = renderPart(partNodes, context, profile, policies, resolver, diagnostics);
                if (part.nativeFallback() || part.error()) return part;
                if (part.visible() || row.absent() == RowNode.AbsentPolicy.KEEP_EMPTY) parts.add(part.text());
            }
            if (parts.isEmpty() && row.empty() == RowNode.EmptyPolicy.REMOVE_ROW) return NodeResult.hidden();
            return NodeResult.text(String.join(staticText(row.join(), profile.textMode()), parts));
        }
        if (node instanceof ColumnsNode columns) {
            List<String> values = new ArrayList<>();
            String gap = staticText(columns.gap(), profile.textMode());
            for (ColumnsNode.Column column : columns.columns()) {
                FragmentResolver.Resolution resolution = resolver.resolve(column.token(), column.mode(), context);
                if (resolution.nativeFallback() || resolution.error()) return new NodeResult(List.of(), "", false, resolution.nativeFallback(), resolution.error());
                if (!resolution.visible()) {
                    if ("keep".equalsIgnoreCase(column.absent()) || "empty".equalsIgnoreCase(column.absent())) values.add("");
                    continue;
                }
                ProviderResult value = resolution.result();
                if (!value.lines().isEmpty()) return new NodeResult(List.of(), "", false, true, false);
                values.add(value.state() == ProviderResult.State.ZERO ? "0" : value.text());
            }
            return NodeResult.text(String.join(gap, values));
        }
        if (node instanceof ConditionalNode conditional) {
            ConditionEvaluator.Truth truth = conditions.evaluate(conditional.condition(), context);
            if (truth == ConditionEvaluator.Truth.FALSE) return NodeResult.hidden();
            if (truth == ConditionEvaluator.Truth.UNKNOWN) {
                return policies.conditionUnknown() == FormatPolicies.ConditionUnknown.KEEP_NATIVE
                    ? new NodeResult(List.of(), "", false, true, false) : NodeResult.hidden();
            }
            List<String> lines = new ArrayList<>();
            for (LayoutNode child : conditional.children()) {
                NodeResult result = renderNode(child, context, profile, policies, resolver, diagnostics);
                if (result.nativeFallback() || result.error()) return result;
                if (!result.lines().isEmpty()) lines.addAll(result.lines()); else if (result.visible()) lines.add(result.text());
            }
            return NodeResult.lines(lines);
        }
        return NodeResult.hidden();
    }
    private NodeResult renderPart(List<LayoutNode> nodes, LoreBuildContext context, FormatProfile profile, FormatPolicies policies,
                                  FragmentResolver resolver, List<String> diagnostics) {
        StringBuilder text = new StringBuilder();
        for (LayoutNode node : nodes) {
            NodeResult result = renderNode(node, context, profile, policies, resolver, diagnostics);
            if (result.nativeFallback() || result.error()) return result;
            if (!result.lines().isEmpty()) return new NodeResult(List.of(), "", false, true, false);
            text.append(result.text());
        }
        return NodeResult.text(text.toString());
    }
    private static String staticText(String text, FormatProfile.TextMode mode) {
        if (text == null) return "";
        if (mode == FormatProfile.TextMode.MINIMESSAGE) {
            try { return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(text)); }
            catch (RuntimeException ignored) { return text; }
        }
        if (mode != FormatProfile.TextMode.LEGACY) return text;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) { out.append('§').append(text.charAt(++i)); }
            else out.append(c);
        }
        return out.toString();
    }
}
