# Telemetry retune report — 42087 hits (7 files)

## Live aggregate (all hits) — avg hitFracPost by tier

| Tier | N | Avg post | Avg pre | >soft-cap | soft-cap | concept min |
|-----:|--:|---------:|--------:|----------:|---------:|------------:|
| T1 | 1533 | 0.134 | 0.077 | 2.0% | 0.30 | — |
| T2 | 2296 | 0.180 | 0.133 | 11.6% | 0.32 | — |
| T3 | 7819 | 0.293 | 0.153 | 10.4% | 0.36 | — |
| T4 | 4666 | 0.378 | 0.289 | 29.3% | 0.40 | — |
| T5 | 19132 | 0.493 | 0.392 | 75.1% | 0.44 | 0.25 |
| T6 | 4761 | 0.503 | 0.349 | 42.4% | 0.48 | — |
| T7 | 1880 | 0.552 | 0.330 | 54.9% | 0.52 | 0.39 |

## God-form band (formBoost ≥ 25)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 414 | 0.128 | 0.30 | — |
| T2 | 1091 | 0.179 | 0.32 | 1.39× |
| T3 | 6038 | 0.295 | 0.36 | 1.65× |
| T4 | 3488 | 0.389 | 0.40 | 1.32× |
| T5 | 18752 | 0.494 | 0.44 | 1.27× |
| T6 | 4490 | 0.503 | 0.48 | 1.02× |
| T7 | 1785 | 0.550 | 0.52 | 1.09× |

## 2.3.161 sim (saiyan even, no skills) vs live god band

| Tier | Live god avg | Sim 5.5k | Sim 100k | Concept min |
|-----:|-------------:|---------:|---------:|------------:|
| T1 | 0.128 | 0.128 | 0.128 | — |
| T3 | 0.295 | 0.249 | 0.104 | — |
| T5 | 0.494 | 0.338 | 0.201 | 0.25 |
| T7 | 0.550 | 0.399 | 0.399 | 0.39 |

## Recent window
Rows: 11206 (last 3 day file(s))

- T3 god recent avg post: **0.308** (n=1652)
- T5 god recent avg post: **0.473** (n=3081)

## Retune guidance

- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.
- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.
- God-band live avg should climb monotonically T1→T7 and stay ≤ soft-cap +2%.
- If live god T3≫T2 step after deploy, check paintEase DMZ gates (not difficulty slider max).

landFrac ladder: T4=0.28 < T5=0.33 < T6=0.38 < T7=0.42