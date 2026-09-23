package vn.saly.custommorelore.api;

import org.junit.jupiter.api.Test;
import vn.saly.custommorelore.provider.BuiltinValueProviders;
import vn.saly.custommorelore.provider.ProviderRegistry;

import static org.junit.jupiter.api.Assertions.*;

class ProviderRegistryTest {
    @Test void duplicateIdsAreRejectedAndZeroIsVisible() {
        ProviderRegistry registry = new ProviderRegistry();
        BuiltinValueProviders.registerDefaults(registry);
        ProviderResult result = registry.resolve("builtin-attribute-value", ProviderRequest.builder("attack-damage").attribute("raw-value", 0).build());
        assertEquals(ProviderResult.State.ZERO, result.state());
        assertTrue(result.visible());
        assertThrows(IllegalArgumentException.class, () -> BuiltinValueProviders.registerDefaults(registry));
    }
    @Test void providerExceptionBecomesTypedError() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.register(new FragmentProvider() {
            public String id() { return "broken"; }
            public Capability capability() { return Capability.FRAGMENT; }
            public boolean supports(ProviderRequest request) { return true; }
            public ProviderResult resolve(ProviderRequest request) { throw new IllegalStateException("boom"); }
        });
        assertEquals(ProviderResult.State.ERROR, registry.resolve("broken", new ProviderRequest("x", "fragment")).state());
    }
}
