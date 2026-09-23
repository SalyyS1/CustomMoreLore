package vn.saly.custommorelore.config;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Schema-level checks that do not require a live MMOItems server. */
public final class FormatValidator {
    private static final Pattern TOKEN = Pattern.compile("#([A-Za-z0-9_.-]+)(?::([A-Za-z0-9_.-]+))?#");
    private static final Set<String> BLOCKS = Set.of("abilities", "elements", "effects", "lore", "native-lore");
    public record Diagnostic(Severity severity, String message, SourceLocation location) {}
    public enum Severity { ERROR, WARNING }
    public List<Diagnostic> validate(FormatConfig config, Set<String> knownTokens) {
        return validate(config, knownTokens, Set.of());
    }
    /** Validate with separate raw numeric adapter IDs; fragment support does not imply value support. */
    public List<Diagnostic> validate(FormatConfig config, Set<String> knownTokens, Set<String> rawValueTokens) {
        List<Diagnostic> result = new ArrayList<>();
        Set<String> canonical = knownTokens == null ? Set.of() : knownTokens;
        Set<String> raw = rawValueTokens == null ? Set.of() : rawValueTokens;
        Set<String> aliases = new HashSet<>();
        config.aliases().forEach((alias, target) -> {
            if (!aliases.add(alias)) result.add(new Diagnostic(Severity.ERROR, "duplicate alias " + alias, SourceLocation.unknown(config.source())));
            if (canonical.contains(alias)) result.add(new Diagnostic(Severity.ERROR, "alias collides with canonical token " + alias, SourceLocation.unknown(config.source())));
            if (!canonical.isEmpty() && !canonical.contains(target)) result.add(new Diagnostic(Severity.ERROR, "alias target is unknown: " + target, SourceLocation.unknown(config.source())));
        });
        for (FormatProfile p : config.profiles()) {
            if (p.nativeLoreMode() != FormatProfile.NativeLoreMode.REPLACE && p.containsToken("native-lore")) {
                result.add(new Diagnostic(Severity.ERROR, "native-lore mode and #native-lore# cannot be combined", p.location()));
            }
            for (Object line : p.layout()) {
                if (line instanceof String text) inspectText(text, p, canonical, raw, config.aliases(), result, config.policies());
                else if (line instanceof Map<?, ?> map) inspectMap(map, p, canonical, raw, config.aliases(), result, config.policies());
            }
        }
        return List.copyOf(result);
    }
    private void inspectText(String text, FormatProfile p, Set<String> known, Set<String> raw, Map<String, String> aliases, List<Diagnostic> out, FormatPolicies policies) {
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            String token = m.group(1), mode = m.group(2);
            String canonicalToken = aliases.getOrDefault(token, token);
            if (!known.isEmpty() && !known.contains(canonicalToken) && !BLOCKS.contains(canonicalToken)) out.add(new Diagnostic(Severity.ERROR, "unknown token " + token, p.location()));
            if (mode != null && !"value".equals(mode)) out.add(new Diagnostic(Severity.ERROR, "invalid token mode :" + mode, p.location()));
            if ("value".equals(mode) && !raw.contains(canonicalToken)) out.add(new Diagnostic(Severity.ERROR, "no raw value adapter registered for " + token, p.location()));
            if (BLOCKS.contains(token) && !(text.trim().equals(m.group(0)))) {
                Severity severity = policies.invalidInlineBlock() == FormatPolicies.InvalidInlineBlock.NATIVE ? Severity.WARNING : Severity.ERROR;
                out.add(new Diagnostic(severity, "block token used inline: " + token, p.location()));
            }
        }
    }
    private void inspectMap(Map<?, ?> map, FormatProfile p, Set<String> known, Set<String> raw, Map<String, String> aliases, List<Diagnostic> out, FormatPolicies policies) {
        for (Object value : map.values()) {
            if (value instanceof String s) inspectText(s, p, known, raw, aliases, out, policies);
            else if (value instanceof Map<?, ?> child) inspectMap(child, p, known, raw, aliases, out, policies);
            else if (value instanceof Collection<?> c) c.forEach(v -> { if (v instanceof String s) inspectText(s, p, known, raw, aliases, out, policies); });
        }
    }
}
