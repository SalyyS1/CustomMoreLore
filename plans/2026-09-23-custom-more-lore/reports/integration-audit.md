# MMOItems integration audit

Date: 2026-09-23

## Evidence

- Current `ItemStackBuilder` applies every stat through `whenApplied`, builds the lore, calls `GenerateLoreEvent` only when `Bukkit.isPrimaryThread()`, then writes the event's final lore to the item. Async builds skip the event.
- `GenerateLoreEvent` is deprecated and its Javadoc says it will be removed; MMOItems has no stable Java event covering both sync and async generation.
- `LoreBuilder.insert(path, ...)` searches for an exact standalone `#path#` line. A line containing `#damage# #speed#` is not a native interpolation expression.
- `ItemStackBuilder` passes `getLore()` and the result of `LoreBuilder.build()` into the event, but the current builder mutates one backing list. The plan must clone both lists before comparing them and must not treat them as independent raw/parsed snapshots.
- `ItemStat.whenApplied` is public but is not a render-only API. Implementations can add NBT tags, future actions, and lore. Replaying it in a temporary builder is therefore an experimental capability, not a safe universal renderer.
- Tooltip texture headers, suffixes, ignored-line counts, and centering are applied during native lore building. Replacing the final lore can bypass that work.
- The MMOItems build path invokes MythicLib's placeholder parser with a null player. Viewer-specific PAPI values must not be persisted into shared item lore.
- `MMOITEMS_DYNAMIC_LORE` is written by MMOItems but the current source comments say it is no longer used for later calculations. It is not a reliable CML state channel.

## Architecture decision

1. Ship a synchronous `GenerateLoreEvent` adapter behind an integration interface.
2. Use native lore as a fallback whenever a profile, stat family, tooltip mode, or version capability is not proven.
3. Make fragment capture a Phase 1/3 spike with NBT/meta/tag diffs. Maintain an allowlist of verified stat families.
4. Use explicit raw adapters for numeric values instead of scraping visible text.
5. Reformat existing stacks through `LiveMMOItem(ItemStack)` as a read context and write lore back to the same stack; never regenerate from template type/ID.
6. Treat async repair as best-effort, main-thread, rate-limited, and opt-in.
7. Avoid NMS, reflection, MMOItems language-file mutation, and shading MMOItems/MythicLib.

## Stop conditions

Pause implementation and choose an upstream MMOItems interpolation patch if the exact supported jars fail any of these gates:

- common numeric/ability/element fragments cannot be captured without rerolling or mutating NBT;
- custom layouts cannot preserve the target tooltip mode;
- Paper 1.20.x and 1.21.x cannot share a safe API surface;
- another plugin's final-lore listener makes the result nondeterministic.

## Sources

- [ItemStackBuilder](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/ItemStackBuilder.java)
- [LoreBuilder](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/item/build/LoreBuilder.java)
- [GenerateLoreEvent](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/event/GenerateLoreEvent.java)
- [ItemStat](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/type/ItemStat.java)
- [DoubleStat](https://gitlab.com/phoenix-dvpmt/mmoitems/-/raw/master/MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/type/DoubleStat.java)
- [MMOItems tooltip docs](https://docs.phoenixdevt.fr/mmoitems/features/item-lore/tooltips.html)
- [PlaceholderAPI usage](https://wiki.placeholderapi.com/developers/using-placeholderapi/)
- [Paper getting started](https://docs.papermc.io/paper/getting-started/)
