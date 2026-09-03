# LegacyMechanics concept audit (2.3.131)

Fail-closed checks against the player's stated balance concept.

## 1) Tier ladder (even build)

- ✅ stock percents 21→200%
- ✅ stock tankDamageHealthRatio 0.28 — got 0.28
- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.129 T5=0.343
- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.343 T7=0.406
- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.343
- ✅ even T7 unprotected ≥40% bag — hitFrac=0.406

## 2) Dump builds feel the ladder (not shrug)

- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.343
- ✅ vit_dump T5 ≥ 58% of even — 0.343 vs even 0.343
- ✅ vit_dump T7 > T1 ×1.8 — T1=0.020 T7=0.406
- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.120
- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.343
- ✅ res_dump T5 ≥ 58% of even — 0.343 vs even 0.343
- ✅ res_dump T7 > T1 ×1.8 — T1=0.065 T7=0.406
- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.120
- ✅ str_dump T5 ≥ 28% bag — hitFrac=0.343
- ✅ str_dump T5 ≥ 58% of even — 0.343 vs even 0.343
- ✅ str_dump T7 > T1 ×1.8 — T1=0.069 T7=0.406
- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.120
- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.343
- ✅ pwr_dump T5 ≥ 58% of even — 0.343 vs even 0.343
- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.069 T7=0.406
- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.120
- ✅ tank class T5 ≥ 28% bag — hitFrac=0.343
- ✅ tank class T5 ≥ 58% of even — tank=0.343 even=0.343

## 3) Skills matter

- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.343 afterKp=0.309
- ✅ Ki Infusion sponges more pack HP — none=874 inf=1119
- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=1506 pu=1686
- ✅ KP still saves on god form — pre=0.440 afterKp=0.396

## 4) God forms do not out-tank (incl. DMZ DEF-cancel)

- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.440 postDef~=0.154 ×54.0
- ✅ SSJG T5 post-DEF ≥12% live — pre=0.440 postDef~=0.154
- ✅ SSJG T7 clears cancel or landing ≥30% — wouldCancel=True dmg=389 flatMit=2442 landingFrac=0.440
- ✅ SSJG T7 landing safety-net ≥30% bag — landingFrac=0.440
- ✅ SSJB T5 post-DEF ≥12% live — pre=0.440 postDef~=0.154
- ✅ SSJB T7 clears cancel or landing ≥30% — wouldCancel=True dmg=389 flatMit=4775 landingFrac=0.440
- ✅ SSJB T7 landing safety-net ≥30% bag — landingFrac=0.440

## 5) Melee AD parity (feature gates)

- ✅ painted melee shock/slam
- ✅ Awakened+ chase speed
- ✅ Adaptive AI speed from Enhanced+

## 6) Version / formula revision

- ✅ VERSION 2.3.162
- ✅ RaceSkillSync present
- ✅ formula revision 44
- ✅ paintEase cap-path split
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
- ✅ god soft-cap ladder T3≤T4 — T3=0.343 T4=0.400
- ✅ god soft-cap ladder T4≤T5 — T4=0.400 T5=0.440
- ✅ god soft-cap ladder T5≤T6 — T5=0.440 T6=0.480
- ✅ god soft-cap ladder T6≤T7 — T6=0.480 T7=0.520
- ✅ god landing T5 < T6 (buy matters) — T5=0.360 T6=0.400
- ✅ god landing T4 < T5 — T4=0.211 T5=0.360
- ✅ god landing ≤ soft-cap T5 — landing=0.360
- ✅ god landing ≤ soft-cap T6 — landing=0.400
- ✅ combat telemetry present
- ✅ god-form landing T1≥3% — landingFrac=0.034
- ✅ god-form landing T5≥22% — landingFrac=0.360
- ✅ god-form landing T7≥30% — landingFrac=0.440
- ✅ god-form landing T7>T1×2.5 — T1=0.034 T7=0.440

## Sample numbers (saiyan warrior)

| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |
|-------|---:|---:|------------:|---:|-----------:|
| even | 0.129 | 0.343 | 0.120 | 0.406 | 1.00× |
| vit_dump | 0.020 | 0.343 | 0.120 | 0.406 | 1.00× |
| res_dump | 0.065 | 0.343 | 0.120 | 0.406 | 1.00× |
| str_dump | 0.069 | 0.343 | 0.120 | 0.406 | 1.00× |
| pwr_dump | 0.069 | 0.343 | 0.120 | 0.406 | 1.00× |
| tank class | — | 0.343 | 0.120 | — | 1.00× |

| Form | hitFrac | post-DEF | softHits |
|------|--------:|---------:|---------:|
| androidforms.ssdroid4 | 0.440 | 0.154 | 0.79 |

**Result:** PASS — 67 ok, 0 error(s).
