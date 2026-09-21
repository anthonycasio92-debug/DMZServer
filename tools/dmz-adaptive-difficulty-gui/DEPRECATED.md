# LegacyMechanicsGUI — deprecated

**Legacy Mechanics 4.5.3+** ships the full player UI in the **Forge mod** via CustomNPCs (`gui/cnpc/*`).

This Bukkit plugin is **not required** for:

- `/lm` hub and system menus (CNPC)
- `/lm admin testgui`
- Staff inspect (Forge `ForgeInspectGui` + `AdminInspectSessions`)

## When to remove

After live smoke tests with **only** `LegacyMechanics-4.5.3.jar`:

1. Remove `plugins/LegacyMechanicsGUI-*.jar`.
2. Ensure Mohist routes `/lm` and related commands to Forge (see `tools/dmz-adaptive-difficulty/FORGE-GUI-ONLY.md`).

## What you lose without the plugin

- CMI/chest inventory GUIs and `/lmdo` click bridge
- PlaceholderAPI expansion bundled here (`DmzDiffExpansion`)
- Bukkit `DeathDropGuard` listener (Forge claim guard remains)

## Rollback

Reinstall matching-version `LegacyMechanicsGUI` jar and set `guiBackend` to `cmi` or `chest` in config.
