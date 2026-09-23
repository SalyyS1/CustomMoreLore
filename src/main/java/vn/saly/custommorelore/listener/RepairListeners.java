package vn.saly.custommorelore.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import vn.saly.custommorelore.service.RepairService;

import java.util.Objects;
import java.util.Arrays;

/** Opt-in best-effort repair hooks. Every path is rate-limited by RepairService. */
public final class RepairListeners implements Listener {
    private final RepairService repair; private final boolean enabled;
    public RepairListeners(RepairService repair, boolean enabled) { this.repair = Objects.requireNonNull(repair); this.enabled = enabled; }
    private void repair(Player player, ItemStack stack, String trigger) { if (enabled) repair.tryRepair(player, stack, trigger); }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (enabled) repair.tryRepairBatch(event.getPlayer(), Arrays.asList(event.getPlayer().getInventory().getContents()), 64, "join");
    }
    @EventHandler public void onOpen(InventoryOpenEvent event) {
        if (enabled && event.getPlayer() instanceof Player p)
            repair.tryRepairBatch(p, Arrays.asList(event.getInventory().getContents()), 64, "inventory-open");
    }
    @EventHandler public void onClick(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player p) repair(p, event.getCurrentItem(), "inventory-click"); }
    @EventHandler public void onPickup(EntityPickupItemEvent event) { Entity entity = event.getEntity(); if (entity instanceof Player p) repair(p, event.getItem().getItemStack(), "pickup"); }
    @EventHandler public void onCraft(PrepareItemCraftEvent event) { if (event.getView().getPlayer() instanceof Player p) repair(p, event.getInventory().getResult(), "craft-result"); }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) { repair(event.getPlayer(), event.getPlayer().getInventory().getItem(event.getNewSlot()), "equipment"); }
}
