package vn.saly.custommorelore.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LayoutEditorConfigServiceTest {
    @TempDir Path temp;

    @Test void savesDefaultLayoutAtomically() throws Exception {
        Path file = temp.resolve("formats.yml");
        Files.writeString(file, "version: 1\nprofiles:\n  default:\n    layout:\n      - old\n");
        LayoutEditorConfigService service = new LayoutEditorConfigService(file);
        EditorSession session = new EditorSession(UUID.randomUUID(), "default", Map.of("layout", List.of("#attack-damage#", "#attack-speed#")));
        EditorDraftService.SaveResult result = service.save(session, session.draft());
        assertTrue(result.success(), result.error());
        String updated = Files.readString(file);
        assertFalse(updated.contains("old"));
        assertEquals(List.of("#attack-damage#", "#attack-speed#"), service.initial("default").get("layout"));
    }
}
