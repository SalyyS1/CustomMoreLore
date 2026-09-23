package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.*;
import java.util.*;

/** Deterministic item > type > default profile matcher. */
public final class ProfileSelector {
    public record Selection(FormatProfile profile, String bucket, String diagnostic) {}

    public Selection select(FormatConfig config, LoreBuildContext context) {
        if (context == null) return choose(config.defaultProfiles(), "default");
        String item = context.itemId();
        if (item != null && !item.isBlank()) {
            List<FormatProfile> itemCandidates = config.itemProfiles().get(item);
            if (itemCandidates == null) itemCandidates = findIgnoreCase(config.itemProfiles(), item);
            if ((itemCandidates == null || itemCandidates.isEmpty()) && context.itemType() != null)
                itemCandidates = findIgnoreCase(config.itemProfiles(), context.itemType() + ":" + item);
            Selection selected = choose(itemCandidates, "item:" + item);
            if (selected.profile() != null) return selected;
        }
        String type = context.itemType();
        if (type != null && !type.isBlank()) {
            Selection selected = choose(config.typeProfiles().get(type), "type:" + type);
            if (selected.profile() == null) selected = choose(findIgnoreCase(config.typeProfiles(), type), "type:" + type);
            if (selected.profile() != null) return selected;
        }
        return choose(config.defaultProfiles(), "default");
    }
    private static List<FormatProfile> findIgnoreCase(Map<String, List<FormatProfile>> profiles, String key) {
        for (Map.Entry<String, List<FormatProfile>> entry : profiles.entrySet())
            if (entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
        return List.of();
    }
    public FormatProfile selectProfile(FormatConfig config, LoreBuildContext context) {
        return select(config, context).profile();
    }
    public Selection choose(List<FormatProfile> candidates, String bucket) {
        if (candidates == null || candidates.isEmpty()) return new Selection(null, bucket, "no matching profile");
        FormatProfile selected = candidates.stream().filter(Objects::nonNull).sorted(Comparator
            .comparingInt(FormatProfile::priority).reversed().thenComparing(FormatProfile::id)).findFirst().orElse(null);
        return new Selection(selected, bucket, selected == null ? "no matching profile" : "selected " + selected.id() + " priority=" + selected.priority());
    }
}
