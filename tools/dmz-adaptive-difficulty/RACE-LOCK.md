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
      "displayName": "Ancient Saiyan",
      "prestigeTooltip": 10
    },
    {
      "id": "sento_saiyan",
      "fabledSkill": "Sento Saiyan",
      "displayName": "Sento Saiyan",
      "prestigeTooltip": 1
    }
  ]
}
```

| Field | Meaning |
|-------|---------|
| `id` | DMZ race id (e.g. `ancient_saiyan`) |
| `fabledSkill` | Fabled skill name that permanently unlocks the race (level ≥ 1) |
| `displayName` | Lock title / messages |
| `prestigeTooltip` | Padlock UI hint only (“Requires Prestige N”) — **not** enforced by Java |

## Behavior
- If a finished character is on a listed race **without** the Fabled skill → reset (`dmzstats reset …`)
- Empty / missing file → defaults to Ancient + Sento (written on first boot)
- Master toggle: `enableRaceLock` in `config/legacymechanics.json`
