---
phase: 1
title: "Bootstrap and integration spike"
status: completed
---

# Phase 1: Bootstrap and integration spike

## Context links

- [Plan overview](./plan.md)
- [MMOItems ItemStackBuilder](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/ItemStackBuilder.java)
- [MMOItems GenerateLoreEvent](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/event/GenerateLoreEvent.java)

## Overview

- Priority: P1
- Status: Implemented
- Goal: prove the hook can observe a generated item and replace final lore without changing NBT or item generation semantics.

## Key insights

MMOItems applies each stat through `LoreBuilder.insert(path, ...)`, which looks for an exact line `#path#`. Its current `GenerateLoreEvent` is deprecated and is only called on the primary thread; async builds skip it. The spike must make this limitation visible before implementing the DSL.

## Requirements

- Bootstrap a Java/Paper plugin project with reproducible Maven or Gradle build.
- Build one compatibility artifact from the lowest common Paper API surface. Use Java 17 bytecode only if the selected Paper 1.20.x matrix proves it; run the server matrix separately because Paper 1.20.5+ may require Java 21.
- Declare MMOItems-API and MythicLib as provided dependencies.
- Soft-depend on PlaceholderAPI.
- Define a version matrix in `docs/compatibility.md`.
- Add a startup capability report showing detected MMOItems, MythicLib, PlaceholderAPI, and hook classes.
- Register a no-op `GenerateLoreEvent` listener that logs only in debug mode.

## Architecture

Create a small integration boundary instead of importing MMOItems classes throughout the plugin:

```text
CustomMoreLorePlugin
  -> HookRegistry
       -> GenerateLoreHook (sync, primary)
       -> RepairHook (optional, later phase)
  -> FormatRepository
  -> LayoutCompiler
  -> LoreRenderer
```

The rest of the plugin receives a neutral `LoreBuildContext` containing the MMOItem identity, item type, selected lore format, rendered baseline lines, and stat handles.

## Related files

Create:

- `/src/main/java/<namespace>/CustomMoreLorePlugin.java`
- `/src/main/java/<namespace>/integration/HookRegistry.java`
- `/src/main/java/<namespace>/integration/GenerateLoreHook.java`
- `/src/main/resources/plugin.yml`
- `/src/main/resources/config.yml`
- `/docs/compatibility.md`

## Implementation steps

1. Choose the namespace and a two-row matrix: Paper 1.20.4 + Java 17 (legacy row) and Paper 1.20.6 or a supported 1.21.x + Java 21 (modern row). If the user's actual server is another 1.20.x build, add it explicitly instead of treating `1.20.x` as one target.
2. Create the build file and plugin metadata.
3. Add compile-time MMOItems API imports only behind the integration package.
4. Listen to `GenerateLoreEvent` and record item type, ID, selected `LORE_FORMAT`, and cloned snapshots of `getLore()` and `getParsedLore()` in debug mode. Verify list identity and mutation order on the exact runtime; do not assume they are separate raw/parsed snapshots.
5. Generate a test item with a normal MMOItems format and verify the hook can call `setFinalLore`.
6. Create a probe profile containing two exact standalone tokens and test the experimental capture path. Diff the source item's NBT/meta/tags before and after, and test numeric, random/history, modifiers/gems, ability/effect, and tooltip items. This is a P1 gate, not an assumed API guarantee.
7. Generate the same item through an async path if available; document the observed absence of the event.
8. Add a capability flag and one warning per server boot when the sync hook is unavailable.

## Todo

- [ ] Select supported dependency versions.
- [ ] Bootstrap project and plugin metadata.
- [ ] Verify sync event lifecycle.
- [ ] Verify async behavior.
- [ ] Record API assumptions and deprecations.

## Success criteria

- A test item reaches the listener on the target MMOItems build.
- Replacing the final lore does not alter item NBT, display name, or stat data.
- The probe either proves a safe native-fragment path for an allowlisted stat family or records the family as raw/block/native fallback; there is no silent promise of universal fragment capture.
- The plugin disables only unsupported integration modes and leaves MMOItems native behavior intact.

## Risks and mitigations

- Event removal in MI7: isolate the listener and fail soft; later add a new adapter when an official hook exists.
- Async builds: do not block or call Bukkit APIs from async threads; use the repair strategy in Phase 4.
- Wrong dependency scope: keep MMOItems and MythicLib provided so the server remains authoritative.
- Paper API drift: avoid Paper-only APIs introduced after the lowest target, especially newer Dialog/UI APIs; use Bukkit inventory APIs for the editor.
- Missing raw/parsed provenance: clone both event lists before inspection and record the exact behavior in the compatibility report.
