package vn.saly.custommorelore.editor;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import vn.saly.custommorelore.config.AtomicConfigWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Connects editor drafts to formats.yml with atomic, syntax-validated writes. */
public final class LayoutEditorConfigService {
    private final Path file;
    private final AtomicConfigWriter writer;
    public LayoutEditorConfigService(Path file) { this(file, new AtomicConfigWriter()); }
    public LayoutEditorConfigService(Path file, AtomicConfigWriter writer) { this.file = Objects.requireNonNull(file); this.writer = Objects.requireNonNull(writer); }
    public Map<String, Object> initial(String profileId) {
        try { Map<String, Object> profile = locate(new Yaml().load(Files.readString(file)), profileId, false); return profile == null ? Map.of("layout", List.of()) : Map.of("layout", profile.getOrDefault("layout", List.of())); }
        catch (Exception ignored) { return Map.of("layout", List.of()); }
    }
    public EditorDraftService.SaveResult save(EditorSession session, Map<String, Object> draft) {
        try {
            Object root = new Yaml().load(Files.readString(file));
            Map<String, Object> profile = locate(root, session.profileId(), true);
            profile.put("layout", draft.getOrDefault("layout", List.of()));
            DumperOptions options = new DumperOptions(); options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); options.setPrettyFlow(true);
            String yaml = new Yaml(options).dump(root);
            AtomicConfigWriter.WriteResult result = writer.write(file, yaml, candidate -> { try { new Yaml().load(candidate); return true; } catch (RuntimeException ex) { return false; } });
            return result.success() ? new EditorDraftService.SaveResult(true, null) : new EditorDraftService.SaveResult(false, result.error());
        } catch (Exception ex) { return new EditorDraftService.SaveResult(false, ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
    }
    private static Map<String, Object> locate(Object root, String profileId, boolean create) {
        if (!(root instanceof Map<?, ?> raw)) throw new IllegalArgumentException("formats.yml root is not a map");
        Map<String, Object> map = cast(raw);
        Map<String, Object> profiles = map.get("profiles") instanceof Map<?, ?> p ? cast(p) : new LinkedHashMap<>();
        if (create) map.put("profiles", profiles);
        String bucket = "default", key = "default";
        if (profileId != null && profileId.startsWith("type:")) { bucket = "types"; key = profileId.substring(5); }
        else if (profileId != null && profileId.startsWith("item:")) { bucket = "items"; key = profileId.substring(5); }
        else if (profileId != null && !profileId.equals("default")) {
            for (String candidate : List.of("types", "items")) if (profiles.get(candidate) instanceof Map<?, ?> p && cast(p).containsKey(profileId)) { bucket = candidate; key = profileId; }
        }
        if ("default".equals(bucket)) {
            if (profiles.get("default") instanceof Map<?, ?> p) {
                Map<String, Object> existing = cast(p);
                if (create) profiles.put("default", existing);
                return existing;
            }
            if (!create) return null; Map<String, Object> made = new LinkedHashMap<>(); profiles.put("default", made); return made;
        }
        Map<String, Object> values = profiles.get(bucket) instanceof Map<?, ?> p ? cast(p) : new LinkedHashMap<>();
        if (create) profiles.put(bucket, values);
        if (values.get(key) instanceof Map<?, ?> p) {
            Map<String, Object> existing = cast(p);
            if (create) values.put(key, existing);
            return existing;
        }
        if (!create) return null; Map<String, Object> made = new LinkedHashMap<>(); values.put(key, made); return made;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) { return (Map<String, Object>) (Map<?, ?>) map; }
}
