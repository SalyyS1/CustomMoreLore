package vn.saly.custommorelore.editor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/** Stable Bukkit inventory editor shell. It edits only an admin draft. */
public final class LayoutEditorMenu implements Listener {
    private final EditorDraftService drafts;
    private final EditorValidationService validator;
    private final EditorDraftService.DraftSaver saver;
    private final JavaPlugin plugin;
    private final Map<UUID, UUID> pendingRows = new HashMap<>();
    public LayoutEditorMenu(EditorDraftService drafts, EditorValidationService validator) {
        this(null, drafts, validator, null);
    }
    public LayoutEditorMenu(EditorDraftService drafts, EditorValidationService validator, EditorDraftService.DraftSaver saver) {
        this(null, drafts, validator, saver);
    }
    public LayoutEditorMenu(JavaPlugin plugin, EditorDraftService drafts, EditorValidationService validator, EditorDraftService.DraftSaver saver) {
        this.plugin = plugin;
        this.drafts = Objects.requireNonNull(drafts); this.validator = Objects.requireNonNull(validator); this.saver = saver;
    }
    public void open(Player player, String profileId, Map<String, ?> initial) {
        EditorSession session = drafts.begin(player.getUniqueId(), profileId, initial);
        player.openInventory(build(session));
    }
    private Inventory build(EditorSession session) {
        Holder holder = new Holder(session.id());
        Inventory inv = Bukkit.createInventory(holder, 27, "CustomMoreLore: " + session.profileId());
        holder.inventory = inv;
        paint(inv, session);
        return inv;
    }
    private void paint(Inventory inv, EditorSession session) {
        inv.clear();
        put(inv, 11, Material.PAPER, "Add row");
        put(inv, 13, validator.valid(session) ? Material.LIME_DYE : Material.RED_DYE,
            validator.valid(session) ? "Save (validation passed)" : "Save disabled: validation errors");
        put(inv, 15, Material.BOOK, "Preview (unsaved)");
    }
    private void put(Inventory inv, int slot, Material material, String name) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); } inv.setItem(slot, item);
    }
    public Optional<EditorSession> session(Inventory inventory) {
        if (!(inventory.getHolder() instanceof Holder holder)) return Optional.empty();
        return drafts.find(holder.sessionId);
    }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Optional<EditorSession> found = session(event.getView().getTopInventory());
        if (found.isEmpty()) return;
        event.setCancelled(true);
        EditorSession session = found.get();
        if (!session.owner().equals(player.getUniqueId()) || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (event.getRawSlot() == 11) {
            pendingRows.put(player.getUniqueId(), session.id());
            player.sendMessage("Type the new lore row in chat (tokens: #attack-damage#, #speed#, #abilities#). Type cancel to abort.");
        } else if (event.getRawSlot() == 13 && saver != null) {
            EditorDraftService.SaveResult result = drafts.save(session, validator, saver);
            player.sendMessage(result.success() ? "Layout saved." : "Save failed: " + result.error());
            if (result.success()) player.closeInventory();
        } else if (event.getRawSlot() == 15) {
            Object current = session.get("layout");
            if (current instanceof Collection<?> rows) rows.forEach(row -> player.sendMessage(String.valueOf(row)));
        }
        // Save/preview are delegated to the command service; the menu never writes config or ItemStacks.
    }
    @EventHandler public void onChat(AsyncPlayerChatEvent event) {
        UUID sessionId = pendingRows.remove(event.getPlayer().getUniqueId());
        if (sessionId == null) return;
        event.setCancelled(true);
        String text = event.getMessage().trim();
        if (text.equalsIgnoreCase("cancel") || text.isBlank()) { send(event.getPlayer(), "Row edit cancelled."); return; }
        Optional<EditorSession> found = drafts.find(sessionId);
        if (found.isEmpty()) { send(event.getPlayer(), "Editor session expired."); return; }
        EditorSession session = found.get();
        Object current = session.get("layout");
        List<Object> rows = new ArrayList<>(current instanceof Collection<?> c ? c : List.of());
        rows.add(text);
        session.put("layout", rows);
        send(event.getPlayer(), "Row added. Click Save when finished.");
    }
    @EventHandler public void onClose(InventoryCloseEvent event) { pendingRows.remove(event.getPlayer().getUniqueId()); cleanup(event.getInventory(), event.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { pendingRows.remove(event.getPlayer().getUniqueId()); drafts.discard(event.getPlayer().getUniqueId()); }
    private void cleanup(Inventory inventory, UUID owner) { if (inventory.getHolder() instanceof Holder h) drafts.discard(owner, h.sessionId); }
    private void send(Player player, String message) {
        Runnable task = () -> { if (player.isOnline()) player.sendMessage(message); };
        if (plugin != null) Bukkit.getScheduler().runTask(plugin, task); else task.run();
    }
    public static final class Holder implements InventoryHolder {
        private final UUID sessionId; private Inventory inventory; public Holder(UUID sessionId) { this.sessionId = sessionId; }
        public UUID sessionId() { return sessionId; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
