package vn.saly.custommorelore.layout;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Safe allow-listed condition evaluator with three-valued logic. */
public final class ConditionEvaluator {
    public enum Truth { TRUE, FALSE, UNKNOWN }
    private static final Pattern COMPARISON = Pattern.compile("value:([A-Za-z0-9_.-]+)\\s*(==|!=|>=|<=|>|<)\\s*(-?[0-9]+(?:\\.[0-9]+)?)");

    public Truth evaluate(Condition condition, LoreBuildContext context) {
        if (condition == null) return Truth.TRUE;
        if (condition instanceof Condition.All all) {
            boolean unknown = false;
            for (Condition c : all.children()) { Truth t = evaluate(c, context); if (t == Truth.FALSE) return Truth.FALSE; if (t == Truth.UNKNOWN) unknown = true; }
            return unknown ? Truth.UNKNOWN : Truth.TRUE;
        }
        if (condition instanceof Condition.Any any) {
            boolean unknown = false;
            for (Condition c : any.children()) { Truth t = evaluate(c, context); if (t == Truth.TRUE) return Truth.TRUE; if (t == Truth.UNKNOWN) unknown = true; }
            return unknown ? Truth.UNKNOWN : Truth.FALSE;
        }
        Condition.Predicate p = (Condition.Predicate) condition;
        return switch (p.kind()) {
            case HAS -> has(p.argument(), context);
            case TYPE -> Objects.equals(p.argument(), context.itemType()) ? Truth.TRUE : Truth.FALSE;
            case ID -> Objects.equals(p.argument(), context.itemId()) ? Truth.TRUE : Truth.FALSE;
            case TIER -> context.tier() == null ? Truth.UNKNOWN : (Objects.equals(p.argument(), context.tier()) ? Truth.TRUE : Truth.FALSE);
            case PERMISSION -> context.viewer() == null ? Truth.UNKNOWN : (context.viewer().hasPermission(p.argument()) ? Truth.TRUE : Truth.FALSE);
            case VALUE -> compare(p, context);
        };
    }
    private Truth has(String token, LoreBuildContext context) {
        ProviderResult r = provider(context).resolve(token, TokenMode.FRAGMENT, context);
        return switch (r.state()) {
            case PRESENT, ZERO -> Truth.TRUE;
            case MISSING, EMPTY -> Truth.FALSE;
            case UNKNOWN, UNSUPPORTED -> Truth.UNKNOWN;
        };
    }
    private Truth compare(Condition.Predicate p, LoreBuildContext context) {
        ProviderResult r = provider(context).resolve(p.argument(), TokenMode.VALUE, context);
        if (r.state() == ProviderResult.State.UNKNOWN || r.state() == ProviderResult.State.UNSUPPORTED || r.state() == ProviderResult.State.MISSING || r.state() == ProviderResult.State.EMPTY) return Truth.UNKNOWN;
        double left = r.state() == ProviderResult.State.ZERO ? 0 : r.numericValue();
        // Raw numeric adapters must provide numericValue; never infer numbers from formatted text.
        if (Double.isNaN(left)) return Truth.UNKNOWN;
        boolean result = switch (p.operator()) {
            case EQ -> left == p.number(); case NE -> left != p.number(); case GT -> left > p.number();
            case GTE -> left >= p.number(); case LT -> left < p.number(); case LTE -> left <= p.number(); case NONE -> false;
        };
        return result ? Truth.TRUE : Truth.FALSE;
    }
    private static FragmentProvider provider(LoreBuildContext context) { return context.provider() == null ? FragmentProvider.EMPTY : context.provider(); }

    @SuppressWarnings("unchecked")
    public static Condition parse(Object raw) {
        return parse(raw, 0);
    }
    private static Condition parse(Object raw, int depth) {
        if (depth > 8) throw new IllegalArgumentException("condition nesting exceeds bound (8)");
        if (raw == null) return new Condition.All(List.of());
        if (raw instanceof String text) return parseString(text);
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("condition must be string or map");
        if (map.containsKey("all")) return new Condition.All(children(map.get("all"), depth + 1));
        if (map.containsKey("any")) return new Condition.Any(children(map.get("any"), depth + 1));
        for (Condition.Predicate.Kind kind : Condition.Predicate.Kind.values()) {
            String key = switch (kind) { case HAS -> "has"; case TYPE -> "type"; case ID -> "id"; case TIER -> "tier"; case PERMISSION -> "permission"; case VALUE -> "value"; };
            if (map.containsKey(key)) {
                String value = String.valueOf(map.get(key));
                if (kind == Condition.Predicate.Kind.VALUE) return parseString("value:" + value);
                return new Condition.Predicate(kind, value, Condition.Predicate.Operator.NONE, 0);
            }
        }
        throw new IllegalArgumentException("unknown condition predicate");
    }
    private static List<Condition> children(Object raw, int depth) {
        if (!(raw instanceof Collection<?> c)) throw new IllegalArgumentException("all/any must be a list");
        if (c.size() > 16) throw new IllegalArgumentException("condition nesting/list exceeds bound");
        return c.stream().map(item -> parse(item, depth)).toList();
    }
    private static Condition parseString(String text) {
        String value = text.trim();
        Matcher comparison = COMPARISON.matcher(value);
        if (comparison.matches()) {
            Condition.Predicate.Operator op = switch (comparison.group(2)) { case "==" -> Condition.Predicate.Operator.EQ; case "!=" -> Condition.Predicate.Operator.NE; case ">" -> Condition.Predicate.Operator.GT; case ">=" -> Condition.Predicate.Operator.GTE; case "<" -> Condition.Predicate.Operator.LT; default -> Condition.Predicate.Operator.LTE; };
            return new Condition.Predicate(Condition.Predicate.Kind.VALUE, comparison.group(1), op, Double.parseDouble(comparison.group(3)));
        }
        int colon = value.indexOf(':');
        if (colon < 1) throw new IllegalArgumentException("invalid condition: " + text);
        Condition.Predicate.Kind kind = switch (value.substring(0, colon).toLowerCase(Locale.ROOT)) {
            case "has" -> Condition.Predicate.Kind.HAS; case "type" -> Condition.Predicate.Kind.TYPE; case "id" -> Condition.Predicate.Kind.ID;
            case "tier" -> Condition.Predicate.Kind.TIER; case "permission" -> Condition.Predicate.Kind.PERMISSION; default -> throw new IllegalArgumentException("unknown condition: " + text);
        };
        return new Condition.Predicate(kind, value.substring(colon + 1), Condition.Predicate.Operator.NONE, 0);
    }
}
