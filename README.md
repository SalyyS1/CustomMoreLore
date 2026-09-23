# CustomMoreLore

CustomMoreLore is a Paper addon for MMOItems that composes MMOItems lore fragments into layouts controlled by an external `formats.yml`. It supports putting multiple scalar stats on one line, hiding empty rows, conditionally showing sections, and keeping MMOItems as the source of stat values and item identity.

The plugin targets Paper 1.20.x and 1.21.x through the common Bukkit/Paper API surface. MMOItems remains the source of item identity and stat data; MythicLib and PlaceholderAPI are optional integrations.

## Example

```yaml
version: 1
profiles:
  types:
    SWORD:
      priority: 20
      layout:
        - '<gray>⚔ #attack-damage# <dark_gray>| <aqua>#attack-speed#'
        - '<yellow>✦ #critical-strike-chance#'
        - '#abilities#'
        - '#lore#'
```

`#stat-id#` and `#stat-id:value#` resolve read-only MMOItems stat values; the `:value` form makes scalar intent explicit. `%placeholder%` tokens resolve only for `/cml preview` when PlaceholderAPI is available. `#lore#` is the lore source exposed by the MMOItems adapter; `#native-lore#` is the complete native baseline. Missing fragments and block tokens follow the validation policy in [docs/configuration-design.md](docs/configuration-design.md).

Use structured rows or dynamic columns when separators should disappear with missing stats:

```yaml
layout:
  - columns:
      - token: attack-damage
        render: fragment
        absent: remove
      - token: attack-speed
        render: value
        absent: remove
    gap: ' <dark_gray>| '
```

Profiles may inherit another profile with `extends: profile-id`; item profiles override type profiles, and type profiles override the default. The in-game editor writes drafts atomically with `/cml editor default`, `/cml editor type:SWORD`, or `/cml editor item:SWORD:sunblade`.

## Commands

- `/cml reload`
- `/cml validate`
- `/cml debug item`
- `/cml preview <profile>`
- `/cml editor <profile>`
- `/cml reformat hand|inventory|player <name>`

Persistent item lore is viewer-independent. Player-specific PlaceholderAPI values are preview-only and are never written to shared ItemStacks.

## Compatibility

See [docs/compatibility.md](docs/compatibility.md) and the implementation plan in [plans/2026-09-23-custom-more-lore/plan.md](plans/2026-09-23-custom-more-lore/plan.md). MMOItems' current lore event is synchronous and deprecated, so asynchronous repair is best-effort and native lore remains the fallback when a capability is not proven.
