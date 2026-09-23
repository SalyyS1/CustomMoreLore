package vn.saly.custommorelore.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Parses formats.yml into an immutable configuration. Compilation is deliberately separate. */
public final class FormatConfigLoader {
    public FormatConfig load(Path path) {
        try (InputStream in = Files.newInputStream(path)) { return load(in, path.toString()); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Unable to read " + path, e); }
    }
    public FormatConfig load(InputStream input) { return load(input, "<stream>"); }
    public FormatConfig load(String yaml) { return load(new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "<string>"); }
    public FormatConfig load(InputStream input, String source) {
        Object value = new Yaml().load(input);
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("formats.yml root must be a map");
        return fromMap(map, source);
    }
    @SuppressWarnings("unchecked")
    public FormatConfig fromMap(Map<?, ?> root, String source) {
        int version = integer(root.get("version"), 1);
        if (version != 1) throw new IllegalArgumentException("Unsupported formats.yml version " + version);
        FormatPolicies policies = parsePolicies(root.get("policies"));
        Map<String, String> aliases = new LinkedHashMap<>();
        Object aliasesValue = root.get("aliases");
        if (aliasesValue instanceof Map<?, ?> aliasesMap) aliasesMap.forEach((k, v) -> aliases.put(String.valueOf(k), String.valueOf(v)));
        List<FormatProfile> profiles = new ArrayList<>();
        Object profilesValue = root.get("profiles");
        if (!(profilesValue instanceof Map<?, ?> profilesMap)) {
            throw new IllegalArgumentException("profiles must be a map");
        }
        Object defaults = profilesMap.get("default");
        if (defaults instanceof Map<?, ?> m) profiles.add(profile("default", FormatProfile.Bucket.DEFAULT, null, m, source));
        else if (defaults instanceof List<?> list) {
            int i = 0; for (Object item : list) if (item instanceof Map<?, ?> m) profiles.add(profile("default-" + i++, FormatProfile.Bucket.DEFAULT, null, m, source));
        }
        readBucket(profiles, profilesMap.get("types"), FormatProfile.Bucket.TYPE, source);
        readBucket(profiles, profilesMap.get("items"), FormatProfile.Bucket.ITEM, source);
        // A flat profile declaration is accepted as a convenient shorthand.
        profilesMap.forEach((key, raw) -> {
            String name = String.valueOf(key);
            if (!Set.of("default", "types", "items").contains(name) && raw instanceof Map<?, ?> m) {
                profiles.add(profile(name, FormatProfile.Bucket.DEFAULT, null, m, source));
            }
        });
        return new FormatConfig(version, policies, aliases, profiles, source);
    }
    private static void readBucket(List<FormatProfile> profiles, Object raw, FormatProfile.Bucket bucket, String source) {
        if (!(raw instanceof Map<?, ?> map)) return;
        Map<String, Map<String, ?>> candidates = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (value instanceof Map<?, ?> m) candidates.put(String.valueOf(key), castMap(m));
            else if (value instanceof List<?> list) {
                int i = 0; for (Object item : list) if (item instanceof Map<?, ?> m) candidates.put(String.valueOf(key) + "-" + i++, castMap(m));
            }
        });
        Map<String, Map<String, Object>> resolved = new ProfileInheritanceResolver().resolveAll(candidates);
        resolved.forEach((key, value) -> profiles.add(profile(key, bucket, key.replaceFirst("-\\d+$", ""), value, source)));
    }
    private static Map<String, ?> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
    private static FormatProfile profile(String id, FormatProfile.Bucket bucket, String matchKey,
                                         Map<?, ?> raw, String source) {
        int priority = integer(raw.get("priority"), 0);
        FormatProfile.TextMode textMode = parseTextMode(raw.get("text-mode"));
        FormatProfile.NativeLoreMode nativeMode = parseNative(raw.get("native-lore"));
        List<Object> layout = new ArrayList<>();
        Object layoutRaw = raw.containsKey("layout") ? raw.get("layout") : raw.get("lines");
        if (layoutRaw instanceof Collection<?> c) layout.addAll(c);
        else if (layoutRaw != null) layout.add(layoutRaw);
        Map<String, Object> metadata = new LinkedHashMap<>();
        raw.forEach((k, v) -> metadata.put(String.valueOf(k), deepImmutable(v)));
        return new FormatProfile(id, bucket, matchKey, priority, textMode, nativeMode, layout, metadata,
            new SourceLocation(source, 0, 0));
    }
    private static Object deepImmutable(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), deepImmutable(v)));
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof Collection<?> c) return List.copyOf(c.stream().map(FormatConfigLoader::deepImmutable).toList());
        return value;
    }
    private static FormatPolicies parsePolicies(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return FormatPolicies.defaults();
        FormatPolicy missing = FormatPolicy.parse(map.get("missing"), FormatPolicy.HIDE);
        FormatPolicy empty = FormatPolicy.parse(map.get("empty"), FormatPolicy.HIDE);
        FormatPolicy unknown = FormatPolicy.parse(map.get("unknown"), FormatPolicy.NATIVE);
        FormatPolicy unsupported = FormatPolicy.parse(map.get("unsupported"), FormatPolicy.NATIVE);
        FormatPolicies.InvalidInlineBlock inline = parseEnum(map.get("invalid-inline-block"), FormatPolicies.InvalidInlineBlock.NATIVE);
        FormatPolicies.ConditionUnknown condition = parseEnum(map.get("condition-unknown"), FormatPolicies.ConditionUnknown.HIDE);
        return new FormatPolicies(missing, empty, unknown, unsupported, inline, condition);
    }
    private static FormatProfile.TextMode parseTextMode(Object value) {
        if (value == null) return FormatProfile.TextMode.MINIMESSAGE;
        try { return FormatProfile.TextMode.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid text-mode: " + value); }
    }
    private static FormatProfile.NativeLoreMode parseNative(Object value) {
        if (value == null) return FormatProfile.NativeLoreMode.REPLACE;
        try { return FormatProfile.NativeLoreMode.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid native-lore mode: " + value); }
    }
    private static <T extends Enum<T>> T parseEnum(Object value, T fallback) {
        if (value == null) return fallback;
        try { return Enum.valueOf(fallback.getDeclaringClass(), String.valueOf(value).trim().toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException e) { return fallback; }
    }
    private static int integer(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException e) { return fallback; }
    }
}
