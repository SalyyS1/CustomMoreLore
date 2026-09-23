package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts YAML declarations into a small immutable AST. */
public final class LayoutCompiler {
    private static final Pattern TOKEN = Pattern.compile("#([A-Za-z0-9_.-]+)(?::(value))?#|(%[^%\\s]+%)|(\\{[^{}\\s]+\\})|(\\[[^\\[\\]\\s]+\\])");
    private static final Set<String> BLOCKS = Set.of("abilities", "elements", "effects", "lore", "native-lore");
    private final Map<String, String> aliases;
    private final FormatPolicies policies;
    private final String source;

    public LayoutCompiler() { this(Map.of(), FormatPolicies.defaults(), "<memory>"); }
    public LayoutCompiler(Map<String, String> aliases, FormatPolicies policies) { this(aliases, policies, "<memory>"); }
    public LayoutCompiler(Map<String, String> aliases, FormatPolicies policies, String source) {
        this.aliases = Map.copyOf(aliases == null ? Map.of() : aliases);
        this.policies = policies == null ? FormatPolicies.defaults() : policies;
        this.source = source == null ? "<memory>" : source;
    }
    public CompiledLayout compile(FormatProfile profile) {
        List<LayoutNode> nodes = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        List<Object> entries = profile.layout();
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            SourceLocation loc = new SourceLocation(source, i + 1, 1);
            try { nodes.add(compileEntry(entry, loc, diagnostics)); }
            catch (RuntimeException e) {
                if (policies.invalidInlineBlock() == FormatPolicies.InvalidInlineBlock.NATIVE
                    && e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("block token"))
                    diagnostics.add("WARNING " + loc + ": " + e.getMessage() + "; native fallback");
                else diagnostics.add("ERROR " + loc + ": " + e.getMessage());
                nodes.add(new NativeFallbackNode(loc));
            }
        }
        if (profile.nativeLoreMode() != FormatProfile.NativeLoreMode.REPLACE && containsNativeToken(nodes)) {
            diagnostics.add("ERROR " + profile.location() + ": native-lore mode and #native-lore# cannot be combined");
        }
        return new CompiledLayout(profile, nodes, diagnostics, policies);
    }
    public Map<String, CompiledLayout> compileAll(FormatConfig config) {
        Map<String, CompiledLayout> result = new LinkedHashMap<>();
        for (FormatProfile p : config.profiles()) result.put(p.id(), new LayoutCompiler(config.aliases(), config.policies(), config.source()).compile(p));
        return Map.copyOf(result);
    }
    private LayoutNode compileEntry(Object entry, SourceLocation loc, List<String> diagnostics) {
        if (entry == null) return new LiteralNode("", loc);
        if (entry instanceof String text) return compileLine(text, loc, diagnostics);
        if (!(entry instanceof Map<?, ?> map)) return new LiteralNode(String.valueOf(entry), loc);
        if (map.containsKey("row")) return compileRow(map.get("row"), loc, diagnostics);
        if (map.containsKey("columns")) return compileColumns(map, loc);
        if (map.containsKey("section")) return compileSection(map.get("section"), loc, diagnostics);
        if (map.containsKey("text")) return compileLine(String.valueOf(map.get("text")), loc, diagnostics);
        return new LiteralNode(String.valueOf(entry), loc);
    }
    private LayoutNode compileColumns(Map<?, ?> map, SourceLocation loc) {
        Object raw = map.get("columns");
        if (!(raw instanceof Collection<?> values)) throw new IllegalArgumentException("columns must be a list");
        List<ColumnsNode.Column> columns = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String text) columns.add(new ColumnsNode.Column(canonical(text), TokenMode.FRAGMENT, "remove"));
            else if (value instanceof Map<?, ?> column) {
                String token = canonical(String.valueOf(valueOr(column, "token", "")));
                if (token.isBlank() || BLOCKS.contains(token)) throw new IllegalArgumentException("columns require scalar tokens");
                TokenMode mode = "value".equalsIgnoreCase(String.valueOf(valueOr(column, "render", "fragment"))) ? TokenMode.VALUE : TokenMode.FRAGMENT;
                columns.add(new ColumnsNode.Column(token, mode, String.valueOf(valueOr(column, "absent", "remove"))));
            } else throw new IllegalArgumentException("column must be a token or map");
        }
        return new ColumnsNode(columns, String.valueOf(valueOr(map, "gap", " ")), loc);
    }
    private LayoutNode compileLine(String text, SourceLocation loc, List<String> diagnostics) {
        Matcher only = TOKEN.matcher(text.trim());
        if (only.matches()) {
            String token = tokenName(only);
            if (BLOCKS.contains(token)) return new BlockTokenNode(token, loc);
        }
        List<LayoutNode> result = parseInline(text, loc);
        if (result.stream().anyMatch(n -> n instanceof BlockTokenNode)) {
            String message = "ERROR " + loc + ": block token used inline";
            if (policies.invalidInlineBlock() == FormatPolicies.InvalidInlineBlock.REJECT_PROFILE) throw new IllegalArgumentException(message);
            diagnostics.add("WARNING " + loc + ": block token used inline; native fallback");
            return new NativeFallbackNode(loc);
        }
        return new InlineNode(result, loc);
    }
    private List<LayoutNode> parseInline(String text, SourceLocation loc) {
        List<LayoutNode> result = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() > end) result.add(new LiteralNode(text.substring(end, matcher.start()), loc));
            if (externalToken(matcher) != null) result.add(new ScalarTokenNode(externalToken(matcher), TokenMode.FRAGMENT, loc));
            else {
                String token = canonical(matcher.group(1));
                if (BLOCKS.contains(token)) result.add(new BlockTokenNode(token, loc));
                else result.add(new ScalarTokenNode(token, matcher.group(2) == null ? TokenMode.FRAGMENT : TokenMode.VALUE, loc));
            }
            end = matcher.end();
        }
        if (end < text.length()) result.add(new LiteralNode(text.substring(end), loc));
        if (result.isEmpty()) result.add(new LiteralNode(text, loc));
        return result;
    }
    private LayoutNode compileRow(Object raw, SourceLocation loc, List<String> diagnostics) {
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("row must be a map");
        Object partsRaw = map.get("parts");
        if (!(partsRaw instanceof Collection<?> parts)) throw new IllegalArgumentException("row.parts must be a list");
        List<List<LayoutNode>> partsNodes = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof String s) partsNodes.add(parseInline(s, loc));
            else if (part instanceof Map<?, ?> p && p.containsKey("token")) {
                String token = canonical(String.valueOf(p.get("token")));
                TokenMode mode = "value".equalsIgnoreCase(String.valueOf(valueOr(p, "render", "fragment"))) ? TokenMode.VALUE : TokenMode.FRAGMENT;
                if (BLOCKS.contains(token)) throw new IllegalArgumentException("block token cannot be a row part");
                partsNodes.add(List.of(new ScalarTokenNode(token, mode, loc)));
            } else if (part instanceof Map<?, ?> p && p.containsKey("text")) partsNodes.add(parseInline(String.valueOf(p.get("text")), loc));
            else partsNodes.add(parseInline(String.valueOf(part), loc));
        }
        RowNode.AbsentPolicy absent = parseAbsent(map.get("absent"));
        RowNode.EmptyPolicy empty = parseEmpty(map.get("empty"));
        return new RowNode(partsNodes, String.valueOf(valueOr(map, "join", "")), absent, empty, loc);
    }
    private LayoutNode compileSection(Object raw, SourceLocation loc, List<String> diagnostics) {
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("section must be a map");
        Condition condition = ConditionEvaluator.parse(map.get("when"));
        Object lines = map.containsKey("lines") ? map.get("lines") : map.get("layout");
        if (!(lines instanceof Collection<?> c)) throw new IllegalArgumentException("section.lines must be a list");
        List<LayoutNode> children = new ArrayList<>();
        int i = 0; for (Object line : c) children.add(compileEntry(line, new SourceLocation(source, loc.line() + i++, 1), diagnostics));
        return new ConditionalNode(condition, children, loc);
    }
    private boolean containsNativeToken(List<LayoutNode> nodes) {
        for (LayoutNode n : nodes) {
            if (n instanceof BlockTokenNode b && "native-lore".equals(b.token())) return true;
            if (n instanceof InlineNode i && i.children().stream().anyMatch(x -> x instanceof BlockTokenNode b && "native-lore".equals(b.token()))) return true;
        }
        return false;
    }
    private String canonical(String token) { return aliases.getOrDefault(token, token); }
    private static String tokenName(Matcher m) { return externalToken(m) != null ? externalToken(m) : m.group(1); }
    private static String externalToken(Matcher m) {
        if (m.group(3) != null) return m.group(3);
        if (m.group(4) != null) return m.group(4);
        return m.group(5);
    }
    private static RowNode.AbsentPolicy parseAbsent(Object value) {
        if (value == null) return RowNode.AbsentPolicy.REMOVE_PART;
        return String.valueOf(value).toLowerCase(Locale.ROOT).contains("keep") ? RowNode.AbsentPolicy.KEEP_EMPTY : RowNode.AbsentPolicy.REMOVE_PART;
    }
    private static RowNode.EmptyPolicy parseEmpty(Object value) {
        if (value == null) return RowNode.EmptyPolicy.REMOVE_ROW;
        return String.valueOf(value).toLowerCase(Locale.ROOT).contains("keep") ? RowNode.EmptyPolicy.KEEP_ROW : RowNode.EmptyPolicy.REMOVE_ROW;
    }
    private static Object valueOr(Map<?, ?> map, String key, Object fallback) {
        return map.containsKey(key) ? map.get(key) : fallback;
    }

    /** Composite inline node is public for adapters and diagnostics, while keeping parser internals simple. */
    public record InlineNode(List<LayoutNode> children, SourceLocation location) implements LayoutNode {
        public InlineNode { children = List.copyOf(children); }
    }
}
