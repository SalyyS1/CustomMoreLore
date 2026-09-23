package vn.saly.custommorelore.service;

import java.util.concurrent.atomic.LongAdder;

/** Lightweight counters suitable for /cml debug and optional Micrometer export. */
public final class OperationMetrics {
    private final LongAdder formatted = new LongAdder();
    private final LongAdder skipped = new LongAdder();
    private final LongAdder fallback = new LongAdder();
    private final LongAdder failed = new LongAdder();
    public void formatted() { formatted.increment(); }
    public void skipped() { skipped.increment(); }
    public void fallback() { fallback.increment(); }
    public void failed() { failed.increment(); }
    public Snapshot snapshot() { return new Snapshot(formatted.sum(), skipped.sum(), fallback.sum(), failed.sum()); }
    public void reset() { formatted.reset(); skipped.reset(); fallback.reset(); failed.reset(); }
    public record Snapshot(long formatted, long skipped, long fallback, long failed) { }
}
