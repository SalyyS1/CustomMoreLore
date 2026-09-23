---
phase: 5
title: "GUI editor, raw stat adapters, columns, and extension API"
status: completed
priority: P1
effort: "4-6d"
dependencies: [phase-02-layout-engine, phase-03-mmoitems-integration, phase-04-operations-and-fallbacks]
---

# Phase 5: GUI editor, raw stat adapters, columns, and extension API

## Overview

Deliver the requested advanced surface as a scheduled product phase before the release-validation phase: an admin layout editor, explicit numeric/raw stat adapters, conditional-row editing, column helpers, locale/inheritance support, and a small public fragment-provider API. This phase is pending until the core renderer and live MMOItems hook are stable, but it is not an indefinite “maybe later” feature or gated on an arbitrary number of users.

The phase extends the v1 DSL additively. Existing shorthand, fragment tokens, profile matching, viewerless persistence, block-only lines, state policies, and exactly-once text conversion remain unchanged. Any incompatible grammar requires `version: 2` and a migration report.

## Requirements

- Provide an inventory GUI/editor using stable Bukkit APIs available on Paper 1.20.x and 1.21.x; do not depend on a newer UI API or a client mod.
- Let an admin select an item/profile, add/reorder rows, insert scalar/block tokens, choose render mode, define join/absence policies, edit conditions, and preview validation errors before saving.
- Save only through the same atomic config service used by `/cml reload`; never write half a profile or persist a player-context preview.
- Add explicit raw stat providers with `value`, optional `label`, `unit`, `icon`, sign, precision, rounding, and percentage rules. A raw adapter must declare support and return typed `ProviderResult`; it must not scrape rendered lore.
- Add optional columns/row templates with explicit separator and alignment intent. Document that width is glyph/font dependent and cannot guarantee pixel-perfect Minecraft alignment.
- Add locale/profile inheritance and conflict validation while keeping item > type > default matching deterministic.
- Expose a versioned public provider/column registration API under `vn.saly.custommorelore.api`; registration occurs during plugin enable and is immutable during a render.
- Keep arbitrary scripting, JavaScript, reflection-based stat discovery, NMS patching, and viewer-dependent persistent lore outside the boundary.

## Data flow

```text
Admin GUI (main thread, draft only)
  -> EditorSession (admin-scoped, timeout, no ItemStack writes)
  -> LayoutDraft + ValidationService
  -> PreviewService (explicit viewer context; unsaved)
  -> AtomicConfigWriter (temp file + fsync/rename)
  -> FormatConfigLoader -> validator/compiler -> last-known-good registry

Raw MMOItems/extension provider registration
  -> ProviderRegistry (enable-time immutable snapshot)
  -> FragmentResolver(value/fragment/block)
  -> Column/RowComposer -> exactly-once text boundary -> final lore
```

A GUI session stores only a draft profile and admin identity; it expires and is never shared globally. The preview renderer receives a copied item/context and returns components without mutating the held item or saving viewer-specific PAPI values. A save writes a complete candidate file, validates/compiles it, then atomically swaps the active registry. If validation fails, the old file and registry remain active.

## GUI/editor design

Use a paged Bukkit inventory with click handlers and explicit state transitions:

1. Profile screen: choose default/type/item bucket; display winning profile and priority.
2. Row screen: add text, fragment token, raw value token, block section, or conditional section; reorder and remove.
3. Part screen: edit token/alias, render mode, join separator, absent/empty policy, and condition predicates.
4. Validation screen: source-like error list with profile, row, token, and remediation; save is disabled while errors exist.
5. Preview screen: show the item in hand with selected profile and optional player context; mark preview as unsaved.

Handlers must verify the session owner, inventory title/session ID, click type, and page bounds. Close, quit, reload, or timeout discards drafts. GUI messages are translatable and use the same permission nodes as commands. The editor can open a text editor command for long MiniMessage strings; it must not attempt to serialize arbitrary inventory NBT.

## Raw providers and columns

Raw adapters are explicit implementations of a public SPI:

```java
public interface FragmentProvider {
    String id();
    Capability capability(); // FRAGMENT, VALUE, BLOCK
    boolean supports(ProviderRequest request);
    ProviderResult resolve(ProviderRequest request);
}
```

`ProviderResult` is the Phase 2 typed state model. Built-ins cover only stat IDs exposed by the pinned MMOItems API and MythicLib; providers report `UNSUPPORTED` instead of guessing. Third-party providers register before the first render and are rejected on duplicate IDs. A provider must be pure for one request: no item mutation, network calls, or global mutable state.

Value formatting is declarative:

```yaml
value-format:
  precision: 2
  rounding: half-up
  sign: always
  unit: '%'
  icon: '<gold>⚔'
```

Columns are explicit row templates, for example `columns: [{token: attack-damage, align: right}, {token: attack-speed, align: right}]` with `gap: ' <dark_gray>| '`. Alignment operates on logical text/glyph count only; no promise of pixel width is made. A missing column is removed according to the row policy; zero remains visible. A block token is rejected in a column.

Locale and inheritance are resolved at compile time: child profile fields override parent fields by key, rows replace by explicit row ID (never implicit index merge), and cycles/unknown parents are errors. Final profile precedence remains item > type > default, then priority and ID tie-break.

## Related code files

Create:

- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/api/FragmentProvider.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/api/ProviderRequest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/api/ProviderResult.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/api/ColumnSpec.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/api/CustomMoreLoreProviderEvent.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/provider/ProviderRegistry.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/provider/BuiltinValueProviders.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/editor/EditorSession.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/editor/LayoutEditorMenu.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/editor/EditorDraftService.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/editor/EditorValidationService.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/config/ProfileInheritanceResolver.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/java/vn/saly/custommorelore/config/AtomicConfigWriter.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/main/resources/editor-messages.yml`

Tests:

- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/api/ProviderRegistryTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/editor/EditorValidationServiceTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/config/ProfileInheritanceResolverTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/java/vn/saly/custommorelore/editor/LayoutEditorMenuTest.java`
- `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/src/test/resources/fixtures/advanced-formats.yml`

## Implementation steps

1. Freeze Phase 2 provider-result/token interfaces and add compatibility tests before extending them.
2. Implement the provider SPI, enable-time registry, duplicate/capability validation, and built-in raw adapters for the supported MMOItems/MythicLib versions.
3. Implement declarative value formatting (precision, rounding, sign, unit, icon, percentage) with locale-aware decimal rules and no rendered-text parsing.
4. Implement structured columns/row templates and reject block tokens in a column at compile time.
5. Implement inheritance and locale overlays with cycle detection and row-ID replacement; test deterministic profile precedence after inheritance.
6. Implement session-scoped editor drafts and paged stable-Bukkit GUI screens. Add owner/session validation, timeout, close/quit cleanup, and no ItemStack mutation.
7. Integrate editor validation, unsaved preview, and atomic save with last-known-good reload behavior.
8. Document provider registration, column limitations, GUI permissions, migration, and rollback in `D:/Project/.1_PROJECT_SL_PLUGINS/CustomMoreLore/docs/configuration-design.md` and operator docs owned by Phase 6.
9. Run Paper 1.20.x and 1.21.x manual GUI tests and integration tests before Phase 6 release packaging.

## Test matrix

| Area | Cases | Expected |
|---|---|---|
| Provider SPI | built-in value, unsupported version, duplicate ID, provider exception | typed result; one warning; no render crash |
| Numeric format | zero, negative, percentage, precision/rounding, locale decimal | deterministic output; no text scraping |
| Columns | all present, middle missing, zero, block token, proportional glyphs | join/absence policy; block rejected; documented width limit |
| Inheritance | parent override, row-ID replacement, missing parent, cycle | deterministic merge; cycle/error keeps old config |
| GUI security | foreign inventory click, stale session, timeout, quit/reload | ignored/closed; no unauthorized save |
| GUI editing | add/reorder/delete/invalid token/condition | draft updates; save disabled on errors |
| Preview/persistence | PAPI with viewer, no viewer, save then reopen | viewer data preview-only; persisted output viewer-independent |
| Atomic save | disk error, invalid generated YAML, concurrent reload | old file/registry remains; actionable error |
| Compatibility | Paper 1.20.x and 1.21.x, Java target/build JDK | no newer UI API dependency |

## Success criteria

- An admin can create a profile with two scalar stats on one row, a conditional section, and a block ability list through the GUI; validation identifies errors before save.
- A registered raw adapter produces `value` output with documented rounding/sign/unit behavior, while unsupported adapters render native fallback.
- Item/type/default profile precedence remains deterministic after inheritance and locale overlays.
- A GUI preview can resolve viewer PAPI values in memory, but no viewer-specific value is written to an ItemStack or config.
- Columns compact missing parts, retain zero, reject block tokens, and state the proportional-font limitation.
- Provider registration and editor sessions are covered by unit tests; GUI flows pass on both supported Paper lines.
- A failed GUI save or reload leaves the previous config and active registry intact; disabling Phase 5 features still leaves Phase 2/3 fragment formatting operational.

## Risk assessment and rollback

- **High / high — GUI writes malformed or partial configuration.** Mitigation: draft-only sessions, schema validation, temp-file write + atomic rename, last-known-good registry. Rollback: disable editor command and continue file-based configuration.
- **Medium / high — raw provider returns incorrect values or mutates items.** Mitigation: explicit capability/support checks, pure request contract, timeout-free local providers, exception isolation, golden values. Rollback: unregister provider ID and force `fragment`/native mode.
- **Medium / high — API/plugin version incompatibility.** Mitigation: stable Bukkit inventory APIs, pinned compile matrix, additive API version, no reflection/NMS. Rollback: disable advanced provider/editor modules while core renderer remains enabled.
- **Medium / medium — inheritance creates surprising profile selection.** Mitigation: explicit row IDs, cycle detection, deterministic precedence diagnostics. Rollback: set inheritance off and flatten generated profiles with migration tool.
- **Medium / medium — column alignment misleads administrators.** Mitigation: document glyph/font limits, require explicit gap/alignment, preview on target resource pack. Rollback: use separators and ordinary rows.
- **Low / high — viewer data leaks into persistent lore.** Mitigation: viewer context is mandatory for preview and prohibited for writes; tests compare output with/without viewer. Rollback: invalidate viewer-dependent token nodes and reformat native lore.

## Dependencies, backwards compatibility, and ownership

Phase 2 owns token/state semantics and must approve additive API changes. Phase 3 owns MMOItems baseline/provider implementation. Phase 4 owns commands and repair listeners; it consumes editor save/preview services without changing hook behavior. Phase 6 owns release docs and compatibility packaging after this phase passes. Existing v1 files and shorthand remain readable; editor-generated files use the same schema. Unrecognized advanced keys fail validation with a migration hint rather than being silently ignored. Removing Phase 5 code leaves native MMOItems lore and Phase 2 fragment layouts usable.

