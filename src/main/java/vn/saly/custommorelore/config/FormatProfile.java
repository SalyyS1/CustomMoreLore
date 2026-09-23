package vn.saly.custommorelore.config;

import java.util.*;

/** Immutable profile declaration. Layout entries are immutable scalar/map values. */
public final class FormatProfile {
    public enum Bucket { ITEM, TYPE, DEFAULT }
    public enum TextMode { MINIMESSAGE, LEGACY, LITERAL }
    public enum NativeLoreMode { REPLACE, PREPEND, APPEND }

    private final String id;
    private final Bucket bucket;
    private final String matchKey;
    private final int priority;
    private final TextMode textMode;
    private final NativeLoreMode nativeLoreMode;
    private final List<Object> layout;
    private final Map<String, Object> metadata;
    private final SourceLocation location;

    public FormatProfile(String id, Bucket bucket, String matchKey, int priority,
                         TextMode textMode, NativeLoreMode nativeLoreMode,
                         List<Object> layout, Map<String, Object> metadata,
                         SourceLocation location) {
        this.id = Objects.requireNonNull(id);
        this.bucket = Objects.requireNonNull(bucket);
        this.matchKey = matchKey;
        this.priority = priority;
        this.textMode = Objects.requireNonNull(textMode);
        this.nativeLoreMode = Objects.requireNonNull(nativeLoreMode);
        this.layout = List.copyOf(layout == null ? List.of() : layout);
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        this.location = location == null ? SourceLocation.unknown("<memory>") : location;
    }
    public String id() { return id; }
    public Bucket bucket() { return bucket; }
    public String matchKey() { return matchKey; }
    public int priority() { return priority; }
    public TextMode textMode() { return textMode; }
    public NativeLoreMode nativeLoreMode() { return nativeLoreMode; }
    public List<Object> layout() { return layout; }
    public Map<String, Object> metadata() { return metadata; }
    public SourceLocation location() { return location; }

    public boolean containsToken(String token) {
        return layout.stream().anyMatch(v -> String.valueOf(v).contains("#" + token));
    }
}
