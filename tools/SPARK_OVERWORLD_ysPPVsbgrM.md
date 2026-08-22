# Spark: Overworld lag (ysPPVsbgrM)

Report: https://spark.lucko.me/ysPPVsbgrM

## Health snapshot
| Metric | Value |
|--------|-------|
| TPS (1m / 5m / 15m) | **8.2 / 9.8 / 14.8** |
| MSPT 1m mean / p95 / max | **112 / 692 / 3933 ms** |
| Players | 11 |
| Entities | ~1772 (not the main issue) |
| Heap used | ~15 GB |

## Primary cause: Rival System CNPC ticks

Hot symbols:
1. `noppes.npcs.util.NBTJsonUtil.FillCompound` (dominant)
2. Nashorn `tick` / `ScriptContainer` / `PlayerScriptData`
3. `rpLoadDatabase` -> `rpProcessPlayer` -> `rivalProxTick`
4. `riLoad` -> `riScan` -> `rivalInstinctTick`
5. Secondary: `StatsSyncS2C`, Create block entities, AdaptiveDifficulty

Every Overworld player (~1s) re-read the full rivalry DB from world storeddata
through CNPC NBT JSON conversion — expensive x online players.

## Fix (Rival 4.7.9)
- Cache rivalry DB raw JSON in world tempdata (3s TTL); invalidate on save
- Proximity tick 1s -> 2.5s; Instinct 4s -> 6s
- Empty Rival init; Sparring JVM once-gate; slimmer Global Player list

## Deploy
Replace world `Rival System.js` (4.7.9) + `Sparring Tp System.js` (3.2.11),
reload CNPC with few players online, re-profile.
