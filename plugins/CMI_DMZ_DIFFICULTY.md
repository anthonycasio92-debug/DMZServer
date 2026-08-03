# CMI GUI for Adaptive Difficulty

Requires:
- CMILib (shipped as CMILib1.5.9.6.jar)
- CMI
- dmz_adaptive_difficulty_gui-3.3.44.jar (or newer matching the Forge mod)

The companion plugin registers Bukkit `/difficulty` and opens a CMILib inventory GUI.
Fallback order: CMI → Bukkit chest → clickable chat.

If `/difficulty` opens chat instead of an inventory:
1. Confirm both jars above are installed and the server was fully restarted
2. Confirm CMILib + CMI are enabled (`/plugins` should list CMILib, CMI, DMZAdaptiveDifficultyGUI)
3. Config `guiBackend` should be `cmi` (or `auto`) in `config/dmz_adaptive_difficulty.json`
4. Force inventory: `/dmzdiffgui`
