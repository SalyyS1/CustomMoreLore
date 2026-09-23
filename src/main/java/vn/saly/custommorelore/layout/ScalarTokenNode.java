package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
public record ScalarTokenNode(String token, TokenMode mode, SourceLocation location) implements LayoutNode {}
