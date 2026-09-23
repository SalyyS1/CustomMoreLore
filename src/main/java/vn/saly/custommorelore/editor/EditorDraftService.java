package vn.saly.custommorelore.editor;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Owns editor lifecycle and guarantees drafts never become global config by mutation. */
public final class EditorDraftService {
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();
    private final Duration timeout;
    public EditorDraftService() { this(Duration.ofMinutes(10)); }
    public EditorDraftService(Duration timeout) { this.timeout = timeout == null ? Duration.ofMinutes(10) : timeout; }
    public EditorSession begin(UUID owner, String profileId, Map<String, ?> initial) {
        Objects.requireNonNull(owner); Objects.requireNonNull(profileId);
        discard(owner);
        EditorSession session = new EditorSession(owner, profileId, initial);
        sessions.put(session.id(), session);
        return session;
    }
    public Optional<EditorSession> find(UUID id) {
        EditorSession session = sessions.get(id);
        if (session == null || session.expired(timeout)) { if (session != null) sessions.remove(id); return Optional.empty(); }
        session.touch(); return Optional.of(session);
    }
    public Optional<EditorSession> findForOwner(UUID owner) {
        return sessions.values().stream().filter(s -> s.owner().equals(owner)).findFirst();
    }
    public void discard(UUID owner) { sessions.values().removeIf(s -> s.owner().equals(owner)); }
    public void discard(UUID owner, UUID sessionId) { sessions.computeIfPresent(sessionId, (id, s) -> s.owner().equals(owner) ? null : s); }
    public int expire() { int before = sessions.size(); sessions.values().removeIf(s -> s.expired(timeout)); return before - sessions.size(); }
    public int size() { return sessions.size(); }
    /** Validates then delegates an immutable draft to the caller's atomic config writer. */
    public SaveResult save(EditorSession session, EditorValidationService validator, DraftSaver saver) {
        Objects.requireNonNull(session); Objects.requireNonNull(validator); Objects.requireNonNull(saver);
        if (!sessions.containsKey(session.id()) || session.expired(timeout)) return new SaveResult(false, "session-expired");
        List<EditorValidationService.Diagnostic> errors = validator.validate(session);
        if (!errors.isEmpty()) return new SaveResult(false, errors.get(0).path() + ": " + errors.get(0).message());
        try { SaveResult result = saver.save(session, session.draft()); if (result.success()) sessions.remove(session.id()); return result; }
        catch (RuntimeException ex) { return new SaveResult(false, "save-error:" + ex.getClass().getSimpleName()); }
    }
    @FunctionalInterface public interface DraftSaver { SaveResult save(EditorSession session, Map<String, Object> draft); }
    public record SaveResult(boolean success, String error) { }
}
