# Spark (TPS profiler)

Spark samples what the server spends time on so you can see *why* TPS is low.

## Install

Already in this repo:

- `mods/spark-1.10.53-forge.jar` (Forge 1.20.1 — works on Mohist)

Restart the server after adding/updating the jar. Remove any duplicate Spark jars.

## Common commands

Run as OP (or with Spark permission):

| Command | Purpose |
|---------|---------|
| `/spark tps` | Live TPS / MSPT |
| `/spark profiler start` | Begin sampling the main thread |
| `/spark profiler stop` | Stop and open a shareable report URL |
| `/spark health` | Heap / GC / tick overview |
| `/spark activity` | Recent tick spikes |

Typical workflow when TPS is stuck around 6–8:

1. `/spark tps` — confirm MSPT is high
2. `/spark profiler start` — fight / explore for ~30–60 seconds under load
3. `/spark profiler stop` — open the report
4. Sort by **self time** / search for `adaptivedifficulty`, `dragonminez`, `customnpcs`, `cmi`, entity tick

## Isolating adaptive difficulty

1. Note TPS with `dmz_adaptive_difficulty-*.jar` installed
2. Stop server, move that jar out of `mods/`, restart
3. `/spark tps` again under the same load
4. If TPS recovers a lot → this mod (or its combat load) is involved
5. If TPS stays ~6–8 → profile with Spark; cost is elsewhere

## Docs

- https://spark.lucko.me/
- https://spark.lucko.me/docs/Command-Usage
