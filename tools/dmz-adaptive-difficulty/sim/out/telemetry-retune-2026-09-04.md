# Telemetry retune — 2.3.175 (Sep 4 pull)

**Source:** `uploads/live-telemetry-2026-09-04-retune/` — **46,184** hits (Aug 29–Sep 4)

## Signal

| Window | T5 avg post | T7 avg post | vs concept |
|--------|------------:|------------:|------------|
| All hits (mixed jars) | 0.485 | 0.523 | Too hot — pre-rollback inflation |
| Since Sep 3 (post-fix) | 0.229 | 0.297 | Too cold — below 0.25 / 0.39 mins |
| Divine+ since Sep 3 | 0.237 | 0.269 | God line under concept |
| Sep 4 only | 0.219 | 0.297 | Confirms cold post-2.3.171 |

**Diagnosis:** Rollback + paintEase fix corrected the 73% above-soft-cap T5 spike, but god-form **landing** path was over-dampened (`land *= paintEase`). Veterans at ease ~0.6 saw ~40% landing cut.

## 2.3.175 adjustments (formula rev 46)

| Knob | T5 | T6 | T7 |
|------|----|----|-----|
| liveShare | 0.50→**0.53** | 0.55→**0.58** | 0.58→**0.62** |
| formNudge | 1.10→**1.11** | 1.14→**1.16** | 1.18→**1.20** |
| landFrac | 0.33→**0.35** | 0.38→**0.40** | 0.42→**0.44** |
| landCap | 0.36→**0.38** | 0.40→**0.42** | 0.44→**0.46** |
| T7 mega damp | 0.18→**0.28** | — | — |
| Vet ease floor (50k+) | 0.70→**0.78** | — | — |
| Landing paintEase | `×ease` → **`×(0.70+0.30×ease)`** | — | — |

## Sim post-retune (saiyan warrior god)

| Tier | hitFrac @ gate | landingFrac @ gate |
|------|---------------:|-------------------:|
| T5 | 0.338 | **0.380** (was ~0.360) |
| T7 | 0.399 | **0.460** (was ~0.440) |

`audit_concept`: **PASS** (72/72). `simulate_build_matrix`: **PASS**.

## Deploy

- `LegacyMechanics-2.3.175.jar`
- Re-pull telemetry after 24–48h; target divine+ T5 ~0.35–0.42, T7 ~0.38–0.45 at gate.
