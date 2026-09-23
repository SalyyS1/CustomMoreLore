package vn.saly.custommorelore.editor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Schema-lite validation for drafts; full format validation remains the config compiler's job. */
public final class EditorValidationService {
    private static final Pattern TOKEN = Pattern.compile("#([A-Za-z0-9_.-]+)(?::(value|fragment|block))?#");
    private static final Set<String> BLOCKS = Set.of("abilities", "elements", "effects", "lore", "native-lore");
    public List<Diagnostic> validate(EditorSession session) {
        if (session == null) return List.of(new Diagnostic("session", "Session is missing"));
        List<Diagnostic> out = new ArrayList<>();
        if (session.profileId().isBlank()) out.add(new Diagnostic("profile", "Profile id cannot be empty"));
        Object layout = session.get("layout");
        if (layout instanceof Collection<?> rows) {
            int i = 0;
            for (Object row : rows) {
                if (row == null || String.valueOf(row).isBlank()) out.add(new Diagnostic("layout[" + i + "]", "Row cannot be empty"));
                validateTokens(String.valueOf(row), "layout[" + i + "]", out);
                i++;
            }
        } else if (layout != null) validateTokens(String.valueOf(layout), "layout", out);
        return List.copyOf(out);
    }
    private void validateTokens(String text, String path, List<Diagnostic> out) {
        int open = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '#') open++;
        if ((open & 1) == 1) out.add(new Diagnostic(path, "Unclosed token marker"));
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            if (BLOCKS.contains(matcher.group(1).toLowerCase(Locale.ROOT)) && !text.trim().equals(matcher.group()))
                out.add(new Diagnostic(path, "Block token must occupy its own row: " + matcher.group()));
        }
    }
    public boolean valid(EditorSession session) { return validate(session).isEmpty(); }
    public record Diagnostic(String path, String message) { }
}
