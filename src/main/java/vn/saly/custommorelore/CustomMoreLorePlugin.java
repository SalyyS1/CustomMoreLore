package vn.saly.custommorelore;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import vn.saly.custommorelore.command.CustomMoreLoreCommand;
import vn.saly.custommorelore.config.FormatConfig;
import vn.saly.custommorelore.config.FormatConfigLoader;
import vn.saly.custommorelore.config.FormatValidator;
import vn.saly.custommorelore.integration.HookRegistry;
import vn.saly.custommorelore.integration.MmoItemContextFactory;
import vn.saly.custommorelore.integration.MmoItemsAdapter;
import vn.saly.custommorelore.layout.CompiledLayout;
import vn.saly.custommorelore.layout.LoreBuildContext;
import vn.saly.custommorelore.layout.LoreRenderer;
import vn.saly.custommorelore.layout.ProfileRegistry;
import vn.saly.custommorelore.layout.ProfileSelector;
import vn.saly.custommorelore.layout.RenderResult;
import vn.saly.custommorelore.model.IntegrationCapabilities;
import vn.saly.custommorelore.service.ReformatService;
import vn.saly.custommorelore.service.RepairService;
import vn.saly.custommorelore.listener.RepairListeners;
import vn.saly.custommorelore.provider.BuiltinValueProviders;
import vn.saly.custommorelore.provider.ProviderRegistry;
import vn.saly.custommorelore.editor.EditorDraftService;
import vn.saly.custommorelore.editor.EditorValidationService;
import vn.saly.custommorelore.editor.LayoutEditorConfigService;
import vn.saly.custommorelore.editor.LayoutEditorMenu;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Plugin entrypoint and owner of the last-known-good format snapshot. */
public final class CustomMoreLorePlugin extends JavaPlugin {
    private final FormatConfigLoader configLoader = new FormatConfigLoader();
    private final ProfileRegistry profiles = new ProfileRegistry();
    private final ProfileSelector selector = new ProfileSelector();
    private final LoreRenderer renderer = new LoreRenderer();
    private final ProviderRegistry providerRegistry = new ProviderRegistry();
    private HookRegistry hooks;
    private IntegrationCapabilities capabilities;
    private MmoItemsAdapter itemAdapter;
    private ReformatService reformatService;
    private RepairService repairService;
    private EditorDraftService editorDrafts;
    private EditorValidationService editorValidator;
    private LayoutEditorMenu editorMenu;
    private LayoutEditorConfigService editorConfig;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("editor-messages.yml", false);
        saveResource("formats.yml", false);
        ensureFormatsFile();
        reloadConfig();
        reloadFormats();
        BuiltinValueProviders.registerDefaults(providerRegistry);
        hooks = new HookRegistry(this, providerRegistry);
        capabilities = hooks.register(getConfig().getBoolean("integration.enabled", true),
                getConfig().getBoolean("debug", false),
                getConfig().getBoolean("integration.apply-custom-layouts", true),
                getConfig().getBoolean("integration.preserve-native-on-unknown", true),
                getConfig().getBoolean("integration.capture.enabled", false),
                readAllowlist(getConfig().getStringList("integration.capture.allowlisted-stat-ids")));
        MmoItemContextFactory contextFactory = hooks.contextFactory();
        if (contextFactory == null) contextFactory = new MmoItemContextFactory(null, providerRegistry);
        itemAdapter = new MmoItemsAdapter(contextFactory);
        reformatService = new ReformatService(this::renderExistingStack);
        repairService = new RepairService(this, reformatService, "r" + currentRevision(), 250L);
        if (getConfig().getBoolean("integration.async-repair.enabled", true))
            getServer().getPluginManager().registerEvents(new RepairListeners(repairService, true), this);
        editorDrafts = new EditorDraftService();
        editorValidator = new EditorValidationService();
        editorConfig = new LayoutEditorConfigService(new File(getDataFolder(), getConfig().getString("profiles-file", "formats.yml")).toPath());
        editorMenu = new LayoutEditorMenu(this, editorDrafts, editorValidator, (session, draft) -> {
            LayoutEditorConfigService configService = editorConfig;
            if (configService == null) return new EditorDraftService.SaveResult(false, "editor-unavailable");
            EditorDraftService.SaveResult saved = configService.save(session, draft);
            if (!saved.success()) return saved;
            String reloadResult = reloadFormats();
            return reloadResult.startsWith("&c") ? new EditorDraftService.SaveResult(false, reloadResult) : saved;
        });
        getServer().getPluginManager().registerEvents(editorMenu, this);
        wireCommand();
        if (hooks.loreHook() != null) hooks.loreHook().setTransformation(this::transform);
        getLogger().info("CustomMoreLore enabled. Capabilities: " + hooks.capabilityMap());
        if (!capabilities.nativeHookAvailable()) getLogger().warning("[CustomMoreLore] MMOItems hook unavailable; native lore is preserved.");
    }

    @Override public void onDisable() { hooks = null; capabilities = null; itemAdapter = null; reformatService = null; repairService = null; editorMenu = null; editorConfig = null; editorDrafts = null; editorValidator = null; }
    public HookRegistry getHookRegistry() { return hooks; }
    public IntegrationCapabilities getCapabilities() { return capabilities; }
    public ProfileRegistry getProfileRegistry() { return profiles; }
    public ProviderRegistry getProviderRegistry() { return providerRegistry; }

    public synchronized String reloadFormats() {
        Path path = new File(getDataFolder(), getConfig().getString("profiles-file", "formats.yml")).toPath();
        try {
            FormatConfig candidate = configLoader.load(path);
            ProfileRegistry.ReloadResult result = profiles.reload(candidate);
            if (!result.activated()) return "&cformats.yml rejected: " + String.join("; ", result.diagnostics());
            if (repairService != null) repairService.setVersion("r" + result.snapshot().revision());
            if (hooks != null && hooks.loreHook() != null) hooks.loreHook().setTransformation(this::transform);
            return "&aLoaded " + candidate.profiles().size() + " profile(s), revision " + result.snapshot().revision();
        } catch (Exception ex) {
            getLogger().warning("[CustomMoreLore] formats.yml reload failed: " + ex.getMessage());
            return "&cformats.yml could not be loaded: " + ex.getClass().getSimpleName();
        }
    }

    private void wireCommand() {
        if (getCommand("cml") == null) return;
        CustomMoreLoreCommand command = new CustomMoreLoreCommand(this::reloadFormats, this::validateFormats,
                this::debugItem, this::previewItem, reformatService, this::openEditor);
        getCommand("cml").setExecutor(command);
        getCommand("cml").setTabCompleter(command);
    }
    private String openEditor(Player player, String profileId) {
        if (editorMenu == null || editorConfig == null) return "&cEditor unavailable.";
        ProfileRegistry.Snapshot snapshot = profiles.active();
        if (snapshot == null) return "&cNo active formats.yml snapshot.";
        if (snapshot.layout(profileId) == null && !"default".equalsIgnoreCase(profileId)) return "&cUnknown profile: " + profileId;
        editorMenu.open(player, profileId, editorConfig.initial(profileId));
        return "&aOpened layout editor for " + profileId + ".";
    }
    private String validateFormats() {
        ProfileRegistry.Snapshot snapshot = profiles.active();
        if (snapshot == null) return "&cNo active formats.yml snapshot.";
        List<FormatValidator.Diagnostic> diagnostics = new FormatValidator().validate(snapshot.config(), Set.of());
        if (diagnostics.isEmpty()) return "&aConfiguration is valid (revision " + snapshot.revision() + ").";
        return diagnostics.stream().map(d -> d.severity() + ": " + d.message()).limit(8).reduce((a, b) -> a + " | " + b).orElse("invalid");
    }
    private String debugItem(Player player, ItemStack stack) {
        if (itemAdapter == null) return "&cMMOItems adapter unavailable.";
        return itemAdapter.readContext(stack, player).map(context -> {
            ProfileRegistry.Snapshot snapshot = profiles.active();
            ProfileSelector.Selection selection = snapshot == null ? new ProfileSelector.Selection(null, "none", "no config") : selector.select(snapshot.config(), context);
            return "&7item=" + context.itemId() + " type=" + context.itemType() + " tier=" + context.tier()
                    + " profile=" + (selection.profile() == null ? "<none>" : selection.profile().id())
                    + " capabilities=" + (hooks == null ? Map.of() : hooks.capabilityMap())
                    + " metrics=" + (reformatService == null ? "{}" : reformatService.metrics().snapshot());
        }).orElse("&eNot an existing MMOItems stack or NBT is unreadable.");
    }
    private String previewItem(Player player, String profileId, ItemStack stack) {
        if (itemAdapter == null) return "&cMMOItems adapter unavailable.";
        return itemAdapter.readContext(stack, player).map(context -> {
            ProfileRegistry.Snapshot snapshot = profiles.active();
            if (snapshot == null) return "&cNo active profile snapshot.";
            CompiledLayout layout = snapshot.layout(profileId);
            if (layout == null) return "&cUnknown profile: " + profileId;
            RenderResult result = renderer.render(context, layout);
            return "&7Preview:\n" + result.lines().stream().map(ChatColor::stripColor).reduce((a, b) -> a + "\n" + b).orElse("<empty>");
        }).orElse("&eNot an existing MMOItems stack or NBT is unreadable.");
    }
    private ReformatService.LoreOutput renderExistingStack(ItemStack stack, ReformatService.Context operation) {
        if (itemAdapter == null) return new ReformatService.LoreOutput(List.of(), true, false);
        Player viewer = operation == null ? null : operation.viewer();
        return itemAdapter.readContext(stack, viewer).map(this::renderContext).orElse(new ReformatService.LoreOutput(List.of(), true, false));
    }
    private ReformatService.LoreOutput renderContext(LoreBuildContext context) {
        ProfileRegistry.Snapshot snapshot = profiles.active();
        if (snapshot == null) return new ReformatService.LoreOutput(context.nativeLore(), true, false);
        ProfileSelector.Selection selection = selector.select(snapshot.config(), context);
        if (selection.profile() == null) return new ReformatService.LoreOutput(context.nativeLore(), true, false);
        CompiledLayout compiled = snapshot.layout(selection.profile().id());
        String tooltipMode = String.valueOf(selection.profile().metadata().getOrDefault("tooltip", "native-only"));
        if (context.nativeTooltipPresent() && !Set.of("replace", "allow", "custom").contains(tooltipMode.toLowerCase()))
            return new ReformatService.LoreOutput(context.nativeLore(), true, false);
        RenderResult result = renderer.render(context, compiled);
        return new ReformatService.LoreOutput(result.lines(), result.nativeFallback(), context.viewer() != null);
    }
    private List<String> transform(LoreBuildContext context) { return renderContext(context).lines(); }
    private long currentRevision() { ProfileRegistry.Snapshot snapshot = profiles.active(); return snapshot == null ? 0 : snapshot.revision(); }

    private void ensureFormatsFile() {
        File file = new File(getDataFolder(), getConfig().getString("profiles-file", "formats.yml"));
        if (file.exists()) return;
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) { getLogger().warning("Could not create plugin data folder; native lore remains active."); return; }
        try { Files.writeString(file.toPath(), "version: 1\nprofiles:\n  default:\n    layout: []\n", StandardCharsets.UTF_8); }
        catch (IOException e) { getLogger().warning("Could not create formats.yml: " + e.getMessage()); }
    }
    private static Set<String> readAllowlist(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) for (String value : values) if (value != null && !value.isBlank()) result.add(value.trim());
        return result;
    }
}
