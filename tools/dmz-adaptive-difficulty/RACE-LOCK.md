# Race Lock config

Edit **`config/legacymechanics/race-lock.json`** to add or change restricted races — **no mod rebuild**.

Reload with `/difficulty reload` (or restart). KubeJS padlock sync re-reads the file every ~15s (or `/kubejs reload server_scripts`).

## Format

```json
{
  "restricted": [
    {
      "id": "ancient_saiyan",
      "fabledSkill": "Ancient Saiyan",
      "displayName": "Ancient Saiyan"
    },
    {
      "id": "sento_saiyan",
      "fabledSkill": "Sento Saiyan",
      "displayName": "Sento Saiyan"
    }
  ]
}
```

| Field | Meaning |
|-------|---------|
| `id` | DMZ race id (e.g. `ancient_saiyan`) |
| `fabledSkill` | Fabled skill name that permanently unlocks the race (level ≥ 1) |
| `displayName` | Lock title / messages |

The padlock’s “Requires Prestige N” hint is not in this file. KubeJS `race_lock_gui_sync.js` still sends that number for the race-select UI. The Java lock does not read it.

## Behavior
- If a finished character is on a listed race **without** the Fabled skill → reset (`dmzstats reset …`)
- Empty / missing file → defaults to Ancient + Sento (written on first boot)
- Master toggle: `enableRaceLock` in `config/legacymechanics.json`
