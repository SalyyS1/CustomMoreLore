package vn.saly.custommorelore.service;

import java.util.regex.Pattern;

/** Prevents viewer-dependent PlaceholderAPI values from being written to shared item lore. */
public final class ViewerPersistenceGuard {
    private static final Pattern PAPI = Pattern.compile("%[^%]*%", Pattern.CASE_INSENSITIVE);
    private ViewerPersistenceGuard() { }
    public static boolean containsPlaceholder(String value) { return value != null && PAPI.matcher(value).find(); }
    public static boolean isViewerDependent(String value) {
        // Unknown expansions are treated as viewer-dependent by default. This fail-closed
        // policy prevents a newly installed PlaceholderAPI expansion from leaking into a
        // stack that may later be shown to another player.
        return containsPlaceholder(value);
    }
    public static void requirePersistentSafe(String value) {
        if (isViewerDependent(value)) throw new IllegalArgumentException("Viewer-dependent PlaceholderAPI value cannot be persisted");
    }
}
