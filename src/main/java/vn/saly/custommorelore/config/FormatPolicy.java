package vn.saly.custommorelore.config;

/** Behaviour to apply when a fragment provider cannot produce normal content. */
public enum FormatPolicy {
    HIDE,
    EMPTY,
    NATIVE,
    ERROR;

    public static FormatPolicy parse(Object value, FormatPolicy fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value).trim().toUpperCase().replace('-', '_');
        try { return valueOf(text); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
