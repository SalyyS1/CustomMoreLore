package vn.saly.custommorelore.layout;

/** Provider for already-rendered MMOItems/PAPI fragments and explicit raw values. */
@FunctionalInterface
public interface FragmentProvider {
    ProviderResult resolve(String token, TokenMode mode, LoreBuildContext context);
    default ProviderResult resolve(String token, LoreBuildContext context) { return resolve(token, TokenMode.FRAGMENT, context); }
    FragmentProvider EMPTY = (token, mode, context) -> token.startsWith("%") ? ProviderResult.unknown("viewer context unavailable") : ProviderResult.unsupported("no provider for " + token);
}
