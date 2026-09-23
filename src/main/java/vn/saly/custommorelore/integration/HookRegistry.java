package vn.saly.custommorelore.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import vn.saly.custommorelore.model.FragmentCapturePolicy;
import vn.saly.custommorelore.model.IntegrationCapabilities;
import vn.saly.custommorelore.provider.ProviderRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Owns optional integrations and exposes a boot-time capability report. */
public final class HookRegistry {
    private final Plugin owner;
    private final Logger logger;
    private GenerateLoreHook loreHook;
    private MmoItemContextFactory contextFactory;
    private MmoItemsAdapter mmoItemsAdapter;
    private final ProviderRegistry providers;
    private IntegrationCapabilities capabilities = new IntegrationCapabilities(false, false, false, false, false, false);

    public HookRegistry(Plugin owner) { this(owner, new ProviderRegistry()); }
    public HookRegistry(Plugin owner, ProviderRegistry providers) {
        this.owner = owner;
        this.logger = owner.getLogger();
        this.providers = providers == null ? new ProviderRegistry() : providers;
    }

    public IntegrationCapabilities register(boolean enabled, boolean debug, boolean applyLayouts,
                                            boolean preserveNativeOnUnknown, boolean captureEnabled,
                                            Set<String> allowlistedStatIds) {
        PluginManager manager = Bukkit.getPluginManager();
        boolean mmoItems = manager.getPlugin("MMOItems") != null && manager.isPluginEnabled("MMOItems");
        boolean mythicLib = manager.getPlugin("MythicLib") != null && manager.isPluginEnabled("MythicLib");
        boolean papi = manager.getPlugin("PlaceholderAPI") != null && manager.isPluginEnabled("PlaceholderAPI");
        boolean event = mmoItems;
        boolean primary = event;
        boolean capture = captureEnabled && event;
        capabilities = new IntegrationCapabilities(mmoItems, mythicLib, papi, event, primary, capture);
        if (!enabled || !mmoItems) {
            if (debug) logger.info("[CustomMoreLore] MMOItems hook unavailable; native lore remains active.");
            return capabilities;
        }
        try {
            contextFactory = new MmoItemContextFactory(new FragmentCapturePolicy(captureEnabled, allowlistedStatIds), providers);
            mmoItemsAdapter = new MmoItemsAdapter(contextFactory);
            loreHook = new GenerateLoreHook(contextFactory, debug, applyLayouts, preserveNativeOnUnknown);
            manager.registerEvents(loreHook, owner);
        } catch (LinkageError | RuntimeException unsupported) {
            // MI7 may remove the deprecated event. Keep the plugin alive and retain native lore.
            loreHook = null;
            mmoItemsAdapter = null;
            capabilities = new IntegrationCapabilities(mmoItems, mythicLib, papi, false, false, false);
            logger.warning("[CustomMoreLore] MMOItems lore event is unavailable; native lore remains active.");
            if (debug) logger.warning("[CustomMoreLore] Hook registration detail: " + unsupported.getClass().getSimpleName()
                    + ": " + unsupported.getMessage());
            return capabilities;
        }
        logger.info("[CustomMoreLore] MMOItems GenerateLoreEvent hook enabled (sync, fail-soft).");
        return capabilities;
    }

    public IntegrationCapabilities capabilities() { return capabilities; }
    public GenerateLoreHook loreHook() { return loreHook; }
    public MmoItemContextFactory contextFactory() { return contextFactory; }
    public MmoItemsAdapter mmoItemsAdapter() { return mmoItemsAdapter; }
    public ProviderRegistry providers() { return providers; }
    public Map<String, Boolean> capabilityMap() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        values.put("MMOItems", capabilities.mmoItems());
        values.put("MythicLib", capabilities.mythicLib());
        values.put("PlaceholderAPI", capabilities.placeholderApi());
        values.put("GenerateLoreEvent", capabilities.loreEvent());
        values.put("primary-thread-hook", capabilities.primaryThreadHook());
        values.put("fragment-capture", capabilities.fragmentCapture());
        return Collections.unmodifiableMap(values);
    }
}
