---
phase: 3
title: "MMOItems hook and fragment capture"
status: completed
---

# Phase 3: MMOItems hook and fragment capture

## Context links

- [Plan overview](./plan.md)
- [ItemStackBuilder source](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/ItemStackBuilder.java)
- [GenerateLoreEvent source](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/event/GenerateLoreEvent.java)
- [MMOItems new stat API](https://docs.phoenixdevt.fr/mmoitems/api/new-stats)

## Overview

- Priority: P1
- Status: Implemented
- Goal: obtain trustworthy stat/ability/lore fragments while preserving MMOItems' own formatting and side effects.

## Key insights

Listening to `ItemBuildEvent` alone is too late to identify which rendered line came from which stat. The adapter must use the `MMOItem` available from `GenerateLoreEvent` and a controlled, capability-tested capture strategy. The event's `getLore()` and `getParsedLore()` may alias the same mutable list, and the final lore has already passed placeholder/tooltip processing. It must never mutate the user's original item or re-run random generation.

## Requirements

- Read MMOItem type, ID, `ItemStats.LORE_FORMAT`, tier, and stat set from public API.
- Preserve MMOItems translations, stat history, upgrades, modifiers, gems, abilities, elements, and dynamic placeholders.
- Compose only the final lore; do not edit NBT, name, attribute modifiers, or MMOItems tags.
- Support existing MMOItems custom description as `#lore#` and the complete native baseline as `#native-lore#`; append/replace behavior is controlled by the profile's native-lore mode.
- Ensure the formatter is rerun after MMOItems revision/update rebuilds.
- Register the event listener at `HIGHEST` and never at `MONITOR`; document conflicts with other plugins that also replace final lore.
- Treat tooltip-texture items as a separate capability: either preserve native lore, implement and test a rebake using the builder's experimental tooltip API, or require `tooltip: native-only` for the profile.

## Adapter design

```text
GenerateLoreEvent
  -> MmoItemContextFactory
  -> BaselineLoreSnapshot
  -> StatFragmentCapture
  -> LoreBuildContext
  -> CompiledLayout.render()
  -> event.setFinalLore(finalLore)
```

`StatFragmentCapture` should prefer public APIs and a temporary builder/capture format, but native-fragment capture is experimental. The spike must verify whether a cloned `MMOItem` plus `ItemStackBuilder.getLore().setLore(standaloneTokens)` followed by stat `whenApplied` calls is safe for each supported stat family. It must prove that random rolls, stat histories, upgrades, gems, tags, future actions, tooltip handling, and the source event item are unchanged. If any stat is not safe to replay, it is raw/block/native fallback with a warning; the renderer must never reroll an item merely to obtain a fragment.

Do not use reflection, NMS, or runtime edits to MMOItems' language files. Do not reload MMOItems for every item. If a future MMOItems release exposes a pre-build interpolation hook, implement a new adapter behind the same interface.

## Related files

Create:

- `/src/main/java/<namespace>/integration/MmoItemsAdapter.java`
- `/src/main/java/<namespace>/integration/MmoItemContextFactory.java`
- `/src/main/java/<namespace>/integration/StatFragmentCapture.java`
- `/src/main/java/<namespace>/integration/BaselineLoreSnapshot.java`
- `/src/main/java/<namespace>/integration/GenerateLoreHook.java`
- `/src/test/java/<namespace>/integration/*`

## Implementation steps

1. Create a neutral context from `GenerateLoreEvent.getItem()` and its builder.
2. Determine profile identity from custom match rules and the item's public `LORE_FORMAT` data.
3. Capture baseline lore before replacement for fallback and `#native-lore#`; resolve `#lore#` from the MMOItems custom-lore stat/provider only.
4. Implement scalar capture for common numeric/string stats only after the probe proves value preservation.
5. Implement block capture for abilities, elements, effects, and custom item lore. Keep `#lore#` reserved for the custom description and `#native-lore#` reserved for the complete baseline; reject combinations that would duplicate the baseline.
6. Resolve dynamic placeholders after composition using the same pipeline order as MMOItems.
7. Call `setFinalLore` only after the full line list is validated.
8. Add recursion protection so a rebuild or repair cannot trigger an infinite formatter loop.
9. Log one warning per item profile, not one warning per token on every build.
10. Add an explicit spike exit gate: if safe fragment capture cannot be proven for a stat family, do not expose that family as inline; keep native/block output or stop for an upstream MMOItems interpolation patch.
11. Preserve unresolved dynamic placeholders and do not bake player-specific PAPI values into persistent item lore. MythicLib's native parser receives a null player in this build path.

## Success criteria

- The sample compact weapon profile renders attack damage and attack speed on one line.
- Ability/effect lists preserve their full multiline output.
- Rebuilding an upgraded/gemmed item produces the same stat values as native MMOItems.
- No custom lore is applied twice when another plugin fires a rebuild.

## Risks and mitigations

- `GenerateLoreEvent` is deprecated: capability check, adapter isolation, exact-version tests, and a fail-soft native fallback.
- Async builds skip the event: Phase 4 repair mode and explicit commands.
- Complex custom stats may depend on other registered placeholders: capture them as block fragments or preserve native output until an adapter is added.
- MMOItems API changes in MI7: keep all imports in this package and pin CI/test dependencies.
- Replaying `whenApplied` may be unsafe for an untested stat: maintain an allowlist of verified stat families and expose a native/block fallback instead of guessing.
- Another HIGHEST listener can overwrite the final lore: expose listener priority/configuration and preserve a native fallback if the final list changes unexpectedly.
- Tooltip injection is performed during the native build: replacing final lines can bypass texture headers, suffixes, centering, or ignored-line counts. Gate this explicitly and test it before enabling custom layouts on tooltip items.
