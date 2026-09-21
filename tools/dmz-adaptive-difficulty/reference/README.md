# Live ki/stamina reference

`build.sh` overlays `DmzResourcePoolClamp.class` from here when a fresh compile differs from production recycle **2.4.115** bytes.

Generate locally:

```bash
unzip -p /path/to/recycle/LegacyMechanics-2.4.115.jar \
  com/dbzlegacy/adaptivedifficulty/progression/DmzResourcePoolClamp.class \
  > tools/dmz-adaptive-difficulty/reference/DmzResourcePoolClamp.class
```

Or set `LM_REFERENCE_JAR` to that recycle jar before `build.sh` (auto-extract if this file is missing).
