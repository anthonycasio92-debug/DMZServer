# Telemetry retune report — 38669 hits (6 files)

## Live aggregate (all hits) — avg hitFracPost by tier

| Tier | N | Avg post | Avg pre | >soft-cap | soft-cap | concept min |
|-----:|--:|---------:|--------:|----------:|---------:|------------:|
| T1 | 1657 | 0.126 | 0.072 | 1.9% | 0.30 | — |
| T2 | 2452 | 0.169 | 0.125 | 10.9% | 0.32 | — |
| T3 | 6793 | 0.263 | 0.121 | 9.0% | 0.36 | — |
| T4 | 3631 | 0.362 | 0.306 | 35.3% | 0.40 | — |
| T5 | 19335 | 0.487 | 0.384 | 74.0% | 0.44 | 0.25 |
| T6 | 3220 | 0.513 | 0.362 | 59.9% | 0.48 | — |
| T7 | 1581 | 0.548 | 0.298 | 45.8% | 0.52 | 0.39 |

## Ultra band (formBoost 15–22) — SSJ4, Ultra Perfect, Super Namek (~15–22×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 599 | 0.138 | 0.30 | — |
| T2 | 160 | 0.165 | 0.32 | 1.19× |
| T3 | 302 | 0.281 | 0.36 | 1.71× |
| T4 | 26 | 0.340 | 0.40 | 1.21× |
| T5 | 136 | 0.382 | 0.44 | 1.12× |
| T6 | 248 | 0.509 | 0.48 | 1.33× |
| T7 | 74 | 0.618 | 0.52 | 1.21× |

## Divine band (formBoost 22–50) — SSG, SSB, Beyond God (~22–50×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 390 | 0.116 | 0.30 | — |
| T2 | 961 | 0.170 | 0.32 | 1.46× |
| T3 | 2886 | 0.253 | 0.36 | 1.49× |
| T4 | 3268 | 0.371 | 0.40 | 1.46× |
| T5 | 14968 | 0.486 | 0.44 | 1.31× |
| T6 | 2025 | 0.509 | 0.48 | 1.05× |
| T7 | 1437 | 0.547 | 0.52 | 1.08× |

## Enhancement band (formBoost 50–80) — Overclock, SSDroid4, Metal Overdrive (~50–80×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 111 | 0.130 | 0.30 | — |
| T2 | 117 | 0.149 | 0.32 | 1.14× |
| T3 | 176 | 0.283 | 0.36 | 1.91× |
| T4 | 9 | 0.408 | 0.40 | 1.44× |
| T5 | 118 | 0.379 | 0.44 | 0.93× |
| T6 | 0 | 0.000 | 0.48 | — |
| T7 | 0 | 0.000 | 0.52 | — |

## Apex band (formBoost ≥ 80) — Primal God / cap forms (≥80×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 49 | 0.139 | 0.30 | — |
| T2 | 338 | 0.190 | 0.32 | 1.37× |
| T3 | 2151 | 0.288 | 0.36 | 1.52× |
| T4 | 15 | 0.392 | 0.40 | 1.36× |
| T5 | 3911 | 0.501 | 0.44 | 1.28× |
| T6 | 905 | 0.528 | 0.48 | 1.05× |
| T7 | 47 | 0.507 | 0.52 | 0.96× |

## Sim vs live divine+ apex (formBoost ≥ 22)

| Tier | Live divine+ avg | Sim 5.5k | Sim 100k | Concept min |
|-----:|-----------------:|---------:|---------:|------------:|
| T1 | 0.119 | 0.128 | 0.128 | — |
| T3 | 0.268 | 0.249 | 0.104 | — |
| T5 | 0.490 | 0.338 | 0.201 | 0.25 |
| T7 | 0.546 | 0.399 | 0.399 | 0.39 |

## Recent window
Rows: 3597 (last 1 day file(s))

- T3 ultra recent avg post: **0.067** (n=2)
- T5 ultra recent avg post: **0.261** (n=53)
- T3 divine recent avg post: **0.071** (n=762)
- T5 divine recent avg post: **0.243** (n=320)
- T7 divine recent avg post: **0.202** (n=8)
- T5 enhancement recent avg post: **0.226** (n=51)
- T3 apex recent avg post: **0.284** (n=14)
- T5 apex recent avg post: **0.226** (n=36)

## Retune guidance

- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.
- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.
- Bands: base≤6.0 awakened | super | ultra(~22) | divine(~50) | enhancement(~80) | apex
- Divine+ (≥22) is true god-line pressure; ultra (~15–22) is SSJ4 / race cap.
- Ultra-band avg should climb T1→T7; divine is endgame retune target.

landFrac ladder: T4=0.28 < T5=0.33 < T6=0.38 < T7=0.42