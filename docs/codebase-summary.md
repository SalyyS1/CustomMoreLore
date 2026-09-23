# Codebase summary

CustomMoreLore is a Java 17 Paper plugin. `CustomMoreLorePlugin` owns the immutable `ProfileRegistry`, wires the MMOItems event hook, and exposes the `/cml` command.

- `config` loads and validates `formats.yml`.
- `layout` compiles YAML into a small AST and renders scalar, block, row, and conditional nodes.
- `integration` adapts MMOItems `GenerateLoreEvent`, `LiveMMOItem`, `ItemStat.whenDisplayed`, scalar stat data, MMOItems lore, and optional PlaceholderAPI preview values.
- `service` handles main-thread reformatting, bounded repair, metrics, and viewer-persistence protection.
- `api` and `provider` expose typed extension providers without allowing providers to mutate item state.
- `editor` manages isolated GUI drafts; saving is delegated to a validated atomic writer.

Native MMOItems lore is the fail-soft baseline. A failed or unsupported render never regenerates an item template or mutates NBT.
