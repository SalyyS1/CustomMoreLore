package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.SourceLocation;
import java.util.List;

public record RowNode(List<List<LayoutNode>> parts, String join, AbsentPolicy absent,
                      EmptyPolicy empty, SourceLocation location) implements LayoutNode {
    public enum AbsentPolicy { REMOVE_PART, KEEP_EMPTY }
    public enum EmptyPolicy { REMOVE_ROW, KEEP_ROW }
    public RowNode {
        parts = parts == null ? List.of() : parts.stream().map(List::copyOf).toList();
        join = join == null ? "" : join;
        absent = absent == null ? AbsentPolicy.REMOVE_PART : absent;
        empty = empty == null ? EmptyPolicy.REMOVE_ROW : empty;
    }
}
