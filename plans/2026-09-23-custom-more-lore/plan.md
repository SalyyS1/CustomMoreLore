---
title: "CustomMoreLore MMOItems layout engine"
description: "Add an MMOItems addon that composes stat, ability, lore, MythicLib, and PlaceholderAPI fragments into arbitrary item lore layouts."
status: implemented
priority: P1
effort: 18-26d
branch: uninitialized/custom-more-lore
tags: [feature, backend, api, minecraft, mmoitems]
created: 2026-09-23
---

# CustomMoreLore MMOItems layout engine

## Overview

Build a standalone Paper plugin that lets server admins put multiple MMOItems stats on one line, reorder sections, hide empty lines, and compose MMOItems/ MythicLib/PlaceholderAPI output without editing MMOItems vendor files.

The repository is currently empty and has no README or project documentation, so this plan starts with project bootstrap and an integration spike before the full renderer. MMOItems' native lore format requires each static placeholder to occupy an exact line; `#damage# #speed#` therefore cannot be implemented by configuration alone. The addon must compose the final lore through a build hook.

## Decision summary

- Own configuration: `plugins/CustomMoreLore/formats.yml`; never rewrite MMOItems language files.
- Primary integration: `GenerateLoreEvent` adapter on synchronous builds.
- Version target: one jar for Paper 1.20.x and 1.21.x, compiled only against APIs common to the supported matrix; test at least one 1.20.4/Java 17 server and one 1.20.6 or 1.21.x/Java 21 server. Exact MMOItems/MythicLib builds are pinned after the spike.
- Compatibility boundary: isolate the deprecated event behind `LoreGenerationHook`; detect missing event/API at startup and disable only the affected mode.
- Async support: automatic lazy repair is enabled by default for join/open/item interaction, with explicit `/cml reformat` commands for bulk repair; coverage remains fail-soft because MMOItems' public lore hook is deprecated.
- Rendering default: `#stat-id#` means the MMOItems-rendered fragment, preserving locale, labels, ranges, upgrades, and custom stat formatting.
- The full product scope includes a validated GUI editor, conditions, and explicit raw-value adapters, but these ship only after the core renderer is proven. They must not be implemented by arbitrary scripting, reflection, or NMS.
- Target the exact Paper, Java, MMOItems, and MythicLib versions chosen during Phase 1; MI7 support is a separate compatibility task.

## Phases

| # | Phase | Status | Effort | Link |
|---|---|---|---:|---|
| 1 | Bootstrap and integration spike | Implemented | 2-3d | [phase-01](./phase-01-bootstrap-and-spike.md) |
| 2 | Config model, compiler, and renderer | Implemented | 3-4d | [phase-02](./phase-02-layout-engine.md) |
| 3 | MMOItems hook and fragment capture | Implemented | 4-6d | [phase-03](./phase-03-mmoitems-integration.md) |
| 4 | Commands, async repair, and operations | Implemented | 2-3d | [phase-04](./phase-04-operations-and-fallbacks.md) |
| 5 | Advanced editor, conditions, and value adapters | Implemented | 4-6d | [phase-05](./phase-05-advanced-features.md) |
| 6 | Test matrix, docs, and release packaging | Implemented | 3-4d | [phase-06](./phase-06-validation-and-release.md) |

## Dependencies

- Paper 1.20.x and 1.21.x runtime matrix; Java 17 for pre-1.20.5 servers and Java 21 for 1.20.5+ where required by the server distribution. Confirm exact requirements on the selected Paper builds before release.
- Exact MMOItems and MythicLib versions supported by the first release.
- PlaceholderAPI is optional; MythicLib remains the primary MMOItems placeholder pipeline.
- A test server with representative weapons, armor, abilities, elements, modifiers, gems, random stats, upgrades, and dynamic durability/uses.

Execution order: Phase 1 -> Phases 2 and 3 in parallel after the hook contract is fixed -> Phase 4 -> Phase 5 -> Phase 6.

## Acceptance gates

1. Two or more scalar stats render on one line while retaining MMOItems' normal translated formatting.
2. Empty stat tokens remove the entire line and leave no dangling separator or empty section bar.
3. Block tokens (`#abilities#`, `#elements#`, `#lore#`, `#native-lore#`) cannot silently be used inline; validation warns and applies a documented fallback.
4. Items without a matching CustomMoreLore profile keep their native MMOItems lore unchanged.
5. `/cml reload`, `/cml validate`, `/cml debug item`, and `/cml reformat` provide actionable output.
6. Rebuilding the same item does not duplicate lore, erase MMOItems dynamic lore, or lose NBT/type/ID data.
7. Synchronous build overhead is measured and kept below 1 ms p95 on the target server for cached layouts.
8. GUI edits produce the same validated config schema as hand-written YAML; GUI cannot save a profile that would silently discard an unsupported fragment.
9. Version-specific failures are reported as capability gaps, with native MMOItems lore preserved.

## External references

- [MMOItems lore format docs](https://docs.phoenixdevt.fr/mmoitems/features/item-lore/lore-format.html)
- [MMOItems LoreBuilder source](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/LoreBuilder.java)
- [MMOItems ItemStackBuilder source](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/ItemStackBuilder.java)
- [Deprecated GenerateLoreEvent source](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/event/GenerateLoreEvent.java)
- [MMOItems API README](https://gitlab.com/phoenix-dvpmt/mmoitems/-/blob/master/README.md)
