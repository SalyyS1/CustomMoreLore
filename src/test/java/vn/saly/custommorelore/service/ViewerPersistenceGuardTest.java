package vn.saly.custommorelore.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewerPersistenceGuardTest {
    @Test void placeholdersAreRejectedForPersistentLore() {
        assertTrue(ViewerPersistenceGuard.isViewerDependent("%player_name%"));
        assertThrows(IllegalArgumentException.class, () -> ViewerPersistenceGuard.requirePersistentSafe("Name: %player_name%"));
    }
    @Test void plainTextIsSafe() { assertFalse(ViewerPersistenceGuard.isViewerDependent("Attack: 10")); }
}
