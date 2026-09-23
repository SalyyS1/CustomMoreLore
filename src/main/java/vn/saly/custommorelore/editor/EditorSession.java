package vn.saly.custommorelore.editor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Admin-scoped draft. It intentionally contains no ItemStack or viewer output. */
public final class EditorSession {
    private final UUID id = UUID.randomUUID();
    private final UUID owner;
    private final String profileId;
    private final Instant created = Instant.now();
    private volatile Instant touched = created;
    private final Map<String, Object> draft = new ConcurrentHashMap<>();

    public EditorSession(UUID owner, String profileId, Map<String, ?> initial) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        if (initial != null) initial.forEach((key, value) -> draft.put(key, copy(value)));
    }
    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String profileId() { return profileId; }
    public Instant created() { return created; }
    public Instant touched() { return touched; }
    public void touch() { touched = Instant.now(); }
    public boolean expired(Duration timeout) { return timeout != null && touched.plus(timeout).isBefore(Instant.now()); }
    public Map<String, Object> draft() {
        Map<String, Object> copy = new LinkedHashMap<>(); draft.forEach((key, value) -> copy.put(key, copy(value)));
        return Collections.unmodifiableMap(copy);
    }
    public Object get(String key) { return copy(draft.get(key)); }
    public void put(String key, Object value) { if (value == null) draft.remove(key); else draft.put(key, copy(value)); touch(); }
    @SuppressWarnings("unchecked") private static Object copy(Object value) {
        if (value instanceof Map<?, ?> map) { Map<String, Object> out = new LinkedHashMap<>(); map.forEach((k, v) -> out.put(String.valueOf(k), copy(v))); return out; }
        if (value instanceof Collection<?> collection) { List<Object> out = new ArrayList<>(); collection.forEach(v -> out.add(copy(v))); return out; }
        return value;
    }
}
