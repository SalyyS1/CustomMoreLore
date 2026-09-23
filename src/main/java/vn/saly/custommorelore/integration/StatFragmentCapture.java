package vn.saly.custommorelore.integration;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import vn.saly.custommorelore.layout.ProviderResult;
import vn.saly.custommorelore.model.FragmentCapturePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Experimental, read-only scalar capture. It never calls ItemStat.whenApplied or rebuilds an item. */
public final class StatFragmentCapture {
    private final FragmentCapturePolicy policy;
    public StatFragmentCapture(FragmentCapturePolicy policy) { this.policy = policy; }
    public ProviderResult capture(MMOItem item, String statId) {
        if (item == null || statId == null || !policy.allows(statId)) return ProviderResult.unsupported("capture not allowlisted");
        try {
            ItemStat found = null;
            for (ItemStat stat : item.getStats()) if (stat.getId().equalsIgnoreCase(statId)) { found = stat; break; }
            if (found == null) return ProviderResult.missing();
            StatData data = item.getData(found);
            if (data == null) return ProviderResult.empty();
            List<String> lines = new ArrayList<>();
            try { found.whenDisplayed(lines, Optional.of(data)); }
            catch (RuntimeException ignored) { }
            lines.removeIf(line -> line == null || line.isBlank());
            if (!lines.isEmpty()) return ProviderResult.lines(lines, "mmoitems-when-displayed");
            String text = String.valueOf(data);
            return text.isBlank() ? ProviderResult.empty() : ProviderResult.present(text, "mmoitems-data");
        } catch (LinkageError e) {
            return ProviderResult.unsupported("MMOItems API unavailable");
        } catch (RuntimeException e) {
            return ProviderResult.unknown("native fragment capture failed");
        }
    }
}
