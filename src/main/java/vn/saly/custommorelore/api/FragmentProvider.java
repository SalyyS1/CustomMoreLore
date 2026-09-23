package vn.saly.custommorelore.api;

/**
 * Extension point for values used by a lore layout. Providers are registered
 * while the plugin is enabling and are treated as immutable during a render.
 * Implementations should be pure and must not mutate the requested item.
 */
public interface FragmentProvider {
    String id();
    Capability capability();
    boolean supports(ProviderRequest request);
    ProviderResult resolve(ProviderRequest request);

    enum Capability { FRAGMENT, VALUE, BLOCK }
}
