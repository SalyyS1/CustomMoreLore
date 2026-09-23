package vn.saly.custommorelore.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, deliberately neutral request passed to a provider. */
public final class ProviderRequest {
    private final String token;
    private final String mode;
    private final String itemId;
    private final String itemType;
    private final Object item;
    private final Object viewer;
    private final Map<String, Object> attributes;

    public ProviderRequest(String token, String mode, String itemId, String itemType,
                           Object item, Object viewer, Map<String, ?> attributes) {
        this.token = Objects.requireNonNull(token, "token");
        this.mode = mode == null ? "fragment" : mode;
        this.itemId = itemId;
        this.itemType = itemType;
        this.item = item;
        this.viewer = viewer;
        Map<String, Object> copy = new LinkedHashMap<>();
        if (attributes != null) copy.putAll(attributes);
        this.attributes = Collections.unmodifiableMap(copy);
    }

    public ProviderRequest(String token, String mode) {
        this(token, mode, null, null, null, null, Map.of());
    }

    public String token() { return token; }
    public String mode() { return mode; }
    public String itemId() { return itemId; }
    public String itemType() { return itemType; }
    public Object item() { return item; }
    public Object viewer() { return viewer; }
    public Map<String, Object> attributes() { return attributes; }
    public Object attribute(String key) { return attributes.get(key); }

    public static Builder builder(String token) { return new Builder(token); }
    public static final class Builder {
        private final String token;
        private String mode = "fragment", itemId, itemType;
        private Object item, viewer;
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private Builder(String token) { this.token = Objects.requireNonNull(token, "token"); }
        public Builder mode(String value) { mode = value; return this; }
        public Builder itemId(String value) { itemId = value; return this; }
        public Builder itemType(String value) { itemType = value; return this; }
        public Builder item(Object value) { item = value; return this; }
        public Builder viewer(Object value) { viewer = value; return this; }
        public Builder attribute(String key, Object value) { attributes.put(key, value); return this; }
        public ProviderRequest build() { return new ProviderRequest(token, mode, itemId, itemType, item, viewer, attributes); }
    }
}
