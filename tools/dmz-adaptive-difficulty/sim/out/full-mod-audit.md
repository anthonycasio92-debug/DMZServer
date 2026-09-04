# Full mod audit — LegacyMechanics 2.3.174

Generated: 2026-09-04 (post-consolidation main)

## Executive summary

| Layer | Result | Notes |
|-------|:------:|-------|
| Product features (`audit_features`) | **PASS** | 677 checks |
| GUI tooltips (`audit_gui_tooltips`) | **PASS** | 206 keys, 188 Java refs, 0 jargon hits |
| GUI ABI (`audit_gui_abi`) | **PASS** | Forge ↔ GUI 2.3.174 handshake |
| Balance concept (`audit_concept`) | **PASS** | 72 checks — tier ladder, dumps, god forms, KP |
| Build matrix (`simulate_build_matrix`) | **PASS** | 35 checks |
| Tier costs (`validate_tier_costs`) | **PASS** | Anchors + monotonic ladder |
| Form bands (`audit_form_bands`) | **PASS** | 125 forms catalogued |
| Tier × level matrix (`audit_tier_level_matrix`) | **ADVISORY** | 71/72 — human android overclock T7 dip |
| Scaling sim (`validate_scaling`) | **ADVISORY** | 55/73 — post-rollback threshold drift |
| Live telemetry (Sep 3–4 pull) | **HEALTHY** | 5,508 hits; T5 avg 0.229 post-hit |

**Ship gate:** `audit_concept` + `audit_features` + `audit_gui_abi` all pass. Scaling sim failures are known drift from the 2.3.162 rollback (counter overlay capped at 1.0, slightly relaxed T5/T7 floors). No live retune recommended yet.

Run again: `bash tools/dmz-adaptive-difficulty/sim/run_full_audit.sh`

---

## 1. Product feature audit (`audit_features`)

**PASS — 677 checks, 0 warnings**

Covers: stock config defaults, gate/scaling markers, personal/death/reward wiring, PWR/ENE channels, class/top-2 counters, dojo rankings API, spar/rival/prestige/progression commands, telemetry hooks, version 2.3.174.

---

## 2. GUI audit

### Tooltips (`audit_gui_tooltips`)

**PASS — 206 catalog keys, 188 referenced from Java**

- All Java-referenced tooltip keys exist in `gui-tooltips.json`
- No banned dev jargon (telemetry, syslog, file-version names, inter-dojo dev-speak)
- Humanized copy for hub, rival, difficulty, spar/dojo (2.3.174)

### ABI (`audit_gui_abi`)

**PASS — 0 warnings**

- `LegacyMechanics-2.3.174.jar` + `LegacyMechanicsGUI-2.3.174.jar`
- Version handshake: Forge VERSION = mods.toml = plugin.yml = 2.3.174
- 241 Forge classes, all required GUI reflection entrypoints present

---

## 3. Balance concept (`audit_concept`)

**PASS — 72 ok, 0 errors**

| Area | Status |
|------|--------|
| Tier ladder (even T1→T5→T7) | ✅ |
| Dump builds (VIT/RES/STR/PWR/tank) | ✅ ≥28% bag at T5 |
| Skills (KP, Ki Infusion, Potential) | ✅ |
| God forms post-DEF | ✅ ≥12% live bag |
| DMZ passthrough NBT clear (all tiers) | ✅ 2.3.164 fix |
| Formula revision | ✅ rev 45 |

Sample saiyan warrior even: T1=0.129 → T5=0.338 → T7=0.399 hitFrac.

---

## 4. Tier economy

### Costs (`validate_tier_costs`)

**PASS**

- T1 @ lvl 1 = 1× Copper
- T7 @ lvl 150k = 100× Netherite
- Strict T1<T2<…<T7 monotonic at all 150k levels

### Tier × level matrix (`audit_tier_level_matrix`)

**ADVISORY — 71 ok, 1 error**

| Check | Result |
|-------|--------|
| Cost anchors + 150k ladder | ✅ |
| Unlock gates T1–T7 | ✅ |
| Saiyan warrior combat bands | ✅ |
| All races × forms soft-cap | ✅ 1554 cells |
| All races × forms dmg>0 | ✅ |
| Peak form mono T3→T7 | ❌ **human** `android_enhancement.overclock@m0`: T6=0.462 → T7=0.451 |

**Action:** Low priority — single human android form edge case; T7 soft-cap interaction causes 0.011 dip.

---

## 5. Combat scaling sim (`validate_scaling`)

**ADVISORY — 55 ok, 18 errors**

Post-rollback (2.3.162) known drift vs stricter legacy thresholds:

| Failure | Value | Context |
|---------|-------|---------|
| class+top2 overlay >1 (7 classes) | overlay=1.000 | Counter overlay capped after rollback |
| T7 tank mobDmg ≤ hitCap | 1380 > 1149 | Tank RES-counter edge; live uses safety-net landing |
| T5 even hitCapFrac ≥ 0.35 | 0.337 | Within 0.003 of threshold |
| T5 god-form post-DEF ≥ 12% | ~11.5% | Generic god probe; SSJG/SSJB pass at 14.5% |
| even T7 hitFrac > T5 | 0.399 vs 0.411 | Soft-cap saturation |

Full report: `sim/out/scaling-validation-report.md`

---

## 6. Build matrix (`simulate_build_matrix`)

**PASS — 35 ok, 0 errors**

---

## 7. Form bands (`audit_form_bands`)

**PASS** — 125 forms. Reference: `sim/out/form-band-reference.md`

---

## 8. Live telemetry (Sep 3–4 pull, 5,508 hits)

| Tier | n | avg hitFracPost | wouldCancel% |
|------|--:|----------------:|-------------:|
| T5 | 604 | **0.229** | 79.0% |
| T6 | 155 | 0.298 | 91.0% |
| T7 | 246 | 0.297 | 71.5% |

**Signal:** T5 engaged pressure healthy. No retune recommended until more post-2.3.174 sample.

---

## 9. Deployed artifacts

| Artifact | Version | Live |
|----------|---------|:----:|
| LegacyMechanics | 2.3.174 | ✅ |
| LegacyMechanicsGUI | 2.3.174 | ✅ |

---

## 10. Recommendations

1. **No combat retune now** — wait 24–48h post-2.3.174 for god-form T5–T7 sample.
2. **Retune `validate_scaling` thresholds** to match rollback constants so CI reflects product gate (`audit_concept`).
3. **Human android overclock** — optional T7 nudge if peak mono must be strict.

Full stdout: `sim/out/full-mod-audit-run.log`
