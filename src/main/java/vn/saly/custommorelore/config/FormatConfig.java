package vn.saly.custommorelore.config;

import java.util.*;

/** Immutable, validated source configuration before AST compilation. */
public final class FormatConfig {
    private final int version;
    private final FormatPolicies policies;
    private final Map<String, String> aliases;
    private final List<FormatProfile> profiles;
    private final Map<String, List<FormatProfile>> itemProfiles;
    private final Map<String, List<FormatProfile>> typeProfiles;
    private final List<FormatProfile> defaultProfiles;
    private final String source;

    public FormatConfig(int version, FormatPolicies policies, Map<String, String> aliases,
                        List<FormatProfile> profiles, String source) {
        this.version = version;
        this.policies = Objects.requireNonNull(policies);
        this.aliases = Map.copyOf(aliases == null ? Map.of() : aliases);
        this.profiles = List.copyOf(profiles == null ? List.of() : profiles);
        this.source = source == null ? "<memory>" : source;
        Map<String, List<FormatProfile>> items = new HashMap<>(), types = new HashMap<>();
        List<FormatProfile> defaults = new ArrayList<>();
        for (FormatProfile p : this.profiles) {
            switch (p.bucket()) {
                case ITEM -> items.computeIfAbsent(p.matchKey(), ignored -> new ArrayList<>()).add(p);
                case TYPE -> types.computeIfAbsent(p.matchKey(), ignored -> new ArrayList<>()).add(p);
                case DEFAULT -> defaults.add(p);
            }
        }
        this.itemProfiles = freeze(items);
        this.typeProfiles = freeze(types);
        this.defaultProfiles = List.copyOf(defaults);
    }
    private static Map<String, List<FormatProfile>> freeze(Map<String, List<FormatProfile>> input) {
        Map<String, List<FormatProfile>> out = new HashMap<>();
        input.forEach((key, value) -> out.put(key, List.copyOf(value)));
        return Map.copyOf(out);
    }
    public int version() { return version; }
    public FormatPolicies policies() { return policies; }
    public Map<String, String> aliases() { return aliases; }
    public List<FormatProfile> profiles() { return profiles; }
    public Map<String, List<FormatProfile>> itemProfiles() { return itemProfiles; }
    public Map<String, List<FormatProfile>> typeProfiles() { return typeProfiles; }
    public List<FormatProfile> defaultProfiles() { return defaultProfiles; }
    public String source() { return source; }
}
