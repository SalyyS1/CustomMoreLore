package vn.saly.custommorelore.layout;

import java.util.List;
import java.util.Objects;

/** Closed result model; null and visible-text parsing are intentionally not supported. */
public final class ProviderResult {
    public enum State { PRESENT, EMPTY, MISSING, ZERO, UNKNOWN, UNSUPPORTED }
    private final State state;
    private final String text;
    private final List<String> lines;
    private final double numericValue;
    private final String reason;
    private final String provider;

    private ProviderResult(State state, String text, List<String> lines, double numericValue, String reason, String provider) {
        this.state = Objects.requireNonNull(state);
        this.text = text;
        this.lines = lines == null ? List.of() : List.copyOf(lines);
        this.numericValue = numericValue;
        this.reason = reason;
        this.provider = provider;
    }
    public static ProviderResult present(String text) { return present(text, "provider"); }
    public static ProviderResult present(String text, String provider) { return new ProviderResult(State.PRESENT, text == null ? "" : text, List.of(), Double.NaN, null, provider); }
    public static ProviderResult lines(List<String> lines, String provider) { return new ProviderResult(State.PRESENT, null, lines, Double.NaN, null, provider); }
    public static ProviderResult empty() { return new ProviderResult(State.EMPTY, "", List.of(), Double.NaN, null, null); }
    public static ProviderResult missing() { return new ProviderResult(State.MISSING, "", List.of(), Double.NaN, null, null); }
    public static ProviderResult zero() { return zero("provider"); }
    public static ProviderResult zero(String provider) { return new ProviderResult(State.ZERO, "0", List.of(), 0, null, provider); }
    public static ProviderResult zero(double numeric, String provider) { return new ProviderResult(State.ZERO, format(numeric), List.of(), 0, null, provider); }
    public static ProviderResult value(double numeric, String text, String provider) {
        if (Double.compare(numeric, 0D) == 0) return zero(provider);
        return new ProviderResult(State.PRESENT, text == null ? format(numeric) : text, List.of(), numeric, null, provider);
    }
    public static ProviderResult unknown(String reason) { return new ProviderResult(State.UNKNOWN, "", List.of(), Double.NaN, reason, null); }
    public static ProviderResult unsupported(String reason) { return new ProviderResult(State.UNSUPPORTED, "", List.of(), Double.NaN, reason, null); }
    private static String format(double n) { return n == (long)n ? Long.toString((long)n) : Double.toString(n); }
    public State state() { return state; }
    public String text() { return text == null ? "" : text; }
    public List<String> lines() { return lines; }
    public double numericValue() { return numericValue; }
    public String reason() { return reason; }
    public String provider() { return provider; }
    public boolean visible() { return state == State.ZERO || (state == State.PRESENT && (!text().isEmpty() || !lines.isEmpty())); }
}
