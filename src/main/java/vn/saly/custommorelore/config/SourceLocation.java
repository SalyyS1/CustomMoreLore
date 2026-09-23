package vn.saly.custommorelore.config;

/** Source position retained on compiled nodes and diagnostics. */
public record SourceLocation(String source, int line, int column) {
    public static SourceLocation unknown(String source) { return new SourceLocation(source == null ? "<memory>" : source, 0, 0); }
    @Override public String toString() { return source + ":" + line + ":" + column; }
}
