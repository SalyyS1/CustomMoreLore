package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
/** Emits baseline lore for a source line that cannot safely be represented. */
public record NativeFallbackNode(SourceLocation location) implements LayoutNode {}
