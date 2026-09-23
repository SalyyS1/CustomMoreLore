---
phase: 6
title: "Test matrix, documentation, and release packaging"
status: completed
priority: P1
effort: "3-4d"
dependencies: [phase-01-bootstrap-and-spike, phase-02-layout-engine, phase-03-mmoitems-integration, phase-04-operations-and-fallbacks, phase-05-advanced-features]
---

# Phase 6: Test matrix, docs, and release packaging

## Context links

- [Plan overview](./plan.md)
- [MMOItems lore format docs](https://docs.phoenixdevt.fr/mmoitems/features/item-lore/lore-format.html)

## Overview

- Priority: P1
- Status: Implemented (unit/package gates green; live Paper matrix remains an operator validation step)
- Goal: prove compatibility, performance, failure handling, and migration behavior before publishing a server-facing build.

## Test matrix

Cover at least:

- Weapon, armor, tool, accessory, gem stone, and consumable types.
- Plain numeric, string, boolean, elemental, ability, effect, modifier, and random stats.
- Upgrades, gems, item levels, tiers, requirements, soulbound, durability, and uses-left.
- Native format fallback, custom profile, no profile, invalid profile, and duplicate priority.
- Sync build, async build, inventory repair, command reformat, item revision/update, and reload during normal operation.
- Paper versions selected in Phase 1 and exact MMOItems/MythicLib versions.
- Both version rows from Phase 1, including Java runtime requirements and a single-jar load test.

## Requirements

- Unit tests for YAML schema, compiler, matcher, token classification, and line pruning.
- Integration tests on a disposable Paper server with real MMOItems jars.
- Golden lore snapshots for representative item templates.
- Performance benchmark for cached and uncached profile rendering.
- Manual test checklist for tooltip textures, MiniMessage/legacy colors, PAPI with and without a player, and other lore-mutating plugins.
- A safety test proving capture does not reroll random stats, mutate NBT, duplicate `MMOITEMS_DYNAMIC_LORE`, or lose tooltip/upgrade/gem output.
- A tooltip test proving the chosen policy (`native-only` or verified rebake) for every profile that matches tooltip items.
- A revision test with MMOItems keep-lore enabled and disabled, confirming CML-owned lines are not duplicated or left stale.
- Operator docs with install, config reference, migration, troubleshooting, and compatibility table.

## Related files

Create:

- `/src/test/java/<namespace>/layout/*`
- `/src/test/java/<namespace>/integration/*`
- `/src/test/resources/fixtures/*`
- `/docs/configuration.md`
- `/docs/troubleshooting.md`
- `/docs/compatibility.md`
- `/CHANGELOG.md`
- `/README.md`

## Implementation steps

1. Add deterministic golden snapshots.
2. Run integration tests against the pinned dependency matrix.
3. Measure p50/p95 render cost and memory allocation.
4. Test plugin reload and server restart with invalid and valid files.
5. Verify native lore remains unchanged for unmatched items.
6. Package a release jar with dependency notes and sample `formats.yml`.
7. Publish a known-limitations section, especially deprecated hook and async best-effort repair.

## Success criteria

- All acceptance gates in `plan.md` pass.
- No high-severity data-loss, NBT mutation, or infinite-recursion issue remains.
- A new server admin can install and create the sample two-stats-per-line layout from docs alone.

## Risk assessment

- Version drift: pin dependencies and fail CI when the adapter API changes.
- Snapshot brittleness: keep snapshots focused on intended lore, with explicit normalization for color serialization.
- Hidden async gaps: keep repair opt-in and report coverage honestly.
