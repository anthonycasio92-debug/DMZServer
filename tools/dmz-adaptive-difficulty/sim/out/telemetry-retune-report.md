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

## Transformed band (formBoost 25–50)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 314 | 0.107 | 0.30 | — |
| T2 | 749 | 0.161 | 0.32 | 1.50× |
| T3 | 2439 | 0.244 | 0.36 | 1.52× |
| T4 | 3187 | 0.372 | 0.40 | 1.53× |
| T5 | 14699 | 0.488 | 0.44 | 1.31× |
| T6 | 2021 | 0.509 | 0.48 | 1.04× |
| T7 | 1437 | 0.547 | 0.52 | 1.08× |

## Mega band (formBoost 50–80)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 111 | 0.130 | 0.30 | — |
| T2 | 117 | 0.149 | 0.32 | 1.14× |
| T3 | 176 | 0.283 | 0.36 | 1.91× |
| T4 | 9 | 0.408 | 0.40 | 1.44× |
| T5 | 118 | 0.379 | 0.44 | 0.93× |
| T6 | 0 | 0.000 | 0.48 | — |
| T7 | 0 | 0.000 | 0.52 | — |

## God band (formBoost ≥ 80)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 49 | 0.139 | 0.30 | — |
| T2 | 338 | 0.190 | 0.32 | 1.37× |
| T3 | 2151 | 0.288 | 0.36 | 1.52× |
| T4 | 15 | 0.392 | 0.40 | 1.36× |
| T5 | 3911 | 0.501 | 0.44 | 1.28× |
| T6 | 905 | 0.528 | 0.48 | 1.05× |
| T7 | 47 | 0.507 | 0.52 | 0.96× |

## Sim vs live god band (formBoost ≥ 80)

| Tier | Live god avg | Sim 5.5k | Sim 100k | Concept min |
|-----:|-------------:|---------:|---------:|------------:|
| T1 | 0.139 | 0.128 | 0.128 | — |
| T3 | 0.288 | 0.249 | 0.104 | — |
| T5 | 0.501 | 0.338 | 0.201 | 0.25 |
| T7 | 0.507 | 0.399 | 0.399 | 0.39 |

## Recent window
Rows: 3597 (last 1 day file(s))

- T3 transformed recent avg post: **0.065** (n=741)
- T5 transformed recent avg post: **0.232** (n=183)
- T7 transformed recent avg post: **0.202** (n=8)
- T5 mega recent avg post: **0.226** (n=51)
- T3 god recent avg post: **0.284** (n=14)
- T5 god recent avg post: **0.226** (n=36)

## Retune guidance

- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.
- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.
- God band = formBoost ≥ 80 (mega target). Transformed 25–50 is normal stack play.
- God-band live avg should climb monotonically T1→T7 and stay ≤ soft-cap +2%.
- If live transformed≫strong step, check stack-form paint — not the old god≥25 bucket.

landFrac ladder: T4=0.28 < T5=0.33 < T6=0.38 < T7=0.42