package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
/** Explicit separator retained in the AST when a layout author asks for one. */
public record SeparatorNode(String text, SourceLocation location) implements LayoutNode {}
