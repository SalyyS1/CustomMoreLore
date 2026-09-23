package vn.saly.custommorelore.layout;

import java.util.List;

public sealed interface Condition permits Condition.Predicate, Condition.All, Condition.Any {
    record Predicate(Kind kind, String argument, Operator operator, double number) implements Condition {
        public enum Kind { HAS, TYPE, ID, TIER, PERMISSION, VALUE }
        public enum Operator { NONE, EQ, NE, GT, GTE, LT, LTE }
    }
    record All(List<Condition> children) implements Condition { public All { children = List.copyOf(children); } }
    record Any(List<Condition> children) implements Condition { public Any { children = List.copyOf(children); } }
}
