package vn.saly.custommorelore.layout;

import vn.saly.custommorelore.config.*;

/** Applies configured state policy to typed provider results before composition. */
public final class FragmentResolver {
    public record Resolution(ProviderResult result, boolean visible, boolean nativeFallback, boolean error) {}
    private final FormatPolicies policies;
    public FragmentResolver(FormatPolicies policies) { this.policies = policies == null ? FormatPolicies.defaults() : policies; }
    public Resolution resolve(String token, TokenMode mode, LoreBuildContext context) {
        ProviderResult result = (context == null || context.provider() == null ? FragmentProvider.EMPTY : context.provider()).resolve(token, mode, context);
        return apply(result);
    }
    public Resolution apply(ProviderResult result) {
        if (result == null) result = ProviderResult.unknown("null provider result");
        return switch (result.state()) {
            case PRESENT, ZERO -> new Resolution(result, result.visible(), false, false);
            case MISSING -> policy(result, policies.missing());
            case EMPTY -> policy(result, policies.empty());
            case UNKNOWN -> policy(result, policies.unknown());
            case UNSUPPORTED -> policy(result, policies.unsupported());
        };
    }
    private Resolution policy(ProviderResult result, FormatPolicy policy) {
        return switch (policy) {
            case HIDE, EMPTY -> new Resolution(result, false, false, false);
            case NATIVE -> new Resolution(result, false, true, false);
            case ERROR -> new Resolution(result, false, false, true);
        };
    }
}
