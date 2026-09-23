---
phase: 4
title: "Commands, async repair, and operations"
status: completed
---

# Phase 4: Commands, async repair, and operations

## Context links

- [Plan overview](./plan.md)
- [MMOItems API README](https://gitlab.com/phoenix-dvpmt/mmoitems/-/blob/master/README.md)

## Overview

- Priority: P1
- Status: Implemented
- Goal: make the formatter operable on a live server and provide a bounded fallback for items produced asynchronously.

## Requirements

- `/cml reload`: reload and validate `config.yml` and `formats.yml` atomically.
- `/cml validate`: print errors/warnings without changing active configuration.
- `/cml debug item`: show MMOItems type/ID, lore format, matched profile, tokens, hook mode, and fallback reason.
- `/cml preview <profile>`: show a preview for the item in hand where a player context exists.
- `/cml reformat hand|inventory|player <name>`: rebuild selected lore synchronously with permission checks.
- Optional repair listeners for inventory open/click, item pickup, crafting result, equipment changes, and join; disabled by default or rate-limited.
- Metrics/debug counters for formatted, skipped, fallback, and failed items.
- Player-specific PAPI placeholders are prohibited for persistent item lore. Preview may resolve them with the viewing player in memory, but the result is never written to a shared item; a future per-view display layer would be a separate feature.
- Reformat existing stacks only on the main thread. Use `LiveMMOItem(ItemStack)` as a read/capture context and write back to the same stack; do not regenerate from the template by type/ID.

## Architecture

Commands depend on a service interface, not Bukkit listeners directly:

```text
Command -> ReformatService -> ItemIdentityResolver
                         -> LoreFormatter
                         -> safe ItemMeta write
```

For an already-created stack, read it with MMOItems' `LiveMMOItem(ItemStack)` when histories and upgrades are needed. Use that object only as a read/capture context, then write the composed lore back to the same stack's `ItemMeta`. Never call `MMOItems.plugin.getMMOItem(type, id)` for a reformat command: that creates a new template instance, may reroll random data, and can lose item-specific state. Bulk operations must retain the original stack reference/amount and only replace lore after the full render succeeds.

Repair mode must mark processed items with a namespaced persistent-data flag containing the formatter config version and profile ID. It must still re-check dynamic items when the item changes; do not rely on a permanent “already formatted” flag alone.

## Related files

Create:

- `/src/main/java/<namespace>/command/CustomMoreLoreCommand.java`
- `/src/main/java/<namespace>/service/ReformatService.java`
- `/src/main/java/<namespace>/service/RepairService.java`
- `/src/main/java/<namespace>/listener/RepairListeners.java`
- `/src/main/resources/messages.yml`
- `/src/main/resources/permissions.yml`

## Implementation steps

1. Add command tree and permission nodes.
2. Implement atomic config reload with last-known-good fallback.
3. Add item identity and profile diagnostics.
4. Implement hand/inventory/player reformat on the main thread only.
5. Add opt-in repair listeners with cooldown and per-tick budget.
6. Preserve item lore when no profile matches or when formatter errors.
7. Add configurable error behavior: `keep-native`, `skip-item`, or `show-debug-marker` (default `keep-native`).

## Success criteria

- A broken profile never replaces valid native lore.
- Admin can identify why an item did not match or why async repair was skipped.
- Bulk reformat cannot freeze the server: process a bounded number per tick and report progress.

## Risks and mitigations

- Repair listeners can miss item creation paths: document them as best-effort, not complete interception.
- Reformatting dynamic lore can race with another plugin: run on main thread and re-read the item immediately before write.
- Permission abuse: restrict bulk commands and never expose item NBT or internal stat data to untrusted players.
- Player-context leakage: do not bake `%player_*%` values into a stack that can move between players; keep unresolved placeholders or require an explicit per-view display layer.
- Live reads are expensive: use them only for an explicit reformat/repair budget, cache compiled layouts, and expose progress/cancellation for inventory-wide jobs.
- MMOItems revision can rewrite lore and may preserve old lines depending on its keep-lore setting: keep a CML marker/version, make replacement idempotent, and test both keep-lore modes.
