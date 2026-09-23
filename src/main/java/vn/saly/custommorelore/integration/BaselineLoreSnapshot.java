package vn.saly.custommorelore.integration;

import net.Indyuce.mmoitems.api.event.GenerateLoreEvent;
import vn.saly.custommorelore.model.LoreSnapshot;

/** Defensive event snapshot. MMOItems currently passes aliases in a different order than the names suggest. */
public final class BaselineLoreSnapshot {
    private BaselineLoreSnapshot() { }

    public static LoreSnapshot from(GenerateLoreEvent event) {
        if (event == null) return new LoreSnapshot(java.util.List.of(), java.util.List.of(), false);
        // MMOItems 6.10.1 constructs the event as (lore builder, parsedLore, unparsedLore),
        // so the API accessors are historically reversed: getLore() is the final native
        // output while getParsedLore() is the mutable list consumed by setFinalLore().
        java.util.List<String> unparsed = event.getParsedLore() == null ? java.util.List.of() : java.util.List.copyOf(event.getParsedLore());
        java.util.List<String> parsed = event.getLore() == null ? java.util.List.of() : java.util.List.copyOf(event.getLore());
        return new LoreSnapshot(unparsed, parsed, event.getLore() == event.getParsedLore());
    }
}
