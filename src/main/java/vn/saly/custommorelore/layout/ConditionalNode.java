package vn.saly.custommorelore.layout;
import vn.saly.custommorelore.config.SourceLocation;
import java.util.List;
public record ConditionalNode(Condition condition, List<LayoutNode> children, SourceLocation location) implements LayoutNode {
    public ConditionalNode { children = List.copyOf(children); }
}
