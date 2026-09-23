package vn.saly.custommorelore.layout;

import org.junit.jupiter.api.Test;
import vn.saly.custommorelore.config.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileSelectorTest {
    private static FormatProfile p(String id, FormatProfile.Bucket bucket, String key, int priority) {
        return new FormatProfile(id, bucket, key, priority, FormatProfile.TextMode.LITERAL, FormatProfile.NativeLoreMode.REPLACE, List.of(), Map.of(), null);
    }
    @Test void itemBeatsTypeAndDefault() {
        FormatConfig c = new FormatConfig(1, FormatPolicies.defaults(), Map.of(), List.of(
            p("default", FormatProfile.Bucket.DEFAULT, null, 0), p("type", FormatProfile.Bucket.TYPE, "SWORD", 1), p("item", FormatProfile.Bucket.ITEM, "rpg:sword", 2)), "test");
        LoreBuildContext context = LoreBuildContext.of("rpg:sword", "SWORD", List.of(), FragmentProvider.EMPTY);
        assertEquals("item", new ProfileSelector().selectProfile(c, context).id());
    }
    @Test void priorityThenIdTieBreak() {
        List<FormatProfile> list = List.of(p("z", FormatProfile.Bucket.DEFAULT, null, 2), p("a", FormatProfile.Bucket.DEFAULT, null, 2));
        assertEquals("a", new ProfileSelector().choose(list, "default").profile().id());
    }
}
