package vn.saly.custommorelore.model;

import java.util.List;

/** Immutable copy of the two lists exposed by MMOItems' deprecated lore event. */
public record LoreSnapshot(List<String> unparsed, List<String> parsed, boolean sameBackingList) {
    public LoreSnapshot {
        unparsed = unparsed == null ? List.of() : List.copyOf(unparsed);
        parsed = parsed == null ? List.of() : List.copyOf(parsed);
    }
}
