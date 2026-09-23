package vn.saly.custommorelore.config;

import java.util.Objects;

/** Immutable state and condition policies for one compiled configuration. */
public final class FormatPolicies {
    private final FormatPolicy missing;
    private final FormatPolicy empty;
    private final FormatPolicy unknown;
    private final FormatPolicy unsupported;
    private final InvalidInlineBlock invalidInlineBlock;
    private final ConditionUnknown conditionUnknown;

    public enum InvalidInlineBlock { NATIVE, REJECT_PROFILE }
    public enum ConditionUnknown { HIDE, KEEP_NATIVE }

    public FormatPolicies(FormatPolicy missing, FormatPolicy empty, FormatPolicy unknown,
                          FormatPolicy unsupported, InvalidInlineBlock invalidInlineBlock,
                          ConditionUnknown conditionUnknown) {
        this.missing = Objects.requireNonNull(missing);
        this.empty = Objects.requireNonNull(empty);
        this.unknown = Objects.requireNonNull(unknown);
        this.unsupported = Objects.requireNonNull(unsupported);
        this.invalidInlineBlock = Objects.requireNonNull(invalidInlineBlock);
        this.conditionUnknown = Objects.requireNonNull(conditionUnknown);
    }

    public static FormatPolicies defaults() {
        return new FormatPolicies(FormatPolicy.HIDE, FormatPolicy.HIDE, FormatPolicy.NATIVE,
            FormatPolicy.NATIVE, InvalidInlineBlock.NATIVE, ConditionUnknown.HIDE);
    }
    public FormatPolicy missing() { return missing; }
    public FormatPolicy empty() { return empty; }
    public FormatPolicy unknown() { return unknown; }
    public FormatPolicy unsupported() { return unsupported; }
    public InvalidInlineBlock invalidInlineBlock() { return invalidInlineBlock; }
    public ConditionUnknown conditionUnknown() { return conditionUnknown; }
}
