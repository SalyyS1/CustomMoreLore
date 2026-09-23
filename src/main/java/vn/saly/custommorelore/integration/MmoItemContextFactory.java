package vn.saly.custommorelore.integration;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import me.clip.placeholderapi.PlaceholderAPI;
import io.lumine.mythic.lib.MythicLib;
import org.bukkit.entity.Player;
import vn.saly.custommorelore.layout.FragmentProvider;
import vn.saly.custommorelore.layout.LoreBuildContext;
import vn.saly.custommorelore.layout.ProviderResult;
import vn.saly.custommorelore.layout.TokenMode;
import vn.saly.custommorelore.model.FragmentCapturePolicy;
import vn.saly.custommorelore.model.LoreSnapshot;
import vn.saly.custommorelore.model.MmoItemIdentity;
import vn.saly.custommorelore.provider.ProviderRegistry;
import vn.saly.custommorelore.api.ProviderRequest;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Optional;

/** Creates a neutral layout context without allowing the renderer to mutate MMOItems state. */
public final class MmoItemContextFactory {
    private final FragmentCapturePolicy capturePolicy;
    private final ProviderRegistry extensionProviders;

    public MmoItemContextFactory(FragmentCapturePolicy capturePolicy) {
        this(capturePolicy, null);
    }
    public MmoItemContextFactory(FragmentCapturePolicy capturePolicy, ProviderRegistry extensionProviders) {
        this.capturePolicy = capturePolicy == null ? new FragmentCapturePolicy(false, java.util.Set.of()) : capturePolicy;
        this.extensionProviders = extensionProviders;
    }

    public LoreBuildContext create(MMOItem item, LoreSnapshot snapshot, Player viewer) {
        MmoItemIdentity identity = identity(item);
        List<String> nativeLore = snapshot == null ? List.of() : snapshot.parsed();
        return new Context(identity, nativeLore, item, viewer, capturePolicy, extensionProviders);
    }

    public static MmoItemIdentity identity(MMOItem item) {
        if (item == null) return new MmoItemIdentity("", "", "", 0);
        String tier = "";
        try { if (item.getTier() != null) tier = item.getTier().getName(); } catch (Throwable ignored) { }
        int upgrade = 0;
        try { upgrade = item.getUpgradeLevel(); } catch (Throwable ignored) { }
        return new MmoItemIdentity(item.getType() == null ? "" : item.getType().getId(), item.getId(), tier, upgrade);
    }

    private static final class Context implements LoreBuildContext {
        private final MmoItemIdentity identity;
        private final List<String> nativeLore;
        private final MMOItem item;
        private final Player viewer;
        private final FragmentProvider provider;
        private final ProviderRegistry extensionProviders;

        private Context(MmoItemIdentity identity, List<String> nativeLore, MMOItem item, Player viewer,
                        FragmentCapturePolicy capturePolicy, ProviderRegistry extensionProviders) {
            this.identity = identity;
            this.nativeLore = List.copyOf(nativeLore);
            this.item = item;
            this.viewer = viewer;
            this.extensionProviders = extensionProviders;
            this.provider = (token, mode, ignored) -> resolve(token, mode, capturePolicy);
        }

        private ProviderResult resolve(String rawToken, TokenMode mode, FragmentCapturePolicy capturePolicy) {
            if (rawToken == null || rawToken.isBlank()) return ProviderResult.missing();
            String token = rawToken.trim();
            if (token.startsWith("#") && token.endsWith("#") && token.length() > 2) token = token.substring(1, token.length() - 1);
            String lower = token.toLowerCase(Locale.ROOT);
            if (extensionProviders != null) {
                vn.saly.custommorelore.api.ProviderResult external = extensionProviders.resolve(
                        ProviderRequest.builder(token).mode(mode.name().toLowerCase(Locale.ROOT)).item(item).viewer(viewer).build());
                if (external.state() != vn.saly.custommorelore.api.ProviderResult.State.UNSUPPORTED)
                    return convertExternal(external);
            }
            if (lower.equals("native-lore")) return ProviderResult.lines(nativeLore, "mmoitems-native");
            if (token.startsWith("%") || token.startsWith("{") || token.startsWith("[")) return resolvePlaceholder(token);
            if (lower.equals("lore")) {
                try {
                    ProviderResult lore = blockResult(item.getData(ItemStats.LORE), "mmoitems-lore");
                    return lore == null ? ProviderResult.missing() : lore;
                } catch (RuntimeException ignored) { return ProviderResult.unsupported("MMOItems lore data unavailable"); }
            }
            if (mode == TokenMode.FRAGMENT) {
                if (lower.equals("abilities")) {
                    ProviderResult block = blockResult(safeData(ItemStats.ABILITIES), "mmoitems-abilities");
                    if (block != null) return block;
                }
                if (lower.equals("elements")) {
                    ProviderResult block = blockResult(safeData(ItemStats.ELEMENTS), "mmoitems-elements");
                    if (block != null) return block;
                }
                if (lower.equals("effects")) {
                    ProviderResult block = blockResult(safeData(ItemStats.EFFECTS), "mmoitems-effects");
                    if (block != null) return block;
                }
            }
            ItemStat found = null;
            try {
                for (ItemStat stat : item.getStats()) {
                    if (stat.getId().equalsIgnoreCase(token) || stat.getNBTPath().equalsIgnoreCase(token)) { found = stat; break; }
                }
            } catch (Throwable ignored) { return ProviderResult.unsupported("MMOItems stat API unavailable"); }
            if (found == null) return ProviderResult.missing();
            StatData data;
            try { data = item.getData(found); } catch (Throwable ignored) { return ProviderResult.unsupported("stat data unavailable"); }
            if (data == null) return ProviderResult.empty();
            if (mode == TokenMode.FRAGMENT && capturePolicy.allows(found.getId())) {
                ProviderResult fragment = nativeFragment(found, data);
                if (fragment != null) return fragment;
            }
            if (mode == TokenMode.FRAGMENT && lower.equals("abilities")) {
                ProviderResult block = blockResult(data, "mmoitems-abilities");
                if (block != null) return block;
            }
            // Reading StatData is side-effect free. Native ItemStat rendering is not exposed as a pure API on
            // every supported MMOItems build, so the safe fallback is a scalar getter (when available) or a
            // stable value string. Allowlisted capture remains a capability marker for future render adapters.
            String rendered = scalarText(found, data, mode == TokenMode.FRAGMENT);
            if (rendered == null || rendered.isBlank()) return ProviderResult.empty();
            try {
                double number = Double.parseDouble(rendered);
                return ProviderResult.value(number, rendered, "mmoitems-scalar");
            } catch (NumberFormatException ignored) { }
            return ProviderResult.present(rendered, "mmoitems-scalar");
        }

        private StatData safeData(ItemStat stat) {
            try { return item.getData(stat); } catch (RuntimeException ignored) { return null; }
        }

        private static ProviderResult convertExternal(vn.saly.custommorelore.api.ProviderResult result) {
            return switch (result.state()) {
                case PRESENT -> result.lines().isEmpty() ? ProviderResult.present(result.text(), "extension") : ProviderResult.lines(result.lines(), "extension");
                case ZERO -> ProviderResult.zero(result.value() instanceof Number n ? n.doubleValue() : 0D, "extension");
                case EMPTY -> ProviderResult.empty();
                case MISSING -> ProviderResult.missing();
                case UNKNOWN -> ProviderResult.unknown(result.reason());
                case UNSUPPORTED -> ProviderResult.unsupported(result.reason());
                case ERROR -> ProviderResult.unsupported(result.reason());
            };
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static ProviderResult nativeFragment(ItemStat stat, StatData data) {
            List<String> lines = new ArrayList<>();
            try { stat.whenDisplayed(lines, Optional.of(data)); }
            catch (RuntimeException ignored) { return null; }
            lines.removeIf(line -> line == null || line.isEmpty());
            return lines.isEmpty() ? null : ProviderResult.lines(lines, "mmoitems-when-displayed");
        }

        private static ProviderResult blockResult(StatData data, String provider) {
            if (data == null || data.isEmpty()) return ProviderResult.empty();
            if (data instanceof StringListData list) return ProviderResult.lines(list.getList(), provider);
            if (data instanceof StringData string) return ProviderResult.lines(List.of(string.getString()), provider);
            if (data instanceof AbilityListData abilities) {
                List<String> lines = abilities.getAbilities().stream().filter(a -> !a.hidesFromLore()).map(String::valueOf).toList();
                return lines.isEmpty() ? ProviderResult.empty() : ProviderResult.lines(lines, provider);
            }
            if (data instanceof net.Indyuce.mmoitems.stat.data.ElementListData elements)
                return ProviderResult.lines(List.of(String.valueOf(elements)), provider);
            return null;
        }

        private ProviderResult resolvePlaceholder(String token) {
            if (viewer == null) return ProviderResult.unknown("viewer context unavailable");
            try {
                String value = PlaceholderAPI.setPlaceholders(viewer, token);
                if (value != null && !value.equals(token)) return ProviderResult.present(value, "placeholderapi");
            } catch (LinkageError e) {
                // Optional dependency: continue with MythicLib parser.
            } catch (RuntimeException ignored) { }
            try {
                String value = MythicLib.inst().getPlaceholderParser().parse(viewer, token);
                if (value != null && !value.equals(token)) return ProviderResult.present(value, "mythiclib");
            } catch (LinkageError ignored) {
                return ProviderResult.unsupported("PlaceholderAPI/MythicLib is not installed");
            } catch (RuntimeException ignored) { }
            return ProviderResult.missing();
        }

        private static String scalarText(ItemStat stat, StatData data, boolean formatted) {
            if (data instanceof DoubleData numeric) {
                double value = numeric.getValue();
                if (formatted) try { return stat.formatNumericStat(value); } catch (RuntimeException ignored) { }
                    return value == (long) value ? Long.toString((long) value) : Double.toString(value);
            }
            return data.toString();
        }

        @Override public String itemId() { return identity.id(); }
        @Override public String itemType() { return identity.type(); }
        @Override public String tier() { return identity.tier(); }
        @Override public List<String> nativeLore() { return nativeLore; }
        @Override public boolean nativeTooltipPresent() {
            try { return item.getTooltip() != null; } catch (Throwable ignored) { return false; }
        }
        @Override public FragmentProvider provider() { return provider; }
        @Override public ViewerContext viewer() {
            if (viewer == null) return null;
            return new ViewerContext() {
                @Override public String name() { return viewer.getName(); }
                @Override public boolean hasPermission(String permission) { return viewer.hasPermission(permission); }
            };
        }
    }
}
