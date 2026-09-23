package vn.saly.custommorelore.layout;

import java.util.List;

/** Small integration boundary. Phase 3 can implement this without depending on compiler internals. */
public interface LoreBuildContext {
    String itemId();
    String itemType();
    default String tier() { return null; }
    List<String> nativeLore();
    /** True when MMOItems owns a tooltip texture/header that custom lore may overwrite. */
    default boolean nativeTooltipPresent() { return false; }
    FragmentProvider provider();
    default ViewerContext viewer() { return null; }
    default boolean preview() { return viewer() != null; }

    interface ViewerContext {
        String name();
        default boolean hasPermission(String permission) { return false; }
        default String placeholder(String token) { return null; }
    }
    static LoreBuildContext of(String itemId, String itemType, List<String> nativeLore, FragmentProvider provider) {
        return new LoreBuildContext() {
            public String itemId() { return itemId; }
            public String itemType() { return itemType; }
            public List<String> nativeLore() { return nativeLore == null ? List.of() : List.copyOf(nativeLore); }
            public FragmentProvider provider() { return provider; }
        };
    }
}
