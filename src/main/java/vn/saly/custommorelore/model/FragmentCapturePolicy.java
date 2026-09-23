package vn.saly.custommorelore.model;

import java.util.Set;

/** Explicit allowlist for experimental scalar fragment capture. */
public record FragmentCapturePolicy(boolean enabled, Set<String> allowlistedStatIds) {
    public FragmentCapturePolicy {
        allowlistedStatIds = allowlistedStatIds == null ? Set.of() : Set.copyOf(allowlistedStatIds);
    }
    public boolean allows(String id) { return enabled && id != null && allowlistedStatIds.contains(id); }
}
