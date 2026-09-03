# High-tier telemetry retune — live pull 2026-09-03

**Source:** `uploads/live-telemetry-2026-09-03-retune/` (38270 hits, Aug 29–Sep 3)

## Executive summary

- **T5–T7 god-form is running hot** vs concept and vs sim @ 100k DMZ (paintEase veterans).
- **75% of T5 hits exceed soft-cap** (+2%); T6 60%, T7 46%.
- **2.3.170 early signal (200 hits post-deploy):** T5 god @ 100k + ease 0.595 → **0.124 avg** (n=11) vs pre-170 **0.492** — paintEase fix is working; need more sample.
- **T6/T7:** no post-170 god hits yet; pre-170 god avg **0.515 / 0.548** vs sim@100k **0.369 / 0.399**.
- **Ladder flattening:** T5→T6 god step only **1.05×**, T6→T7 **1.06×** (target ≥1.10× for T5→T7 concept).

## Live god-band (formBoost ≥ 25)

| Tier | N | Avg post | Soft-cap | Δ vs cap | Sim@10k | Sim@100k | paintEase@100k |
|-----:|--:|---------:|---------:|---------:|--------:|---------:|---------------:|
| T4 | 3211 | 0.372 | 0.40 | -0.028 | 0.307 | 0.108 | 0.350 |
| T5 | 18621 | 0.492 | 0.44 | +0.052 | 0.338 | 0.201 | 0.595 |
| T6 | 2924 | 0.515 | 0.48 | +0.035 | 0.369 | 0.369 | 0.841 |
| T7 | 1476 | 0.548 | 0.52 | +0.028 | 0.399 | 0.399 | 0.656 |

## Pre vs post 2.3.170 (god, T4–T7)

| Tier | Pre avg | Pre n | Post avg | Post n | Post ease | Post dmz |
|-----:|--------:|------:|---------:|-------:|----------:|---------:|
| T4 | 0.375 | 3181 | 0.045 | 30 | 0.350 | 100,000 |
| T5 | 0.492 | 18610 | 0.124 | 11 | 0.595 | 100,000 |
| T6 | 0.515 | 2924 | 0.000 | 0 | 0.000 | 0 |
| T7 | 0.548 | 1476 | 0.000 | 0 | 0.000 | 0 |

## KP load at T5–T7 (god)

| Tier | KP | N | Avg post |
|-----:|---|--:|---------:|
| T5 | 1-5 | 11329 | 0.517 |
| T5 | 10 | 7292 | 0.453 |
| T6 | 0 | 3 | 0.494 |
| T6 | 1-5 | 1310 | 0.555 |
| T6 | 10 | 1611 | 0.482 |
| T7 | 0 | 1 | 0.585 |
| T7 | 1-5 | 645 | 0.620 |
| T7 | 10 | 830 | 0.492 |

## Retune recommendations (higher tiers)

### 1. Wait for 2.3.170 sample (priority)
Post-deploy T5 @ 100k shows **0.124** avg (n=11) with paintEase **0.595**. Do **not** nerf T5 combat constants yet — the DMZ-level read was the main bug. Re-pull after **24–48h** of god-form T5–T7 traffic.

### 2. If T5 stays hot after 170 (target god ≈ 0.20–0.35 @ 100k)
- Confirm telemetry `dmzLevel` / `paintEase` are populated on hot hits.
- If ease is correct but hits still >0.40: trim **T5 form nudge** (1.10→1.06) or **liveShare** (0.50→0.46).

### 3. T6/T7 ladder flattening
Live god T5→T6 step **1.05×** (want ≥1.10× to T7 concept). Options if still flat after paintEase:
- Raise **T6/T7 landFrac** slightly (more bite via safety-net) OR
- Raise **T6/T7 form nudge** (1.14/1.18 → 1.18/1.22) only if post-170 avg stays <0.45.

### 4. T7 KP under-investment
KP1-5 on T7 god avg **0.620** vs KP10 **0.492** — KP is load-bearing; keep hit-cap relief; avoid global T7 nerfs that collapse KP10 players.

### 5. T4 overshoot (god 0.372 vs sim@100k 0.108)
T4 vet band (5k→10k) may need stronger ease above 50k — same paintEase vet curve as T5+; verify after dmzLevel logging.

### Current knobs (stock)

- **T4:** soft-cap 0.40, landFrac 0.28, formNudge 1.06, liveShare 0.42, paintEase@100k 0.350
- **T5:** soft-cap 0.44, landFrac 0.33, formNudge 1.1, liveShare 0.50, paintEase@100k 0.595
- **T6:** soft-cap 0.48, landFrac 0.38, formNudge 1.14, liveShare 0.55, paintEase@100k 0.841
- **T7:** soft-cap 0.52, landFrac 0.42, formNudge 1.18, liveShare 0.58, paintEase@100k 0.656
