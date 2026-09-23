package vn.saly.custommorelore.model;

import java.util.List;
import vn.saly.custommorelore.layout.LoreBuildContext;

/** Optional renderer callback. Returning an empty result leaves MMOItems lore untouched. */
@FunctionalInterface
public interface LoreTransformation {
    List<String> transform(LoreBuildContext context);
}
