package vn.saly.custommorelore.integration;

import net.Indyuce.mmoitems.api.event.GenerateLoreEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vn.saly.custommorelore.layout.LoreBuildContext;
import vn.saly.custommorelore.model.LoreSnapshot;
import vn.saly.custommorelore.model.LoreTransformation;
import vn.saly.custommorelore.service.ViewerPersistenceGuard;

import java.util.List;
import java.util.logging.Level;

/** Synchronous MMOItems bridge. Any formatter error leaves the native result untouched. */
@SuppressWarnings("deprecation")
public final class GenerateLoreHook implements Listener {
    private final MmoItemContextFactory contextFactory;
    private final boolean debug;
    private final boolean applyLayouts;
    private final boolean preserveNativeOnUnknown;
    private final ThreadLocal<Boolean> inHook = ThreadLocal.withInitial(() -> false);
    private volatile LoreTransformation transformation;

    public GenerateLoreHook(MmoItemContextFactory contextFactory, boolean debug, boolean applyLayouts,
                            boolean preserveNativeOnUnknown) {
        this.contextFactory = contextFactory;
        this.debug = debug;
        this.applyLayouts = applyLayouts;
        this.preserveNativeOnUnknown = preserveNativeOnUnknown;
    }

    public void setTransformation(LoreTransformation transformation) { this.transformation = transformation; }
    public LoreTransformation transformation() { return transformation; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGenerateLore(GenerateLoreEvent event) {
        if (event == null || inHook.get() || !Bukkit.isPrimaryThread()) return;
        LoreSnapshot snapshot = BaselineLoreSnapshot.from(event);
        if (debug) {
            Bukkit.getLogger().fine("[CustomMoreLore] GenerateLoreEvent " + event.getItem().getType().getId()
                    + ":" + event.getItem().getId() + " lines=" + snapshot.parsed().size()
                    + " aliased=" + snapshot.sameBackingList());
        }
        if (!applyLayouts || transformation == null) return;
        inHook.set(true);
        try {
            LoreBuildContext context = contextFactory.create(event.getItem(), snapshot, null);
            List<String> transformed = transformation.transform(context);
            // An empty list is a valid intentional layout (it clears lore); null means the formatter declined.
            if (transformed == null) return;
            if (transformed.stream().anyMatch(ViewerPersistenceGuard::isViewerDependent)) return;
            if (preserveNativeOnUnknown && transformed.equals(snapshot.parsed())) return;
            // setFinalLore mutates only MMOItems' event list; NBT, stats and the source MMOItem remain untouched.
            event.setFinalLore(List.copyOf(transformed));
        } catch (Throwable failure) {
            Bukkit.getLogger().log(debug ? Level.WARNING : Level.FINE,
                    "[CustomMoreLore] Formatter failed; retaining native MMOItems lore", failure);
        } finally {
            inHook.remove();
        }
    }
}
