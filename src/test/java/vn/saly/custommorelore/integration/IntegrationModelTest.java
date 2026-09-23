package vn.saly.custommorelore.integration;

import org.junit.jupiter.api.Test;
import vn.saly.custommorelore.model.FragmentCapturePolicy;
import vn.saly.custommorelore.model.LoreSnapshot;
import vn.saly.custommorelore.model.MmoItemIdentity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationModelTest {
    @Test
    void snapshotsAreDefensiveAndIdentityIsStable() {
        var raw = new java.util.ArrayList<>(List.of("#attack-damage#"));
        var parsed = new java.util.ArrayList<>(List.of("§7Damage: 10"));
        LoreSnapshot snapshot = new LoreSnapshot(raw, parsed, false);
        raw.add("mutated"); parsed.clear();
        assertEquals(List.of("#attack-damage#"), snapshot.unparsed());
        assertEquals(List.of("§7Damage: 10"), snapshot.parsed());
        assertEquals(new MmoItemIdentity("SWORD", "IRON_SWORD", "RARE", 2),
                new MmoItemIdentity("SWORD", "IRON_SWORD", "RARE", 2));
    }

    @Test
    void fragmentCaptureRequiresExplicitAllowlist() {
        FragmentCapturePolicy disabled = new FragmentCapturePolicy(false, Set.of("ATTACK_DAMAGE"));
        assertFalse(disabled.allows("ATTACK_DAMAGE"));
        FragmentCapturePolicy enabled = new FragmentCapturePolicy(true, Set.of("ATTACK_DAMAGE"));
        assertTrue(enabled.allows("ATTACK_DAMAGE"));
        assertFalse(enabled.allows("ATTACK_SPEED"));
    }
}
