package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
public record LiteralNode(String text, SourceLocation location) implements LayoutNode {}
