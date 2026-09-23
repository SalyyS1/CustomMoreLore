package vn.saly.custommorelore.api;

import java.util.List;
import java.util.Objects;

/** Typed provider result. A zero numeric value is explicitly present. */
public final class ProviderResult {
    public enum State { PRESENT, EMPTY, MISSING, ZERO, UNKNOWN, UNSUPPORTED, ERROR }
    private final State state;
    private final Object value;
    private final String text;
    private final String reason;
    private final String label;
    private final String unit;
    private final String icon;
    private final List<String> lines;

    private ProviderResult(State state, Object value, String text, String reason,
                           String label, String unit, String icon, List<String> lines) {
        this.state = Objects.requireNonNull(state);
        this.value = value; this.text = text; this.reason = reason;
        this.label = label; this.unit = unit; this.icon = icon;
        this.lines = lines == null ? List.of() : List.copyOf(lines);
    }
    public static ProviderResult present(String text) { return new ProviderResult(State.PRESENT, null, text, null, null, null, null, null); }
    public static ProviderResult present(Object value, String text) { return new ProviderResult(State.PRESENT, value, text, null, null, null, null, null); }
    public static ProviderResult zero(Object value) { return new ProviderResult(State.ZERO, value, String.valueOf(value), null, null, null, null, null); }
    public static ProviderResult empty() { return new ProviderResult(State.EMPTY, null, "", null, null, null, null, null); }
    public static ProviderResult missing() { return new ProviderResult(State.MISSING, null, null, null, null, null, null, null); }
    public static ProviderResult unknown(String reason) { return new ProviderResult(State.UNKNOWN, null, null, reason, null, null, null, null); }
    public static ProviderResult unsupported(String reason) { return new ProviderResult(State.UNSUPPORTED, null, null, reason, null, null, null, null); }
    public static ProviderResult error(String reason) { return new ProviderResult(State.ERROR, null, null, reason, null, null, null, null); }
    public static ProviderResult block(List<String> lines) { return new ProviderResult(State.PRESENT, null, null, null, null, null, null, lines); }
    public ProviderResult withMetadata(String label, String unit, String icon) {
        return new ProviderResult(state, value, text, reason, label, unit, icon, lines);
    }
    public State state() { return state; }
    public Object value() { return value; }
    public String text() { return text; }
    public String reason() { return reason; }
    public String label() { return label; }
    public String unit() { return unit; }
    public String icon() { return icon; }
    public List<String> lines() { return lines; }
    public boolean visible() { return state == State.PRESENT || state == State.ZERO; }
}
