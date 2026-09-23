package vn.saly.custommorelore.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.saly.custommorelore.service.ReformatService;

import java.util.*;

/** Safe command facade. All mutations are delegated to services and run on the main thread. */
public final class CustomMoreLoreCommand implements CommandExecutor, TabCompleter {
    public static final String ROOT = "custommorelore.admin";
    public static final String COMMAND = "custommorelore.command";
    private final ReloadHandler reload; private final ValidationHandler validate;
    private final DebugHandler debug; private final PreviewHandler preview; private final ReformatService reformat;
    private final EditorHandler editor;
    public CustomMoreLoreCommand(ReloadHandler reload, ValidationHandler validate, DebugHandler debug,
                                  PreviewHandler preview, ReformatService reformat) {
        this(reload, validate, debug, preview, reformat, null);
    }
    public CustomMoreLoreCommand(ReloadHandler reload, ValidationHandler validate, DebugHandler debug,
                                  PreviewHandler preview, ReformatService reformat, EditorHandler editor) {
        this.reload = reload; this.validate = validate; this.debug = debug; this.preview = preview; this.reformat = reformat;
        this.editor = editor;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> { if (!guard(sender, "custommorelore.reload")) return true; reply(sender, reload == null ? "Reload unavailable" : reload.reload()); }
            case "validate" -> { if (!guard(sender, "custommorelore.validate")) return true; reply(sender, validate == null ? "Validation unavailable" : validate.validate()); }
            case "debug" -> debug(sender, args);
            case "preview" -> preview(sender, args);
            case "reformat" -> reformat(sender, args);
            case "editor" -> editor(sender, args);
            default -> help(sender);
        }
        return true;
    }
    private void editor(CommandSender sender, String[] args) {
        if (!guard(sender, "custommorelore.editor")) return;
        if (!(sender instanceof Player player)) { reply(sender, "This command requires a player."); return; }
        if (args.length < 2 || args[1].isBlank()) { reply(sender, "/cml editor default|type:<TYPE>|item:<TYPE>:<ID>"); return; }
        reply(sender, editor == null ? "Editor unavailable" : editor.open(player, args[1]));
    }
    private void debug(CommandSender sender, String[] args) {
        if (!guard(sender, "custommorelore.debug")) return;
        if (!(sender instanceof Player player)) { reply(sender, "This command requires a player for item diagnostics."); return; }
        if (args.length < 2 || !"item".equalsIgnoreCase(args[1])) { reply(sender, "/cml debug item"); return; }
        ItemStack stack = player.getInventory().getItemInMainHand(); reply(sender, debug == null ? "Debug unavailable" : debug.debug(player, stack == null ? null : stack.clone()));
    }
    private void preview(CommandSender sender, String[] args) {
        if (!guard(sender, "custommorelore.preview")) return;
        if (!(sender instanceof Player player)) { reply(sender, "This command requires a player."); return; }
        if (args.length < 2 || args[1].isBlank()) { reply(sender, "/cml preview <profile>"); return; }
        ItemStack stack = player.getInventory().getItemInMainHand();
        reply(sender, preview == null ? "Preview unavailable" : preview.preview(player, args[1], stack == null ? null : stack.clone()));
    }
    private void reformat(CommandSender sender, String[] args) {
        if (!guard(sender, "custommorelore.reformat")) return;
        if (args.length < 2) { reply(sender, "/cml reformat hand|inventory|player <name>"); return; }
        String scope = args[1].toLowerCase(Locale.ROOT); Player target = sender instanceof Player p ? p : null;
        if ("player".equals(scope)) { if (args.length < 3) { reply(sender, "Player name required."); return; } target = Bukkit.getPlayerExact(args[2]); }
        if (target == null) { reply(sender, "Target player is not online."); return; }
        switch (scope) {
            case "hand" -> reply(sender, result(reformat.reformat(target, target.getInventory().getItemInMainHand(), ReformatService.Context.persistent())));
            case "inventory", "player" -> reply(sender, batch(reformat.reformatInventory(target, 256)));
            default -> reply(sender, "/cml reformat hand|inventory|player <name>");
        }
    }
    private String result(ReformatService.Result r) { return r.status() + (r.reason() == null ? "" : " (" + r.reason() + ")"); }
    private String batch(ReformatService.BatchResult r) { return "Scanned " + r.scanned() + ", formatted " + r.formatted() + ", failed " + r.failed(); }
    private boolean guard(CommandSender sender, String permission) { if (sender.hasPermission(ROOT) || sender.hasPermission(COMMAND) || sender.hasPermission(permission)) return true; reply(sender, ChatColor.RED + "No permission."); return false; }
    private void help(CommandSender sender) { reply(sender, "/cml reload | validate | debug item | preview <profile> | editor <profile> | reformat hand|inventory|player <name>"); }
    private void reply(CommandSender sender, String message) { sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message == null ? "" : message)); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return partial(args[0], List.of("reload", "validate", "debug", "preview", "editor", "reformat"));
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) return partial(args[1], List.of("item"));
        if (args.length == 2 && "reformat".equalsIgnoreCase(args[0])) return partial(args[1], List.of("hand", "inventory", "player"));
        if (args.length == 3 && "player".equalsIgnoreCase(args[1]) && "reformat".equalsIgnoreCase(args[0])) return null;
        return List.of();
    }
    private List<String> partial(String input, List<String> values) { List<String> out = new ArrayList<>(); for (String value : values) if (value.startsWith(input.toLowerCase(Locale.ROOT))) out.add(value); return out; }

    @FunctionalInterface public interface ReloadHandler { String reload(); }
    @FunctionalInterface public interface ValidationHandler { String validate(); }
    @FunctionalInterface public interface DebugHandler { String debug(Player player, ItemStack stack); }
    @FunctionalInterface public interface PreviewHandler { String preview(Player player, String profile, ItemStack stack); }
    @FunctionalInterface public interface EditorHandler { String open(Player player, String profile); }
}
