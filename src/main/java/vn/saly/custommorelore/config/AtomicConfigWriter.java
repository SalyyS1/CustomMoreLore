package vn.saly.custommorelore.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Objects;
import java.util.function.Predicate;

/** Writes a complete candidate config to a sibling temp file then atomically swaps it. */
public final class AtomicConfigWriter {
    public WriteResult write(Path target, String content, Predicate<String> validator) {
        Objects.requireNonNull(target, "target"); Objects.requireNonNull(content, "content");
        if (validator != null && !validator.test(content)) return new WriteResult(false, "candidate rejected by validator");
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null) return new WriteResult(false, "target has no parent");
        Path temp = null;
        try {
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, target.getFileName().toString(), ".cml.tmp");
            Files.writeString(temp, content, StandardOpenOption.TRUNCATE_EXISTING);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            return new WriteResult(true, null);
        } catch (IOException e) {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            return new WriteResult(false, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
    public WriteResult write(Path target, String content) { return write(target, content, null); }
    public record WriteResult(boolean success, String error) { }
}
