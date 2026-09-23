package vn.saly.custommorelore.provider;

import vn.saly.custommorelore.api.ColumnSpec;
import vn.saly.custommorelore.api.ProviderResult;

import java.util.*;
import java.util.function.Function;

/** Joins explicit logical columns. It never pads by pixel width and rejects multiline blocks. */
public final class ColumnComposer {
    public Result compose(List<ColumnSpec> columns, Function<ColumnSpec, ProviderResult> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        if (columns == null || columns.isEmpty()) return new Result("", List.of(), false);
        List<String> values = new ArrayList<>(); List<String> diagnostics = new ArrayList<>();
        String separator = " ";
        for (ColumnSpec column : columns) {
            ProviderResult result;
            try { result = resolver.apply(column); }
            catch (RuntimeException ex) { diagnostics.add(column.token() + ": provider error"); continue; }
            if (result == null) { diagnostics.add(column.token() + ": null result"); continue; }
            if (!result.lines().isEmpty()) { diagnostics.add(column.token() + ": block token is invalid in a column"); return new Result("", diagnostics, true); }
            if (result.visible()) values.add(result.text() == null ? String.valueOf(result.value()) : result.text());
            else if (!"remove".equalsIgnoreCase(column.absent())) values.add("");
            if (values.size() == 1) separator = column.separator();
        }
        return new Result(String.join(separator, values), diagnostics, false);
    }
    public record Result(String text, List<String> diagnostics, boolean rejected) { public Result { diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics); } }
}
