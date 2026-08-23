# Spark Overworld — 0QA559wEa7

URL: https://spark.lucko.me/0QA559wEa7

## Snapshot

| Metric | Value |
|--------|-------|
| Players | 8 |
| TPS (1m / 5m / 15m) | ~19.3 / ~7.4 / ~10.4 |
| MSPT 1m mean / median / p95 / max | ~46 / ~42 / ~79 / ~158 ms |
| MSPT 5m mean / max | ~124 ms / **~20.2 s** (one catastrophic spike) |
| Sampler | Overworld @ 4 kHz |

Compared to ysPPVsbgrM (TPS ~8, MSPT p95 ~700ms): **much better** after Rival 4.7.9 raw-string cache — `NBTJsonUtil.FillCompound` is gone.

## Still hot (this sampler)

| Rank | Share of tick | Cause |
|------|---------------|--------|
| 1 | ~12%+ | Nashorn `JSONParser` / `JSON.parse` of rivalry DB |
| 2 | | `rpLoadDatabase` → still parsing every `rivalProxTick` |
| 3 | | `rivalProxTick` / `riScan` |
| 4 | secondary | `StatsSyncS2C` / DMZ packets |

Root cause: 4.7.9 caches the **string** for 3s but **every player engine still `JSON.parse`s** that string on each prox/instinct load. With many players, that is still expensive.

## Fix shipped in repo (Rival 4.7.10)

1. **`rivalDbParseCached()`** — memoize parsed object until `rpDbEpoch` or string changes.
2. Raw world-tempdata TTL **15s** (was 3s).
3. Prox interval **5s**, instinct **10s**.

Deploy `customnpcs/scripts/Rival System.js` to the **world** scripts folder and `/noppes script reload` (few players online).

## Not primary this time

- Ki / End Dimension Strength — not visible as top Overworld cost.
- Sparring boot spam — log noise, not this MSPT profile.
