# CustomMoreLore compatibility matrix

This matrix is the release contract for the current bootstrap.

| Row | Server | Java runtime | API/build target | Status |
|---|---|---:|---|---|
| Legacy | Paper 1.20.4 | 17 | Lowest common Bukkit/Paper API | Build and tests pass |
| Modern | Paper 1.21.8 | 21 | Same common API surface | Build passes |

The phrase “Paper 1.20.x” is not sufficient for release support: server runtime requirements and item APIs change within that line. Add the exact patch/build number used by a server before marking a row supported. Paper's current getting-started documentation lists Java requirements by supported server range; verify the selected historical build as part of the release test.

MMOItems and MythicLib versions are pinned separately because the public MMOItems lore event is deprecated and the API may change before MI7. MMOItems and MythicLib are provided server dependencies; CustomMoreLore must not shade or relocate them. The first release should publish exact tested patch/build numbers, not only the major Minecraft version.

## Compatibility rules

- Compile against the lowest API surface that passes both rows.
- Do not use version-specific NMS, CraftBukkit package names, Paper Dialog APIs, or data-component APIs in the core renderer.
- Keep all MMOItems imports behind `integration/` adapters.
- Use Bukkit inventory APIs for the GUI so the editor does not split by Paper minor version.
- Run the same golden lore fixtures and hook probe on every supported row.
- If the hook or capture capability is missing, keep native MMOItems lore and report the capability in `/cml debug item`.
