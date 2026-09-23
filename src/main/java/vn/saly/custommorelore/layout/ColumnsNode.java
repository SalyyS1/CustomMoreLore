package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.SourceLocation;
import java.util.List;

/** A single logical row made from scalar columns. */
public record ColumnsNode(List<Column> columns, String gap, SourceLocation location) implements LayoutNode {
    public ColumnsNode {
        columns = List.copyOf(columns == null ? List.of() : columns);
        gap = gap == null ? " " : gap;
    }
    public record Column(String token, TokenMode mode, String absent) {
        public Column { absent = absent == null ? "remove" : absent; }
    }
}
