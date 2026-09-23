# CustomMoreLore configuration design

This document is the source of truth for the `formats.yml` DSL used by CustomMoreLore. The first implementation targets the package namespace `vn.saly.custommorelore` (the namespace remains an assumption until Phase 1 confirms the project coordinates). The renderer consumes MMOItems' already formatted fragments where possible; it does not modify MMOItems language files or evaluate arbitrary code. The implementation keeps the integration boundary small: `LoreBuildContext` supplies immutable item identity, baseline lore, viewer preview state, and a `FragmentProvider`; providers return the typed six-state `layout.ProviderResult`. The compiler and renderer do not depend on Bukkit or MMOItems classes, allowing Paper 1.20/1.21 adapters to be tested with fakes.

## Rendering contract

A format is selected from the immutable configuration snapshot, compiled once, and rendered against a per-build `LoreBuildContext`:

```text
ItemStack + MMOItem metadata + baseline native lore
  -> ProfileSelector (item ID > item type > default; deterministic priority)
  -> CompiledLayout (AST with source locations)
  -> FragmentProviders (MMOItems, custom lore, MythicLib/PAPI, raw adapters)
  -> ConditionEvaluator + RowComposer
  -> MiniMessage/legacy conversion exactly once
  -> final lore lines (or native fallback on an error)
```

A provider returns a typed result, never a nullable string with overloaded meaning. The result states are `PRESENT(text)`, `EMPTY`, `MISSING`, `ZERO(value=0)`, `UNKNOWN(reason)`, or `UNSUPPORTED(provider/version)`. `ZERO` is present and renders as `0`; it must not be hidden by a missing-stat policy. `MISSING` means the item has no such stat. `EMPTY` is an intentionally empty list/fragment (for example, no abilities). `UNKNOWN` means the adapter could not determine the value because context or API data is unavailable. `UNSUPPORTED` means the token is known but this server/version has no adapter. The configured policies handle these states independently:

```yaml
policies:
  missing: hide             # hide | empty | native
  empty: hide               # hide | empty | native
  unknown: native           # hide | empty | native | error
  unsupported: native       # hide | empty | native | error
  invalid-inline-block: native # native | reject-profile
  condition-unknown: hide   # hide | keep-native
```

`native` means retain the affected baseline line/profile fallback, never a guessed value. A profile that fails compilation is not installed; reload keeps the last-known-good snapshot.

## Profile matching and persistence

Persistent item lore cannot depend on a viewer permission or a player-specific PlaceholderAPI value. Profile matching therefore has a deterministic, context-free chain:

1. exact item ID override (`profiles.items.<id>`),
2. exact MMOItems item type (`profiles.types.<type>`),
3. `profiles.default`.

Within one bucket, higher `priority` wins; ties are broken by profile ID in ascending lexical order. The `type` key is an MMOItems type key such as `SWORD`, `AXE`, `ARMOR`, or a server-defined type. `WEAPON` must not be assumed to exist; startup validation checks the installed MMOItems type registry and reports unknown keys. A missing item ID/type never falls through to a permission-only profile.

A `permission` matcher is allowed only for an explicit unsaved player preview (`/cml preview`) and other commands that provide a viewer context. It is ignored for persistent build/reformat operations, and the debug output states that it was skipped. A saved ItemStack must therefore render identically for every viewer. PlaceholderAPI values follow the same rule: without a viewer, `%player_name%` and similar placeholders resolve to `UNKNOWN` and are not persisted. A player-context preview may resolve them in memory and must never write the preview result to the item.

Example profile structure:

```yaml
version: 1
profiles:
  default:
    priority: 0
    layout: []
  types:
    SWORD:
      priority: 20
      layout: []
  items:
    rpg:sunblade:
      priority: 100
      layout: []
```

Use `extends: profile-id` (or the backward-compatible `parent:` key) to inherit a profile in the same bucket. Child scalar fields replace parent fields; rows with an explicit `id` replace the matching parent row.

## Token forms and aliases

Token names are exact, case-sensitive canonical IDs. Aliases are explicit and validated for collisions; there is no fuzzy matching:

```yaml
aliases:
  damage: attack-damage
  speed: attack-speed
```

The following forms are distinct:

- `#attack-damage#` requests an MMOItems-rendered scalar fragment, preserving the server's label, locale, color, range, upgrade, modifier, and gem formatting.
- `#attack-damage:value#` requests a numeric value from a registered raw adapter. It is an error/`UNSUPPORTED` result when no adapter exists; visible-text parsing is never a fallback.
- `#abilities#`, `#elements#`, `#effects#`, and `#lore#` are block tokens and emit zero or more complete lines.
- `#native-lore#` explicitly emits the complete baseline MMOItems lore captured before CustomMoreLore composition.
- `%placeholder%` is a PlaceholderAPI token when PAPI is installed and a viewer is supplied. MythicLib placeholder syntax is also attempted when MythicLib is present; without a viewer the result is `UNKNOWN`.

`#lore#` means the MMOItems custom description/lore fragment only, excluding generated stat/ability/element lines. It is not an alias for the complete baseline. To avoid duplicate native lore, a profile may either set `native-lore: prepend|append` or use `#native-lore#`, but not both; validation rejects the combination. `native-lore: replace` (default for a matching profile) emits only configured lines. Unmatched items remain untouched by returning their original lore list.

## Rows, literal spacing, and blocks

The string shorthand preserves every literal character, including multiple spaces:

```yaml
layout:
  - '<gray>#attack-damage#     #attack-speed#'
```

Those spaces are not column alignment in Minecraft's proportional font; they are emitted exactly as configured. Missing fragments in shorthand become empty text, so an administrator should use structured rows when separators must disappear automatically.

Structured rows distinguish content parts from the join separator:

```yaml
layout:
  - row:
      parts:
        - text: '<gray>⚔ '
        - token: attack-damage
          render: fragment
        - token: attack-speed
          render: fragment
      join: ' <dark_gray>| '
      absent: remove-part
      empty: remove-row
```

`absent: remove-part` removes a part whose result is `MISSING`/`EMPTY` or was hidden by an explicit policy, then joins the remaining parts with `join`, preventing dangling separators. `absent: keep-empty` preserves the configured part position. An `UNKNOWN`/`UNSUPPORTED` result with `native` or `error` policy keeps the affected row/native fallback and is not silently compacted. A row is removed only when its resolved parts contain no visible content (`empty: remove-row`); a numeric zero is visible content. Structured `parts` are the supported way to create dynamic columns. Arbitrary space padding and width assumptions are not used.

The `columns` shorthand is useful for stat grids and applies the same absent-item policy to each token:

```yaml
- columns:
    - token: attack-damage
      render: fragment
      absent: remove
    - token: attack-speed
      render: value
      absent: remove
  gap: ' <dark_gray>| '
```

`render: value` requests a numeric/raw adapter while `render: fragment` keeps MMOItems formatting. If every column is missing, the row is hidden.

Block tokens are line-producing nodes and must be the sole semantic content of a line or section. They cannot be flattened into an inline string, concatenated with a scalar, or silently joined with a separator. Inline use is a compile error; with `invalid-inline-block: native`, the affected baseline line is retained and a rate-limited warning identifies the profile and source line. A block section can be conditionally omitted but always preserves the provider's multiline output.

## Conditions and optional sections

Conditions use a small allowlisted grammar; there is no expression evaluator, scripting, JavaScript, reflection, or `eval`:

```yaml
- section:
    when:
      all:
        - has: attack-damage
        - any:
            - has: attack-speed
            - has: attack-range
    lines:
      - row:
          parts:
            - token: attack-damage
            - token: attack-speed
          join: ' | '
```

Supported predicates are `has:<token>`, `type:<MMOItemsType>`, `id:<item-id>`, `tier:<tier-id>`, `permission:<node>` (preview context only), and numeric comparisons on explicit raw values (`value:<token> >= 10`). `has` is true for a present value including zero, false for `MISSING`/`EMPTY`, and `UNKNOWN` when the provider cannot decide. `all` is false if any child is false, otherwise `UNKNOWN` if any child is unknown, otherwise true. `any` is true if any child is true, otherwise `UNKNOWN` if any child is unknown, otherwise false. `condition-unknown` controls whether an unknown section is hidden or keeps its native counterpart. Conditions must be validated before activation and have a bounded nesting depth.

## Text/color processing

The profile declares one text mode (`minimessage`, `legacy`, or `literal`). Static config text is converted at the final boundary exactly once. Legacy `&`/`§` conversion occurs only in `legacy` mode. MiniMessage tags from static config are parsed only once; provider output, MMOItems fragments, and PAPI values are inserted as already-rendered/literal content and are never reparsed as MiniMessage. This prevents external placeholder data from injecting formatting tags. A fragment carries its representation (`COMPONENT`, `LEGACY_TEXT`, or `PLAIN_TEXT`) so a second conversion cannot occur.

## Validation and diagnostics

Validation reports file path, profile ID, line/column, token, and remediation. Errors include unknown token, alias collision, block token in an inline row, `#token:value#` without a registered adapter, invalid condition grammar, unsupported MMOItems type, both `native-lore` and `#native-lore#`, permission/PAPI use in a persistent operation, and duplicate profile priority tie without deterministic ID ordering. Warnings include proportional-space padding, unknown viewer context, and version-specific provider fallback.

The validator receives separate canonical fragment and raw-value adapter allowlists so a fragment token never implicitly becomes a numeric provider.

`/cml validate` validates a candidate snapshot without activating it. `/cml debug item` reports the selected bucket/profile, each token's result state/provider, condition result, text mode, native-lore mode, and fallback reason. Diagnostics never expose private player data or raw NBT to untrusted users.

## Compatibility and migration

Version `1` keeps the existing shorthand (`lines: ["#token#"]`) valid. A migration tool may convert shorthand to structured rows but must preserve literal spaces. Legacy MMOItems `LORE_FORMAT` remains the source of baseline fragments; no vendor file is rewritten. Unknown v2+ fields are rejected with a clear version error until a future compiler supports them. A failed reload retains the last-known-good compiled snapshot, and disabling CustomMoreLore restores native MMOItems lore without changing item identity or NBT.

