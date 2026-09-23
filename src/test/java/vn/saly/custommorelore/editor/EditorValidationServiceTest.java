package vn.saly.custommorelore.editor;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EditorValidationServiceTest {
    @Test void blockTokenMustBeAWholeRow() {
        EditorSession session = new EditorSession(UUID.randomUUID(), "sword", Map.of("layout", List.of("<gray>#abilities#")));
        assertFalse(new EditorValidationService().valid(session));
    }
    @Test void ordinaryRowsPass() {
        EditorSession session = new EditorSession(UUID.randomUUID(), "sword", Map.of("layout", List.of("#damage# #speed#", "#abilities#")));
        assertTrue(new EditorValidationService().valid(session));
    }
    @Test void saveDelegatesOnlyValidatedDraft() {
        EditorDraftService drafts = new EditorDraftService();
        EditorValidationService validation = new EditorValidationService();
        EditorSession session = drafts.begin(UUID.randomUUID(), "sword", Map.of("layout", List.of("#damage#")));
        EditorDraftService.SaveResult result = drafts.save(session, validation, (s, draft) -> {
            assertEquals("sword", s.profileId()); assertTrue(draft.containsKey("layout"));
            return new EditorDraftService.SaveResult(true, null);
        });
        assertTrue(result.success()); assertTrue(drafts.find(session.id()).isEmpty());
    }
}
