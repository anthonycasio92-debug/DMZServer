# Live ki/stamina reference

`build.sh` overlays the **`ki-pool-2.4.115/`** class slice from `LegacyMechanics-2.4.115.jar` (live-known-good pool/HUD/clamp/sync) into every release jar. Java sources are compile stubs only for that stack — **runtime behavior is the 2.4.115 bytecode**, not reimplemented logic.

Ship line bumps `AdaptiveDifficultyMod.VERSION`, `mods.toml`, and `plugin.yml` together.

## Refresh the slice from recycle/live jar

```bash
JAR="${LM_REFERENCE_JAR:-/path/to/recycle/LegacyMechanics-2.4.115.jar}"
REF="tools/dmz-adaptive-difficulty/reference/ki-pool-2.4.115"
# same class list as build.sh overlay — extract with unzip -p "$JAR" <path> > "$REF/<path>"
```

Do not hand-edit `.class` files; re-extract from the reference jar when live ki behavior changes.
