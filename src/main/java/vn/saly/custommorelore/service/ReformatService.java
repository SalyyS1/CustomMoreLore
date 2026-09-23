package vn.saly.custommorelore.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

/** Main-thread-only existing-stack formatter. It never regenerates an MMOItems template. */
public final class ReformatService {
    private final LoreReformatter reformatter;
    private final OperationMetrics metrics;
    public ReformatService(LoreReformatter reformatter) { this(reformatter, new OperationMetrics()); }
    public ReformatService(LoreReformatter reformatter, OperationMetrics metrics) { this.reformatter = Objects.requireNonNull(reformatter); this.metrics = Objects.requireNonNull(metrics); }
    public OperationMetrics metrics() { return metrics; }

    public Result reformat(Player player, ItemStack stack, Context context) {
        requireMainThread();
        if (stack == null || stack.getType().isAir()) { metrics.skipped(); return Result.skipped("empty-stack"); }
        LoreOutput output;
        try { output = reformatter.render(stack, context == null ? Context.persistent() : context); }
        catch (RuntimeException ex) { metrics.failed(); return Result.failed("render-error:" + ex.getClass().getSimpleName()); }
        if (output == null || output.nativeFallback()) { metrics.fallback(); return Result.skipped("native-fallback"); }
        if (output.viewerDependent() || output.lines().stream().anyMatch(ViewerPersistenceGuard::isViewerDependent)) {
            metrics.skipped(); return Result.skipped("viewer-dependent-persistent-output");
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) { metrics.skipped(); return Result.skipped("no-item-meta"); }
        meta.setLore(output.lines());
        stack.setItemMeta(meta);
        metrics.formatted(); return Result.formatted(output.lines().size());
    }
    public BatchResult reformatInventory(Player player, int maxItems) {
        requireMainThread();
        if (player == null) return new BatchResult(0, 0, 0, List.of("no-player"));
        int budget = Math.max(1, maxItems), scanned = 0, formatted = 0, failed = 0;
        List<String> reasons = new ArrayList<>(); PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (scanned++ >= budget) break;
            Result result = reformat(player, stack, Context.persistent());
            if (result.status() == Status.FORMATTED) formatted++; else if (result.status() == Status.FAILED) { failed++; reasons.add(result.reason()); }
        }
        return new BatchResult(scanned, formatted, failed, reasons);
    }
    public BatchHandle reformatInventoryBounded(Plugin plugin, Player player, int perTick, Consumer<BatchResult> completion) {
        Objects.requireNonNull(plugin); Objects.requireNonNull(player); requireMainThread();
        int contentSize = player.getInventory().getSize();
        BatchHandle handle = new BatchHandle(); int budget = Math.max(1, perTick);
        final int[] cursor = {0};
        Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            if (handle.cancelled()) return;
            int start = cursor[0], end = Math.min(contentSize, start + budget), formatted = 0, failed = 0;
            for (; cursor[0] < end; cursor[0]++) { Result result = reformat(player, player.getInventory().getItem(cursor[0]), Context.persistent()); if (result.status() == Status.FORMATTED) formatted++; else if (result.status() == Status.FAILED) failed++; }
            handle.add(end - start, formatted, failed);
            if (cursor[0] < contentSize) Bukkit.getScheduler().runTask(plugin, tick[0]);
            else if (completion != null) completion.accept(handle.result());
        };
        Bukkit.getScheduler().runTask(plugin, tick[0]); return handle;
    }
    private static void requireMainThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Reformat must run on the Bukkit main thread"); }

    public interface LoreReformatter { LoreOutput render(ItemStack stack, Context context); }
    public record Context(boolean preview, Player viewer) { public static Context persistent() { return new Context(false, null); } public static Context preview(Player p) { return new Context(true, p); } }
    public record LoreOutput(List<String> lines, boolean nativeFallback, boolean viewerDependent) { public LoreOutput { lines = lines == null ? List.of() : List.copyOf(lines); } }
    public enum Status { FORMATTED, SKIPPED, FAILED }
    public record Result(Status status, String reason, int lines) { public static Result formatted(int l) { return new Result(Status.FORMATTED, null, l); } public static Result skipped(String r) { return new Result(Status.SKIPPED, r, 0); } public static Result failed(String r) { return new Result(Status.FAILED, r, 0); } }
    public record BatchResult(int scanned, int formatted, int failed, List<String> reasons) { }
    public static final class BatchHandle {
        private volatile boolean cancelled; private int scanned, formatted, failed;
        private synchronized void add(int s, int f, int e) { scanned += s; formatted += f; failed += e; }
        public void cancel() { cancelled = true; } public boolean cancelled() { return cancelled; }
        public synchronized BatchResult result() { return new BatchResult(scanned, formatted, failed, List.of()); }
    }
}
