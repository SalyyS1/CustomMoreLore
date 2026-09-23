package vn.saly.custommorelore.api;

import java.util.Objects;

/** Declarative logical column. Minecraft's proportional font limits pixel alignment. */
public record ColumnSpec(String token, Align align, String separator, String absent) {
    public ColumnSpec {
        Objects.requireNonNull(token, "token");
        align = align == null ? Align.LEFT : align;
        separator = separator == null ? " " : separator;
        absent = absent == null ? "remove" : absent;
    }
    public ColumnSpec(String token, Align align) { this(token, align, " ", "remove"); }
    public enum Align { LEFT, CENTER, RIGHT }
}
