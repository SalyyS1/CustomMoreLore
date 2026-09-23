package vn.saly.custommorelore.provider;

import vn.saly.custommorelore.api.FragmentProvider;
import vn.saly.custommorelore.api.ProviderRequest;
import vn.saly.custommorelore.api.ProviderResult;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/** Conservative raw adapters. Values are supplied by an integration adapter, never scraped from lore. */
public final class BuiltinValueProviders {
    private BuiltinValueProviders() { }

    public static void registerDefaults(ProviderRegistry registry) {
        // The generic adapter is intentionally opt-in by request attribute and is safe for tests/integrations.
        registry.register(new AttributeValueProvider());
    }

    public static final class AttributeValueProvider implements FragmentProvider {
        @Override public String id() { return "builtin-attribute-value"; }
        @Override public Capability capability() { return Capability.VALUE; }
        @Override public boolean supports(ProviderRequest request) {
            return request != null && request.attribute("raw-value") instanceof Number;
        }
        @Override public ProviderResult resolve(ProviderRequest request) {
            Object raw = request.attribute("raw-value");
            if (!(raw instanceof Number number)) return ProviderResult.unsupported("raw-value is not numeric");
            BigDecimal decimal = new BigDecimal(number.toString());
            String text = decimal.stripTrailingZeros().toPlainString();
            if (decimal.compareTo(BigDecimal.ZERO) == 0) return ProviderResult.zero(decimal);
            return ProviderResult.present(decimal, text);
        }
    }

    /** Convenience pure provider backed by a map, useful for integrations and tests. */
    public static FragmentProvider map(String id, FragmentProvider.Capability capability, Map<String, ?> values) {
        Map<String, ?> snapshot = Map.copyOf(values == null ? Map.of() : values);
        return new FragmentProvider() {
            @Override public String id() { return id; }
            @Override public Capability capability() { return capability; }
            @Override public boolean supports(ProviderRequest request) { return request != null && snapshot.containsKey(request.token()); }
            @Override public ProviderResult resolve(ProviderRequest request) {
                Object value = snapshot.get(request.token());
                if (value == null) return ProviderResult.missing();
                if (value instanceof Number n && Double.compare(n.doubleValue(), 0D) == 0) return ProviderResult.zero(n);
                return ProviderResult.present(value, String.valueOf(value));
            }
        };
    }
}
