package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
public record BlockTokenNode(String token, SourceLocation location) implements LayoutNode {}
