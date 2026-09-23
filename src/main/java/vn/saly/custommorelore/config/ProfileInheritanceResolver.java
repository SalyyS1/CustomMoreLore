package vn.saly.custommorelore.config;

import java.util.*;

/** Deterministic inheritance merger for profile maps. Child keys override parent keys. */
public final class ProfileInheritanceResolver {
    public Map<String, Map<String, Object>> resolveAll(Map<String, ? extends Map<String, ?>> profiles) {
        if (profiles == null) return Map.of();
        Map<String, Map<String, Object>> source = new LinkedHashMap<>();
        profiles.forEach((id, value) -> source.put(id, copy(value)));
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (String id : source.keySet()) out.put(id, resolve(id, source, new ArrayDeque<>()));
        return Collections.unmodifiableMap(out);
    }
    public Map<String, Object> resolve(String id, Map<String, ? extends Map<String, ?>> profiles) {
        Map<String, Map<String, Object>> source = new LinkedHashMap<>();
        profiles.forEach((key, value) -> source.put(key, copy(value)));
        return resolve(id, source, new ArrayDeque<>());
    }
    private Map<String, Object> resolve(String id, Map<String, Map<String, Object>> source, Deque<String> path) {
        if (!source.containsKey(id)) throw new IllegalArgumentException("Unknown profile: " + id);
        if (path.contains(id)) { path.addLast(id); throw new IllegalArgumentException("Profile inheritance cycle: " + String.join(" -> ", path)); }
        path.addLast(id);
        Map<String, Object> own = source.get(id), result = new LinkedHashMap<>();
        Object parent = own.containsKey("extends") ? own.get("extends") : own.get("parent");
        if (parent != null && !String.valueOf(parent).isBlank()) result.putAll(resolve(String.valueOf(parent), source, path));
        path.removeLast();
        Object inheritedRows = result.get("rows");
        result.putAll(own);
        result.remove("parent");
        result.remove("extends");
        if (own.get("rows") instanceof Collection<?> childRows && inheritedRows instanceof Collection<?> parentRows)
            result.put("rows", mergeRows(parentRows, childRows));
        return Collections.unmodifiableMap(result);
    }
    private static List<Object> mergeRows(Collection<?> parent, Collection<?> child) {
        List<Object> merged = new ArrayList<>(); Map<String, Integer> byId = new LinkedHashMap<>();
        for (Object row : parent) { merged.add(row); String id = rowId(row); if (id != null) byId.put(id, merged.size() - 1); }
        for (Object row : child) { String id = rowId(row); if (id != null && byId.containsKey(id)) merged.set(byId.get(id), row); else merged.add(row); }
        return List.copyOf(merged);
    }
    private static String rowId(Object row) { return row instanceof Map<?, ?> map && map.get("id") != null ? String.valueOf(map.get("id")) : null; }
    private static Map<String, Object> copy(Map<String, ?> input) { Map<String, Object> out = new LinkedHashMap<>(); if (input != null) input.forEach((k, v) -> out.put(String.valueOf(k), deepCopy(v))); return out; }
    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) { Map<String, Object> out = new LinkedHashMap<>(); map.forEach((k, v) -> out.put(String.valueOf(k), deepCopy(v))); return out; }
        if (value instanceof Collection<?> collection) { List<Object> out = new ArrayList<>(); collection.forEach(v -> out.add(deepCopy(v))); return List.copyOf(out); }
        return value;
    }
}
