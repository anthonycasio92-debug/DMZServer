# Telemetry retune report — 38270 hits (6 files)

## Live aggregate (all hits) — avg hitFracPost by tier

| Tier | N | Avg post | Avg pre | >soft-cap | soft-cap | concept min |
|-----:|--:|---------:|--------:|----------:|---------:|------------:|
| T1 | 1657 | 0.126 | 0.072 | 1.9% | 0.30 | — |
| T2 | 2452 | 0.169 | 0.125 | 10.9% | 0.32 | — |
| T3 | 6741 | 0.265 | 0.122 | 9.0% | 0.36 | — |
| T4 | 3588 | 0.366 | 0.309 | 35.7% | 0.40 | — |
| T5 | 19061 | 0.490 | 0.389 | 75.1% | 0.44 | 0.25 |
| T6 | 3207 | 0.513 | 0.363 | 60.2% | 0.48 | — |
| T7 | 1564 | 0.551 | 0.298 | 46.3% | 0.52 | 0.39 |

## God-form band (formBoost ≥ 25)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 474 | 0.116 | 0.30 | — |
| T2 | 1204 | 0.168 | 0.32 | 1.45× |
| T3 | 4727 | 0.267 | 0.36 | 1.59× |
| T4 | 3211 | 0.372 | 0.40 | 1.39× |
| T5 | 18621 | 0.492 | 0.44 | 1.32× |
| T6 | 2924 | 0.515 | 0.48 | 1.05× |
| T7 | 1476 | 0.548 | 0.52 | 1.06× |

## 2.3.161 sim (saiyan even, no skills) vs live god band

| Tier | Live god avg | Sim 5.5k | Sim 100k | Concept min |
|-----:|-------------:|---------:|---------:|------------:|
| T1 | 0.116 | 0.128 | 0.128 | — |
| T3 | 0.267 | 0.249 | 0.104 | — |
| T5 | 0.492 | 0.338 | 0.201 | 0.25 |
| T7 | 0.548 | 0.399 | 0.399 | 0.39 |

## Recent window
Rows: 12993 (last 3 day file(s))

- T3 god recent avg post: **0.235** (n=2288)
- T5 god recent avg post: **0.461** (n=3214)

## Retune guidance

- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.
- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.
- God-band live avg should climb monotonically T1→T7 and stay ≤ soft-cap +2%.
- If live god T3≫T2 step after deploy, check paintEase DMZ gates (not difficulty slider max).

landFrac ladder: T4=0.28 < T5=0.33 < T6=0.38 < T7=0.42