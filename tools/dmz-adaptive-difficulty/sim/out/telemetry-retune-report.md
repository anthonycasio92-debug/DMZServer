# Telemetry retune report — 46182 hits (8 files)

## Live aggregate (all hits) — avg hitFracPost by tier

| Tier | N | Avg post | Avg pre | >soft-cap | soft-cap | concept min |
|-----:|--:|---------:|--------:|----------:|---------:|------------:|
| T1 | 1814 | 0.120 | 0.069 | 1.7% | 0.30 | — |
| T2 | 2656 | 0.164 | 0.119 | 10.1% | 0.32 | — |
| T3 | 8954 | 0.262 | 0.136 | 9.1% | 0.36 | — |
| T4 | 6050 | 0.305 | 0.228 | 22.6% | 0.40 | — |
| T5 | 19670 | 0.485 | 0.382 | 73.1% | 0.44 | 0.25 |
| T6 | 4916 | 0.496 | 0.340 | 41.1% | 0.48 | — |
| T7 | 2122 | 0.523 | 0.304 | 48.7% | 0.52 | 0.39 |

## Ultra band (formBoost 15–22) — SSJ4, Ultra Perfect, Super Namek (~15–22×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 654 | 0.135 | 0.30 | — |
| T2 | 160 | 0.165 | 0.32 | 1.22× |
| T3 | 302 | 0.281 | 0.36 | 1.71× |
| T4 | 759 | 0.340 | 0.40 | 1.21× |
| T5 | 136 | 0.382 | 0.44 | 1.12× |
| T6 | 259 | 0.502 | 0.48 | 1.31× |
| T7 | 98 | 0.561 | 0.52 | 1.12× |

## Divine band (formBoost 22–50) — SSG, SSB, Beyond God (~22–50×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 390 | 0.116 | 0.30 | — |
| T2 | 1117 | 0.159 | 0.32 | 1.37× |
| T3 | 4037 | 0.246 | 0.36 | 1.55× |
| T4 | 4510 | 0.298 | 0.40 | 1.21× |
| T5 | 15036 | 0.485 | 0.44 | 1.63× |
| T6 | 2118 | 0.500 | 0.48 | 1.03× |
| T7 | 1612 | 0.517 | 0.52 | 1.03× |

## Enhancement band (formBoost 50–80) — Overclock, SSDroid4, Metal Overdrive (~50–80×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 111 | 0.130 | 0.30 | — |
| T2 | 117 | 0.149 | 0.32 | 1.14× |
| T3 | 178 | 0.283 | 0.36 | 1.91× |
| T4 | 9 | 0.408 | 0.40 | 1.44× |
| T5 | 118 | 0.379 | 0.44 | 0.93× |
| T6 | 3 | 0.360 | 0.48 | 0.95× |
| T7 | 4 | 0.422 | 0.52 | 1.17× |

## Apex band (formBoost ≥ 80) — Primal God / cap forms (≥80×)

| Tier | N | Avg post | soft-cap | step vs prev |
|-----:|--:|---------:|---------:|-------------:|
| T1 | 49 | 0.139 | 0.30 | — |
| T2 | 338 | 0.190 | 0.32 | 1.37× |
| T3 | 3087 | 0.295 | 0.36 | 1.55× |
| T4 | 448 | 0.329 | 0.40 | 1.12× |
| T5 | 4175 | 0.499 | 0.44 | 1.52× |
| T6 | 2488 | 0.496 | 0.48 | 0.99× |
| T7 | 357 | 0.552 | 0.52 | 1.11× |

## Sim vs live divine+ apex (formBoost ≥ 22)

| Tier | Live divine+ avg | Sim 5.5k | Sim 100k | Concept min |
|-----:|-----------------:|---------:|---------:|------------:|
| T1 | 0.119 | 0.128 | 0.128 | — |
| T3 | 0.267 | 0.249 | 0.104 | — |
| T5 | 0.488 | 0.338 | 0.201 | 0.25 |
| T7 | 0.523 | 0.399 | 0.399 | 0.39 |

## Recent window
Rows: 5506 (last 2 day file(s))

- T3 ultra recent avg post: **0.067** (n=2)
- T5 ultra recent avg post: **0.261** (n=53)
- T7 ultra recent avg post: **0.383** (n=24)
- T3 divine recent avg post: **0.064** (n=904)
- T5 divine recent avg post: **0.239** (n=388)
- T7 divine recent avg post: **0.264** (n=183)
- T5 enhancement recent avg post: **0.226** (n=51)
- T7 enhancement recent avg post: **0.422** (n=4)
- T3 apex recent avg post: **0.284** (n=14)
- T5 apex recent avg post: **0.226** (n=36)
- T7 apex recent avg post: **0.400** (n=1)

## Retune guidance

- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.
- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.
- Bands: base≤6.0 awakened | super | ultra(~22) | divine(~50) | enhancement(~80) | apex
- Divine+ (≥22) is true god-line pressure; ultra (~15–22) is SSJ4 / race cap.
- Ultra-band avg should climb T1→T7; divine is endgame retune target.

landFrac ladder: T4=0.28 < T5=0.33 < T6=0.38 < T7=0.42