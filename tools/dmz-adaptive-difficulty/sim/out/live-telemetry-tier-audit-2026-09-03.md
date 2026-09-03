# Live telemetry tier audit — 2026-09-03

**Source:** SFTP pull from production `config/legacymechanics/telemetry/`  
**Hits:** 42,059 across 7 files (Aug 9 – Sep 3)  
**Live jars:** `LegacyMechanics-2.3.159.jar` (Sep 2)  
**Live config:** `config/legacymechanics.json` — stock tier percents confirmed (21%→200%)

## Verdict

Scaling is **not globally broken** — the tier ladder is monotonic T1→T7 on live telemetry.  
Two pressure points match player reports:

| Issue | Live signal | Concept target | Fix |
|-------|-------------|----------------|-----|
| **T2→T3 cliff** | God step **1.65×** (0.179→0.295) — steepest jump | ~1.3–1.4× | **2.3.160** — delayed counters, eased god floors, level ramp |
| **T5→T6 glue** | Aggregate step **1.02×** (0.494→0.503) | Sim **1.15×** (0.520→0.600) | Monitor; engaged players see 1.04–1.10× |

## Live god-form ladder (42k hits)

| Tier | n | avg hitPost | step | soft-cap | vs cap |
|-----|--:|------------:|-----:|---------:|-------:|
| T1 | 414 | 0.128 | — | 0.34 | below |
| T2 | 1,088 | 0.179 | 1.40× | 0.36 | below |
| T3 | 6,038 | 0.295 | **1.65×** | 0.44 | below |
| T4 | 3,488 | 0.389 | 1.32× | 0.50 | below |
| T5 | 18,752 | 0.494 | 1.27× | 0.52 | at cap |
| T6 | 4,490 | 0.503 | **1.02×** | 0.60 | below |
| T7 | 1,785 | 0.550 | 1.09× | 0.62 | below |

All tiers sit **below** soft-cap ceilings — no runaway one-shots. T5 is the first tier where god players routinely kiss the cap.

## Per-player T5→T6 (god band, both tiers sampled)

| Player | T5 avg | T6 avg | T6/T5 |
|--------|-------:|-------:|------:|
| Ch4osDoom64 | 0.516 | 0.570 | 1.10× |
| RogerioTorio | 0.473 | 0.500 | 1.06× |
| YokaiPrime | 0.519 | 0.542 | 1.04× |
| Daiko4319 | 0.454 | 0.464 | 1.02× |

Players who actually buy T6 see modest climb; aggregate flatness is mostly **sample mix** (heavy T5 population, different builds at T6).

## Concept audits (2.3.160 code)

| Audit | Result |
|-------|--------|
| `audit_concept.py` | PASS — 66 checks |
| `audit_tier_level_matrix.py` | PASS — 72 checks |
| `validate_scaling.py` | PASS — 73 checks |
| `audit_features.py` | PASS — 657 checks |

## Sim vs live (god form, saiyan warrior even)

| Tier | Sim 2.3.160 | Live | Delta |
|-----:|------------:|-----:|------:|
| T3 | 0.440 | 0.295 | +0.145 |
| T5 | 0.520 | 0.494 | +0.026 |
| T6 | 0.600 | 0.503 | +0.097 |

Live reads **below** sim at T3/T6 because real players run KP, DEF, and mitigation — telemetry `hitFracPost` is after player mitigation. Sim is unmitigated paint ceiling.

## T3 level ramp (2.3.160 sim, god form)

| DMZ level | hitFrac | landingFrac |
|----------:|--------:|------------:|
| 1,000 | 0.440 | 0.197 |
| 5,500 | 0.440 | 0.228 |
| 10,000 | 0.440 | 0.260 |

God forms at T3 hit the **0.44 soft-cap** regardless of level — ramp eases tank/live **floors** and **landing** for sub-cap paths (fresh buys, non-god, tanks).

## Recommended actions

1. **Deploy 2.3.160 to test** — addresses T3 cliff (counter delay 65%→72%, eased god floors, level ramp).
2. **Validate T3 feel** with fresh buys (~level 1k) and mid-band (~level 5k).
3. **Watch T5→T6** on test after T3 deploy; no formula change yet — live engaged players already climb 1.04–1.10×.
4. Keep telemetry on: `/difficulty admin telemetry on`

## Tools added

- `sim/analyze_tier_ladder.py` — tier monotonicity + concept comparison from hits JSONL
- `sim/summarize_telemetry.py` — existing aggregate summary
