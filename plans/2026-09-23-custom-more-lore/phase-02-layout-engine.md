---
phase: 2
title: "Config model, compiler, and renderer"
status: completed
priority: P1
effort: "3-4d"
dependencies: [phase-01-bootstrap-and-spike]
---

# Phase 2: Config model, compiler, and renderer

## Overview

Build the immutable `formats.yml` schema, compiler, matcher, condition evaluator, and renderer used by the MMOItems adapter. The phase owns the DSL contract in [docs/configuration-design.md](../../docs/configuration-design.md); it does not implement Bukkit event wiring. Phase 3 and Phase 2 agree on the provider-result contract at their shared start gate; Phase 2 remains testable with fakes while Phase 3 implements live MMOItems providers.

## Requirements

- Compile versioned YAML profiles into an AST with source file/line/column on every node.
- Render MMOItems-formatted scalar fragments (`#attack-damage#`) and explicit raw values (`#attack-damage:value#`) through providers; never parse visible text to obtain a number.
- Support string shorthand with literal spaces preserved and structured rows with `parts`, `join`, `absent`, and `empty` policies.
- Support block-only multiline tokens (`#abilities#`, `#elements#`, `#effects#`, `#lore#`, `#native-lore#`) without arbitrary flattening.
- Support allowlisted `all`/`any` conditions with documented three-valued unknown behavior; no scripting or evaluator.
- Distinguish `MISSING`, `EMPTY`, `ZERO`, `UNKNOWN`, and `UNSUPPORTED` results.
- Select profiles deterministically by item override > MMOItems type > default, then priority descending and profile ID ascending. A persistent item selection must not depend on viewer permission or PlaceholderAPI context.
- Parse static MiniMessage/legacy text exactly once at the final boundary. PAPI/provider output is literal/already-rendered and is never reparsed as MiniMessage.
- Keep unmatched items' original native lore and preserve the last-known-good compiled snapshot on reload failure.

## Data flow

```text
formats.yml candidate
  -> FormatConfigLoader (YAML + version)
  -> FormatValidator (schema, aliases, types, token modes, conditions)
  -> LayoutCompiler (immutable AST + source locations)
  -> ProfileRegistry (atomic snapshot/cache)

LoreBuildContext (item ID/type, baseline lore, provider registry, optional viewer)
  -> ProfileSelector
  -> ConditionEvaluator + FragmentResolver
  -> RowComposer (structured separator/absence rules)
  -> TextBoundary (MiniMessage/legacy once)
  -> List<Component>/legacy lines OR native fallback
```

The registry is process-scoped and replaced atomically on reload. `LoreBuildContext`, provider results, and condition state are build-scoped; no player or mutable item data may be stored in the compiled profile. Rendered lore is never cached because upgrades, durability, gems, random stats, and placeholders can change between builds.

## Configuration contract

The complete contract and examples live in `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/docs/configuration-design.md`. The minimum v1 shape is:

```yaml
version: 1
policies:
  missing: hide
  empty: hide
  unknown: native
  unsupported: native
  invalid-inline-block: native
  condition-unknown: hide
aliases:
  damage: attack-damage
profiles:
  default: { priority: 0, layout: [] }
  types:
    SWORD: { priority: 20, layout: [] }
  items:
    rpg:sunblade: { priority: 100, layout: [] }
```

`SWORD` is an example MMOItems type key. Do not assume `WEAPON`; Phase 1/3 validate keys against the installed registry. `permission` matching is accepted only for unsaved player previews and is skipped (with a diagnostic) for persistent item formatting. `%player_name%` and similar PAPI tokens become `UNKNOWN` without an explicit viewer context and cannot be written to an ItemStack.

Token rules are exact and explicit: `#token#` is a formatted fragment, `#token:value#` is a registered numeric adapter, aliases map one exact name to one canonical token, and block tokens must occupy a block line/section. `#lore#` is MMOItems custom description only; `#native-lore#` is the complete baseline snapshot. A profile cannot combine `#native-lore#` with `native-lore: prepend|append`, preventing duplicate native output.

## Architecture

Create a small AST rather than regex replacement:

- `LiteralNode` — static text; retains spaces and source span.
- `ScalarTokenNode` — canonical token, `fragment` or explicit `value` mode.
- `BlockTokenNode` — list-producing node valid only as a line/section body.
- `RowNode` — structured parts, join separator, absence and empty policies.
- `ConditionalNode` — bounded `all`/`any` predicates and nested sections.
- `SeparatorNode` — explicit bars/separators, never inferred from whitespace.

Provider results implement a sealed/closed result model with the six states defined in the configuration design. The renderer applies state policy before row composition. `ZERO` is content; `MISSING`/`EMPTY` may remove a part; `UNKNOWN` and `UNSUPPORTED` follow configured fallback. A block result expands to complete lines; an inline use produces a source-located validation diagnostic and either native-line fallback or profile rejection.

Profile selection uses three maps (`items`, `types`, `default`) and deterministic ordering. A type matcher uses the MMOItems type ID (`SWORD`, etc.) from `LoreBuildContext`; it must not infer a broad category. The selected profile ID and score are included in diagnostics.

Text processing carries representation metadata so static config is converted once. `MiniMessage` parses only configured text; provider/PAPI values are inserted as literal/components. A second parse or automatic tag injection is a test failure.

## Related code files

Create:

- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/config/FormatConfigLoader.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/config/FormatProfile.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/config/FormatValidator.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/LayoutCompiler.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/CompiledLayout.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/LoreRenderer.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/FragmentResolver.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/ProviderResult.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/ConditionEvaluator.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/layout/ProfileSelector.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/resources/formats.yml`

Tests (owned by Phase 2; integration fixtures are Phase 3/5):

- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/layout/LayoutCompilerTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/layout/LoreRendererTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/layout/ConditionEvaluatorTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/layout/ProfileSelectorTest.java`

## Implementation steps

1. Define schema version, immutable DTOs, source locations, and typed provider-result states.
2. Implement exact token/alias parsing and reject unknown aliases, aliases that collide with canonical IDs, malformed `:value` forms, and unsupported block placement.
3. Implement profile validation and selection maps. Verify item-ID > type > default precedence; tie-break by priority then lexical profile ID; record skipped permission/PAPI matchers when no viewer exists.
4. Compile shorthand lines while preserving every literal space; compile structured rows with explicit `join`, `absent`, and `empty` behavior.
5. Implement bounded condition parsing/evaluation (`has`, `type`, `id`, `tier`, preview-only permission, and explicit numeric comparisons) with `all`/`any` three-valued semantics.
6. Implement fragment resolution and line composition. Ensure `ZERO` renders, missing parts can be removed without dangling separators, and block output stays multiline.
7. Add one final text-boundary conversion for MiniMessage/legacy; mark provider/PAPI fragments as literal/already-rendered to prevent reparsing.
8. Add atomic profile registry replacement and native fallback for rejected candidate snapshots.
9. Add unit/golden tests and diagnostics snapshots before handing provider interfaces to Phase 3.

## Test matrix

| Area | Cases | Expected |
|---|---|---|
| Shorthand | two scalar tokens, repeated spaces, literal bars | exact spaces/text retained |
| Structured row | first/middle/last part missing; all missing; zero value | separators compact; zero visible; empty row removed |
| States | MISSING, EMPTY, ZERO, UNKNOWN, UNSUPPORTED | independent policy behavior; no string guessing |
| Blocks | standalone abilities/elements/lore/native-lore; inline block | multiline preserved; inline diagnostic + native/reject policy |
| Conditions | `all`, `any`, nesting bound, unknown child | documented three-valued results; no evaluator execution |
| Matching | item/type/default, priority tie, unknown `SWORD` key | deterministic profile; startup validation warning |
| Context | no viewer, preview viewer, PAPI output with MiniMessage tags | no persistent viewer data; preview unsaved; external tags literal |
| Native lore | `#lore#`, `#native-lore#`, native mode conflict | custom description distinct; duplicate combination rejected |
| Reload | valid -> invalid -> valid | last-known-good remains active during invalid snapshot |

## Success criteria

- `#attack-damage#     #attack-speed#` produces one line with all configured literal spaces when both fragments exist.
- Structured rows remove absent parts and their join separators without hiding a numeric zero.
- Block fragments always remain complete multiline output; no implicit flattening.
- `#lore#` never expands to the complete baseline, and duplicate native insertion is rejected.
- A persistent item produces the same output without a viewer; PAPI/permission data appears only in an unsaved preview with an explicit viewer.
- MiniMessage/legacy conversion occurs exactly once, verified by a tag-injection regression test.
- Invalid profiles leave the last-known-good registry and unmatched items retain native lore.
- Cached compilation meets the plan p95 target once Phase 6 runs the server benchmark.

## Risk assessment and rollback

- **High likelihood / high impact — provider state conflation.** Mitigation: closed result type, state matrix tests, and no visible-text numeric parsing. Rollback: disable raw `:value` adapters while retaining fragment mode.
- **Medium / high — block token silently flattened or duplicated.** Mitigation: AST node type and compile-time placement rejection; explicit `#native-lore#` conflict check. Rollback: set `invalid-inline-block: native` and disable the affected profile.
- **Medium / high — viewer-specific output persisted.** Mitigation: context-free profile selection for writes; PAPI/permission diagnostics when no viewer. Rollback: disable PAPI/permission tokens for persistent operations and reformat from native baseline.
- **Medium / medium — MiniMessage double conversion/injection.** Mitigation: representation metadata and exactly-once boundary test. Rollback: switch profile to `literal` mode and keep raw components.
- **Low / high — invalid reload replaces good config.** Mitigation: compile off-thread if safe, activate atomically only after validation. Rollback: retain the previous immutable snapshot; no item mutation occurs on failed reload.

## Dependencies and ownership

Phase 1 fixes dependency versions and namespace. Phase 3 supplies `LoreBuildContext`, baseline snapshot, and provider interfaces; Phase 2 may use fakes before integration. Phase 4 consumes the renderer and diagnostics but does not alter these files. Phase 5 extends provider/column contracts through additive interfaces; it must preserve the v1 DSL and cannot change the semantics above without a schema version bump. Phase 6 owns final matrix validation and release packaging.



