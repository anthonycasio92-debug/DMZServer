# LegacyMechanics full mod audit (2.3.131)

Generated: 2026-09-01T23:24Z
Branch: cursor/telemetry-scaling-cal-c766

| Audit | Result | Detail |
|-------|:------:|--------|
| audit_features | PASS | 555 checks, 0 warnings |
| audit_concept | PASS | 64 ok, 0 errors |
| validate_tier_costs | PASS | buy-cost ladder |
| validate_scaling | PASS | 73 checks |
| simulate_build_matrix --check | PASS | 35 ok, 0 errors |
| simulate_race_forms --check | PASS | hard flags: none (10 races) |
| audit_tier_level_matrix | PASS | 72 ok — costs 1–150k + 1554 race/form cells |
| audit_gui_abi | PASS | ABI intact, 0 warnings |

## Version handshake
/workspace/tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java:33:    public static final String VERSION = "2.3.131";
/workspace/tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java:60:                VERSION,

## Concept (excerpt)
7:- ✅ stock percents 21→200%
8:- ✅ stock tankDamageHealthRatio 0.28 — got 0.28
9:- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.135 T5=0.468
10:- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.468 T7=0.577
11:- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.468
12:- ✅ even T7 unprotected ≥40% bag — hitFrac=0.577
16:- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.441
17:- ✅ vit_dump T5 ≥ 60% of even — 0.441 vs even 0.468
18:- ✅ vit_dump T7 > T1 ×1.8 — T1=0.050 T7=0.577
19:- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.154
20:- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.468
21:- ✅ res_dump T5 ≥ 60% of even — 0.468 vs even 0.468
22:- ✅ res_dump T7 > T1 ×1.8 — T1=0.068 T7=0.577
23:- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.164
24:- ✅ str_dump T5 ≥ 28% bag — hitFrac=0.468
25:- ✅ str_dump T5 ≥ 60% of even — 0.468 vs even 0.468
26:- ✅ str_dump T7 > T1 ×1.8 — T1=0.234 T7=0.577
27:- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.164
28:- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.468
29:- ✅ pwr_dump T5 ≥ 60% of even — 0.468 vs even 0.468
30:- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.206 T7=0.577
31:- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.164
32:- ✅ tank class T5 ≥ 28% bag — hitFrac=0.445
33:- ✅ tank class T5 ≥ 60% of even — tank=0.445 even=0.468
37:- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.468 afterKp=0.421
38:- ✅ Ki Infusion sponges more pack HP — none=874 inf=1119
39:- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=1506 pu=1686
40:- ✅ KP still saves on god form — pre=0.520 afterKp=0.468
44:- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.520 postDef~=0.182 ×54.0
45:- ✅ SSJG T5 post-DEF ≥12% live — pre=0.520 postDef~=0.182
46:- ✅ SSJG T7 clears cancel or landing ≥35% — wouldCancel=True dmg=464 flatMit=2442 landingFrac=0.620
47:- ✅ SSJG T7 landing safety-net ≥35% bag — landingFrac=0.620
48:- ✅ SSJB T5 post-DEF ≥12% live — pre=0.520 postDef~=0.182
49:- ✅ SSJB T7 clears cancel or landing ≥35% — wouldCancel=True dmg=464 flatMit=4775 landingFrac=0.620
50:- ✅ SSJB T7 landing safety-net ≥35% bag — landingFrac=0.620
54:- ✅ painted melee shock/slam
55:- ✅ Awakened+ chase speed
56:- ✅ Adaptive AI speed from Enhanced+
60:- ✅ VERSION 2.3.131
61:- ✅ RaceSkillSync present

## Features (sample Android / combat)
9:  OK  RaceSkillSync discovers DMZ races
10:  OK  RaceSkillSync grants Fabled skill
11:  OK  RaceSkillSync skips race-lock purchase gates
62:  OK  event incoming soft-cap
147:=== Post-pierce soft-cap clamp (1.0.25) ===
150:  OK  formula revision 39
166:  OK  formula revision 39
174:  OK  progressive soft-caps
219:  OK  T3 soft-cap 0.44
220:  OK  T4 soft-cap 0.50
221:  OK  T1 soft-cap 0.34
222:  OK  T2 soft-cap 0.36
223:  OK  README soft-cap ladder
387:=== Android convert + remove (2.3.54) ===
388:  OK  AndroidConversion.remove
389:  OK  AndroidConversion two-click confirm
390:  OK  AndroidConversion restore superforms
391:  OK  Android blocks bioandroid only
392:  OK  Android gate via androidforms TP costs
393:  OK  Android eligibleRaceHint
394:  OK  Android isAndroidUpgraded helper
395:  OK  Android deny message not humans-only
396:  OK  GUI lists all Android-capable races
415:  OK  Hub Remove Android button
416:  OK  CMI Hub Remove Android
663:PASS — intended features intact (555 checks, 0 warning(s))

## Race/form peaks (T5 m100)
2:race               boost   dmgJ   hpJ   hits   hitF   cap form
14:Hard flags:
26:Wrote /opt/cursor/artifacts/ad-race-form-simulation.csv /opt/cursor/artifacts/ad-race-form-balance-report.md
27:Discovered races (10): ancient_saiyan, bioandroid, frostdemon, human, majin, monkey, namekian, saiyan, sento_saiyan, viltrumite

## Tier × level × race matrix (section 5)
65:- ✅ god landing ≤ soft-cap all tiers
112:- ✅ discovered ≥8 stock races — 10: ancient_saiyan, bioandroid, frostdemon, human, majin, monkey, namekian, saiyan, sento_saiyan, viltrumite
125:- ✅ race/form soft-cap all cells (1554) — 0.04s
126:- ✅ race/form mobDmg > 0 all cells — ok
127:- ✅ race peak form ladders / floors — 10 peaks ok
129:### Android forms coverage
137:**Result:** PASS — 72 ok, 0 error(s).

## GUI ABI

=== Package root OK (227 classes under com.dbzlegacy.adaptivedifficulty) ===

=== Summary ===
PASS — ABI intact (0 warning(s))

**Overall: PASS — all 8 audits green.**
