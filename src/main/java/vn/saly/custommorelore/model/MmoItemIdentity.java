package vn.saly.custommorelore.model;

/** Stable identity captured from an MMOItems object for diagnostics and profile matching. */
public record MmoItemIdentity(String type, String id, String tier, int upgradeLevel) {
    public MmoItemIdentity {
        type = type == null ? "" : type;
        id = id == null ? "" : id;
        tier = tier == null ? "" : tier;
    }
}
