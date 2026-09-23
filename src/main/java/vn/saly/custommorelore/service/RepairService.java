package vn.saly.custommorelore.service;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Best-effort, rate-limited repair facade for async item creation paths. */
public final class RepairService {
    private final ReformatService reformat;
    private final NamespacedKey marker;
    private final long cooldownMillis;
    private final Map<UUID, Long> lastByPlayer = new ConcurrentHashMap<>();
    private volatile String version;
    private final String profileId;
    public RepairService(Plugin plugin, ReformatService reformat, String configVersion, long cooldownMillis) {
        this(plugin, reformat, configVersion, "*", cooldownMillis);
    }
    public RepairService(Plugin plugin, ReformatService reformat, String configVersion, String profileId, long cooldownMillis) {
        this.reformat = Objects.requireNonNull(reformat); this.marker = new NamespacedKey(plugin, "formatted");
        this.version = configVersion == null ? "unknown" : configVersion; this.profileId = profileId == null ? "*" : profileId;
        this.cooldownMillis = Math.max(0, cooldownMillis);
    }
    public ReformatService.Result tryRepair(Player player, ItemStack stack, String trigger) {
        if (player == null || stack == null || stack.getType().isAir()) return ReformatService.Result.skipped("empty-stack");
        long now = System.currentTimeMillis(), last = lastByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMillis) return ReformatService.Result.skipped("rate-limited");
        if (isCurrent(stack)) return ReformatService.Result.skipped("already-current");
        lastByPlayer.put(player.getUniqueId(), now);
        ReformatService.Result result = reformat.reformat(player, stack, ReformatService.Context.persistent());
        if (result.status() == ReformatService.Status.FORMATTED) mark(stack, trigger);
        return result;
    }
    public void setVersion(String configVersion) { this.version = configVersion == null ? "unknown" : configVersion; }
    /** Repairs a bounded collection in one main-thread operation, bypassing the per-player event cooldown. */
    public BatchResult tryRepairBatch(Player player, Collection<ItemStack> stacks, int maxItems, String trigger) {
        if (player == null || stacks == null) return new BatchResult(0, 0, 0);
        int scanned = 0, formatted = 0, failed = 0;
        for (ItemStack stack : stacks) {
            if (scanned++ >= Math.max(1, maxItems)) break;
            if (stack == null || stack.getType().isAir() || isCurrent(stack)) continue;
            ReformatService.Result result = reformat.reformat(player, stack, ReformatService.Context.persistent());
            if (result.status() == ReformatService.Status.FORMATTED) { mark(stack, trigger); formatted++; }
            else if (result.status() == ReformatService.Status.FAILED) failed++;
        }
        return new BatchResult(scanned, formatted, failed);
    }
    public boolean isCurrent(ItemStack stack) {
        PersistentDataContainer data = stack.getItemMeta() == null ? null : stack.getItemMeta().getPersistentDataContainer();
        String markerValue = data == null ? null : data.get(marker, PersistentDataType.STRING);
        return markerValue != null && markerValue.equals(version + "|" + profileId);
    }
    private void mark(ItemStack stack, String trigger) {
        if (stack.getItemMeta() == null) return; org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(marker, PersistentDataType.STRING, version + "|" + profileId); stack.setItemMeta(meta);
    }
    public record BatchResult(int scanned, int formatted, int failed) { }
}
