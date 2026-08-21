# forge_scripts.json (CustomNPCs Global Forge Scripts)

This file is the **source export** of three CNPC Global Forge Scripts.

**Runtime replacement:** `kubejs/startup_scripts/shurui_spawner_bridges.js`

After deploying the KubeJS port, set:

```
"ScriptEnabled": 0b
```

(or disable Global Forge Scripts in the CNPC UI) so the old Nashorn hooks do not run alongside KubeJS.

See `tools/SHURUI_SPAWNER_BRIDGES_KUBEJS.md`.
