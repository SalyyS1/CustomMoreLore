package vn.saly.custommorelore.integration;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.saly.custommorelore.layout.LoreBuildContext;
import vn.saly.custommorelore.model.LoreSnapshot;

import java.util.List;
import java.util.Optional;

/** Read-only adapter for existing MMOItems stacks used by explicit reformat/preview commands. */
public final class MmoItemsAdapter {
    private final MmoItemContextFactory contexts;

    public MmoItemsAdapter(MmoItemContextFactory contexts) { this.contexts = contexts; }

    public Optional<LoreBuildContext> readContext(ItemStack stack, Player viewer) {
        if (stack == null || stack.getType().isAir() || !Bukkit.isPrimaryThread()) return Optional.empty();
        try {
            MMOItem item = new LiveMMOItem(stack);
            List<String> existing = stack.hasItemMeta() && stack.getItemMeta().hasLore()
                    ? stack.getItemMeta().getLore() : List.of();
            return Optional.of(contexts.create(item, new LoreSnapshot(existing, existing, true), viewer));
        } catch (Throwable ignored) {
            // LiveMMOItem can reject malformed or outdated NBT. Commands must keep the original lore untouched.
            return Optional.empty();
        }
    }
}
