package vn.saly.custommorelore.provider;

import vn.saly.custommorelore.api.FragmentProvider;
import vn.saly.custommorelore.api.CustomMoreLoreProviderEvent;
import vn.saly.custommorelore.api.ProviderRequest;
import vn.saly.custommorelore.api.ProviderResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.plugin.PluginManager;

/** Thread-safe registration point. A render gets a stable immutable snapshot. */
public final class ProviderRegistry {
    private final AtomicReference<Map<String, FragmentProvider>> providers = new AtomicReference<>(Map.of());

    public void register(FragmentProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = Objects.requireNonNull(provider.id(), "provider.id").trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Provider id cannot be empty");
        providers.updateAndGet(current -> {
            if (current.containsKey(id)) throw new IllegalArgumentException("Duplicate provider id: " + id);
            Map<String, FragmentProvider> next = new LinkedHashMap<>(current);
            next.put(id, provider);
            return Map.copyOf(next);
        });
    }
    /** Dispatches the cancellable registration event before inserting the provider. */
    public boolean register(PluginManager pluginManager, FragmentProvider provider) {
        Objects.requireNonNull(pluginManager, "pluginManager");
        CustomMoreLoreProviderEvent event = new CustomMoreLoreProviderEvent(provider);
        pluginManager.callEvent(event);
        if (event.isCancelled()) return false;
        register(provider); return true;
    }
    public void registerAll(Collection<? extends FragmentProvider> values) {
        if (values == null) return;
        for (FragmentProvider value : values) register(value);
    }
    public boolean unregister(String id) {
        if (id == null) return false;
        final boolean[] changed = {false};
        providers.updateAndGet(current -> {
            if (!current.containsKey(id)) return current;
            changed[0] = true;
            Map<String, FragmentProvider> next = new LinkedHashMap<>(current);
            next.remove(id);
            return Map.copyOf(next);
        });
        return changed[0];
    }
    public Optional<FragmentProvider> find(String id) { return Optional.ofNullable(providers.get().get(id)); }
    public Map<String, FragmentProvider> snapshot() { return providers.get(); }
    public int size() { return providers.get().size(); }

    /** Resolve safely. Provider exceptions become typed errors and never break a lore build. */
    public ProviderResult resolve(String id, ProviderRequest request) {
        FragmentProvider provider = providers.get().get(id);
        if (provider == null) return ProviderResult.unsupported("No provider registered: " + id);
        try {
            if (!provider.supports(request)) return ProviderResult.unsupported("Provider does not support request: " + id);
            ProviderResult result = provider.resolve(request);
            return result == null ? ProviderResult.error("Provider returned null: " + id) : result;
        } catch (RuntimeException ex) {
            return ProviderResult.error(id + ": " + ex.getClass().getSimpleName());
        }
    }

    /** Resolves the first provider that claims a request; registration order is deterministic. */
    public ProviderResult resolve(ProviderRequest request) {
        if (request == null) return ProviderResult.error("null provider request");
        for (FragmentProvider provider : providers.get().values()) {
            try {
                if (provider.supports(request)) {
                    ProviderResult result = provider.resolve(request);
                    return result == null ? ProviderResult.error("Provider returned null: " + provider.id()) : result;
                }
            } catch (RuntimeException ex) { return ProviderResult.error(provider.id() + ": " + ex.getClass().getSimpleName()); }
        }
        return ProviderResult.unsupported("No provider supports request: " + request.token());
    }
}
