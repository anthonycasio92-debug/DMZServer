# Full mod audit — LegacyMechanics 2.4.2

Generated: 2026-09-15T21:56Z

## Suite summary

| Audit | Result |
|-------|:------:|
| audit_scaling_sync | PASS |
| audit_features | PASS |
| audit_gui_tooltips | PASS |
| audit_form_bands | PASS |
| validate_tier_costs | PASS |
| audit_tier_level_matrix | PASS |
| validate_scaling | PASS |
| simulate_build_matrix | PASS |
| audit_concept | PASS |
| audit_gui_abi | PASS |

## Notes

- **audit_scaling_sync** keeps Java literals aligned with `scaling_constants.py` (run on every build).
- **audit_concept** is the product-level balance gate (player-facing intent).
- **validate_scaling** + **audit_tier_level_matrix** catch tier-ladder and race/form regressions.

See `sim/out/full-mod-audit-run.log` for full output.

## Tier × level matrix

# Tier × level matrix audit (1–150000)

Fail-closed checks for unlock tiers T1–T7 across the DMZ level cap.

## 1) Cost anchors

- ✅ T1 @ lvl 1 = 1× Copper — 1× Copper
- ✅ T7 @ lvl 150000 = 100× Netherite — 100× Netherite (10000000)
- ✅ stock bases T1..T7
- ✅ anchor 150000

## 2) Buy-cost ladder T1<T2<…<T7 at every level

- ✅ cost T-ladder mono across 150000 levels — 0.87s
- ✅ cost non-decreasing with level (≤150k) — ok
- ✅ past-anchor clamp T7 200k==150k — 10000000 vs 10000000
- ✅ past-anchor clamp T1 200k==150k — 6700 vs 6700

### Sample costs

| level | T1 | T2 | T3 | T4 | T5 | T6 | T7 |
|------:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1× Copper | 5× Copper | 15× Copper | 5× Iron | 15× Iron | 5× Gold | 15× Gold |
| 500 | 1× Copper | 5× Copper | 15× Copper | 51× Copper | 16× Iron | 52× Iron | 16× Gold |
| 1000 | 1× Copper | 5× Copper | 16× Copper | 53× Copper | 16× Iron | 53× Iron | 16× Gold |
| 5000 | 1× Copper | 7× Copper | 2× Iron | 67× Copper | 21× Iron | 68× Iron | 21× Gold |
| 10000 | 2× Copper | 9× Copper | 27× Copper | 9× Iron | 27× Iron | 9× Gold | 27× Gold |
| 50000 | 19× Copper | 94× Copper | 29× Iron | 95× Iron | 29× Gold | 95× Gold | 29× Emerald |
| 100000 | 36× Iron | 18× Gold | 54× Gold | 18× Emerald | 54× Emerald | 18× Diamond | 54× Diamond |
| 150000 | 67× Gold | 34× Emerald | 1× Netherite | 34× Diamond | 10× Netherite | 34× Netherite | 100× Netherite |

## 3) Unlock DMZ level gates

- ✅ stock REQUIRED matches UnlockTier — {1: 1, 2: 500, 3: 1000, 4: 5000, 5: 10000, 6: 50000, 7: 100000}
- ✅ T1 unlock level ≥ prior — 1 ≥ 0
- ✅ T1 unlocked at lvl 1 — level≥1 or prestige≥1
- ✅ T2 unlock level ≥ prior — 500 ≥ 1
- ✅ T2 locked below lvl 500 — gate=500
- ✅ T2 unlocked at lvl 500 — level≥500 or prestige≥2
- ✅ T3 unlock level ≥ prior — 1000 ≥ 500

… full report: `sim/out/tier-level-matrix-audit.md`

## Scaling validation (excerpt)


… full report: `sim/out/scaling-validation-report.md`

**Overall:** PASS
