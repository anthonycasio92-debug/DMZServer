# LegacyMechanics concept audit (2.3.131)

Fail-closed checks against the player's stated balance concept.

## 1) Tier ladder (even build)

- ✅ stock percents 21→200%
- ✅ stock tankDamageHealthRatio 0.28 — got 0.28
- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.135 T5=0.468
- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.468 T7=0.577
- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.468
- ✅ even T7 unprotected ≥40% bag — hitFrac=0.577

## 2) Dump builds feel the ladder (not shrug)

- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.441
- ✅ vit_dump T5 ≥ 60% of even — 0.441 vs even 0.468
- ✅ vit_dump T7 > T1 ×1.8 — T1=0.050 T7=0.577
- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.154
- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.468
- ✅ res_dump T5 ≥ 60% of even — 0.468 vs even 0.468
- ✅ res_dump T7 > T1 ×1.8 — T1=0.068 T7=0.577
- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.164
- ✅ str_dump T5 ≥ 28% bag — hitFrac=0.468
- ✅ str_dump T5 ≥ 60% of even — 0.468 vs even 0.468
- ✅ str_dump T7 > T1 ×1.8 — T1=0.234 T7=0.577
- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.164
- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.468
- ✅ pwr_dump T5 ≥ 60% of even — 0.468 vs even 0.468
- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.206 T7=0.577
- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.164
- ✅ tank class T5 ≥ 28% bag — hitFrac=0.445
- ✅ tank class T5 ≥ 60% of even — tank=0.445 even=0.468

## 3) Skills matter

- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.468 afterKp=0.421
- ✅ Ki Infusion sponges more pack HP — none=874 inf=1119
- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=1506 pu=1686
- ✅ KP still saves on god form — pre=0.520 afterKp=0.468

## 4) God forms do not out-tank (incl. DMZ DEF-cancel)

- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.520 postDef~=0.182 ×54.0
- ✅ SSJG T5 post-DEF ≥12% live — pre=0.520 postDef~=0.182
- ✅ SSJG T7 clears cancel or landing ≥35% — wouldCancel=True dmg=464 flatMit=2442 landingFrac=0.620
- ✅ SSJG T7 landing safety-net ≥35% bag — landingFrac=0.620
- ✅ SSJB T5 post-DEF ≥12% live — pre=0.520 postDef~=0.182
- ✅ SSJB T7 clears cancel or landing ≥35% — wouldCancel=True dmg=464 flatMit=4775 landingFrac=0.620
- ✅ SSJB T7 landing safety-net ≥35% bag — landingFrac=0.620

## 5) Melee AD parity (feature gates)

- ✅ painted melee shock/slam
- ✅ Awakened+ chase speed
- ✅ Adaptive AI speed from Enhanced+

## 6) Version / formula revision

- ✅ VERSION 2.3.146
- ✅ RaceSkillSync present
- ✅ formula revision 40
- ✅ hpFloorStrength present
- ✅ T1–T3 god-form floors raised
- ✅ T7 incoming soft-cap in events
- ✅ T5 soft-cap 52%
- ✅ T6 soft-cap 58%
- ✅ T3 soft-cap ≤ T4
- ✅ T4 soft-cap ≤ T5
- ✅ T5 soft-cap ≤ T6
- ✅ T6 soft-cap ≤ T7
- ✅ landFrac ladder progressive T4<T5<T6<T7
- ✅ god soft-cap ladder T3≤T4 — T3=0.440 T4=0.500
- ✅ god soft-cap ladder T4≤T5 — T4=0.500 T5=0.520
- ✅ god soft-cap ladder T5≤T6 — T5=0.520 T6=0.580
- ✅ god soft-cap ladder T6≤T7 — T6=0.580 T7=0.620
- ✅ god landing T5 < T6 (buy matters) — T5=0.520 T6=0.580
- ✅ god landing T4 < T5 — T4=0.487 T5=0.520
- ✅ god landing ≤ soft-cap T5 — landing=0.520
- ✅ god landing ≤ soft-cap T6 — landing=0.580
- ✅ combat telemetry present
- ✅ god-form landing T1≥10% — landingFrac=0.137
- ✅ god-form landing T5≥28% — landingFrac=0.520
- ✅ god-form landing T7≥40% — landingFrac=0.620
- ✅ god-form landing T7>T1×2.5 — T1=0.137 T7=0.620

## Sample numbers (saiyan warrior)

| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |
|-------|---:|---:|------------:|---:|-----------:|
| even | 0.135 | 0.468 | 0.164 | 0.577 | 1.00× |
| vit_dump | 0.050 | 0.441 | 0.154 | 0.577 | 0.94× |
| res_dump | 0.068 | 0.468 | 0.164 | 0.577 | 1.00× |
| str_dump | 0.234 | 0.468 | 0.164 | 0.577 | 1.00× |
| pwr_dump | 0.206 | 0.468 | 0.164 | 0.577 | 1.00× |
| tank class | — | 0.445 | 0.156 | — | 0.95× |

| Form | hitFrac | post-DEF | softHits |
|------|--------:|---------:|---------:|
| androidforms.ssdroid4 | 0.520 | 0.182 | 0.79 |

**Result:** PASS — 64 ok, 0 error(s).
