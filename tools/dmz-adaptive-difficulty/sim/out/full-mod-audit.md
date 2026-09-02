# LegacyMechanics full mod audit (2.3.156)

Date: 2026-09-02T21:14Z  
Branch: `cursor/mod-audit-c766` (tip includes 2.3.156 Skill Check / Potential Piccolo gate)  
Jars: `LegacyMechanics-2.3.156.jar` + `LegacyMechanicsGUI-2.3.156.jar`

Fail-closed suite — all green:

| Audit | Result | Detail |
|-------|:------:|--------|
| audit_features | PASS | 646 checks, 0 warnings |
| audit_concept | PASS | 64 ok, 0 errors |
| validate_tier_costs | PASS | T1–T7 ladder 1→150k; T7@150k = 100× Netherite |
| validate_scaling | PASS | 73 checks (class/top-2/VIT-RES-STR dumps) |
| simulate_build_matrix --check | PASS | 35 ok; 9792 rows / 10 races |
| simulate_race_forms --check | PASS | 10 races discovered |
| audit_tier_level_matrix | PASS | 72 ok; **1554** race×form×tier cells soft≤cap |
| audit_gui_abi | PASS | Forge↔GUI reflection + version handshake; 229 classes |

## Notes

- Version handshake: Forge `AdaptiveDifficultyMod.VERSION` = GUI `plugin.yml` = **2.3.156**.
- Android forms coverage still present for Human / Saiyan / Frost Demon / Viltrumite.
- Race/form soft-cap: all 1554 cells OK; mob damage > 0 all cells.
- Glass-pack notes on high-offense races (saiyan / ancient_saiyan / etc.) are informational — RES counters still apply.

## Artifacts

- `sim/out/feature-audit-report.txt`
- `sim/out/ad-concept-audit.md`
- `sim/out/scaling-validation-report.md`
- `sim/out/tier-level-matrix-audit.md`
- `sim/out/gui-abi.txt`
- `/opt/cursor/artifacts/ad-race-form-balance-report.md`
- `/opt/cursor/artifacts/ad-build-matrix-report.md`
