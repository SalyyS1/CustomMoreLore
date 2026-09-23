# Deployment guide

1. Build with Java 17 or newer: `mvn clean package`.
2. Copy `target/custom-more-lore-0.1.0-SNAPSHOT.jar` to the server `plugins/` directory.
3. Ensure MMOItems and MythicLib are installed. PlaceholderAPI is optional.
4. Start the server once, then edit `plugins/CustomMoreLore/formats.yml`.
5. Run `/cml validate`, then `/cml reload`.

The Maven build is compiled against Paper 1.20.4 API and uses only common Bukkit/Paper APIs intended to run on Paper 1.20.x and 1.21.x. Verify the selected MMOItems/MythicLib jars on both target server lines before production rollout.

Use `/cml reformat hand` or `/cml reformat inventory` only on the main thread through the command. Async repair is opt-in and rate-limited. Viewer-specific PlaceholderAPI output is preview-only and is never persisted to an item.
