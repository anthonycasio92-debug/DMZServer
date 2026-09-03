# LegacyMechanics concept audit (2.3.131)

Fail-closed checks against the player's stated balance concept.

## 1) Tier ladder (even build)

- ✅ stock percents 21→200%
- ✅ stock tankDamageHealthRatio 0.28 — got 0.28
- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.129 T5=0.447
- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.447 T7=0.583
- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.447
- ✅ even T7 unprotected ≥40% bag — hitFrac=0.583

## 2) Dump builds feel the ladder (not shrug)

- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.291
- ✅ vit_dump T5 ≥ 58% of even — 0.291 vs even 0.447
- ✅ vit_dump T7 > T1 ×1.8 — T1=0.020 T7=0.351
- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.102
- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.358
- ✅ res_dump T5 ≥ 58% of even — 0.358 vs even 0.447
- ✅ res_dump T7 > T1 ×1.8 — T1=0.065 T7=0.450
- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.125
- ✅ str_dump T5 ≥ 28% bag — hitFrac=1.148
- ✅ str_dump T5 ≥ 58% of even — 1.148 vs even 0.447
- ✅ str_dump T7 > T1 ×1.8 — T1=0.442 T7=1.620
- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.402
- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.543
- ✅ pwr_dump T5 ≥ 58% of even — 0.543 vs even 0.447
- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.185 T7=0.724
- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.190
- ✅ tank class T5 ≥ 28% bag — hitFrac=0.286
- ✅ tank class T5 ≥ 58% of even — tank=0.286 even=0.447

## 3) Skills matter

- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.447 afterKp=0.403
- ✅ Ki Infusion sponges more pack HP — none=874 inf=1119
- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=1506 pu=1686
- ✅ KP still saves on god form — pre=0.533 afterKp=0.480

## 4) God forms do not out-tank (incl. DMZ DEF-cancel)

- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.533 postDef~=0.187 ×54.0
- ✅ SSJG T5 post-DEF ≥12% live — pre=0.854 postDef~=0.299
- ✅ SSJG T7 clears cancel or landing ≥30% — wouldCancel=True dmg=871 flatMit=2442 landingFrac=0.306
- ✅ SSJG T7 landing safety-net ≥30% bag — landingFrac=0.306
- ✅ SSJB T5 post-DEF ≥12% live — pre=0.919 postDef~=0.322
- ✅ SSJB T7 clears cancel or landing ≥30% — wouldCancel=True dmg=943 flatMit=4775 landingFrac=0.310
- ✅ SSJB T7 landing safety-net ≥30% bag — landingFrac=0.310

## 5) Melee AD parity (feature gates)

- ✅ painted melee shock/slam
- ✅ Awakened+ chase speed
- ✅ Adaptive AI speed from Enhanced+

## 6) Version / formula revision

- ✅ VERSION 2.3.161
- ✅ RaceSkillSync present
- ✅ formula revision 43
- ✅ hpFloorStrength present
- ✅ T1–T3 god-form floors (1.0.12 rollback)
- ✅ paintEase DMZ level ramp
- ✅ counter strength from T4
- ✅ T7 incoming soft-cap via profile
- ✅ T5 soft-cap 44%
- ✅ T6 soft-cap 48%
- ✅ T3 soft-cap ≤ T4
- ✅ T4 soft-cap ≤ T5
- ✅ T5 soft-cap ≤ T6
- ✅ T6 soft-cap ≤ T7
- ✅ landFrac ladder progressive T4<T5<T6<T7
- ✅ god soft-cap ladder T3≤T4 — T3=0.411 T4=0.682
- ✅ god soft-cap ladder T4≤T5 — T4=0.682 T5=0.919
- ✅ god soft-cap ladder T5≤T6 — T5=0.919 T6=1.057
- ✅ god soft-cap ladder T6≤T7 — T6=1.057 T7=1.258
- ✅ god landing T5 < T6 (buy matters) — T5=0.241 T6=0.278
- ✅ god landing T4 < T5 — T4=0.211 T5=0.241
- ✅ god landing ≤ soft-cap T5 — landing=0.241
- ✅ god landing ≤ soft-cap T6 — landing=0.278
- ✅ combat telemetry present
- ✅ god-form landing T1≥3% — landingFrac=0.034
- ✅ god-form landing T5≥22% — landingFrac=0.241
- ✅ god-form landing T7≥30% — landingFrac=0.310
- ✅ god-form landing T7>T1×2.5 — T1=0.034 T7=0.310

## Sample numbers (saiyan warrior)

| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |
|-------|---:|---:|------------:|---:|-----------:|
| even | 0.129 | 0.447 | 0.157 | 0.583 | 1.00× |
| vit_dump | 0.020 | 0.291 | 0.102 | 0.351 | 0.65× |
| res_dump | 0.065 | 0.358 | 0.125 | 0.450 | 0.80× |
| str_dump | 0.442 | 1.148 | 0.402 | 1.620 | 2.57× |
| pwr_dump | 0.185 | 0.543 | 0.190 | 0.724 | 1.21× |
| tank class | — | 0.286 | 0.100 | — | 0.64× |

| Form | hitFrac | post-DEF | softHits |
|------|--------:|---------:|---------:|
| androidforms.ssdroid4 | 0.533 | 0.187 | 0.79 |

**Result:** PASS — 66 ok, 0 error(s).
