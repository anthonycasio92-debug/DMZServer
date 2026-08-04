# AdaptiveDifficulty concept audit (1.0.17)

Fail-closed checks against the player's stated balance concept.

## 1) Tier ladder (even build)

- ✅ stock percents 21→200%
- ✅ stock tankDamageHealthRatio 0.22 — got 0.22
- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.135 T5=0.429
- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.429 T7=0.530
- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.429
- ✅ even T7 unprotected ≥40% bag — hitFrac=0.530

## 2) Dump builds feel the ladder (not shrug)

- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.347
- ✅ vit_dump T5 ≥ 60% of even — 0.347 vs even 0.429
- ✅ vit_dump T7 > T1 ×1.8 — T1=0.032 T7=0.513
- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.121
- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.429
- ✅ res_dump T5 ≥ 60% of even — 0.429 vs even 0.429
- ✅ res_dump T7 > T1 ×1.8 — T1=0.068 T7=0.530
- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.150
- ✅ str_dump T5 ≥ 28% bag — hitFrac=0.429
- ✅ str_dump T5 ≥ 60% of even — 0.429 vs even 0.429
- ✅ str_dump T7 > T1 ×1.8 — T1=0.156 T7=0.530
- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.150
- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.429
- ✅ pwr_dump T5 ≥ 60% of even — 0.429 vs even 0.429
- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.156 T7=0.530
- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.150
- ✅ tank class T5 ≥ 28% bag — hitFrac=0.350
- ✅ tank class T5 ≥ 60% of even — tank=0.350 even=0.429

## 3) Skills matter

- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.429 afterKp=0.386
- ✅ Ki Infusion sponges more pack HP — none=1341 inf=1716
- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=2309 pu=2586
- ✅ KP still saves on god form — pre=0.970 afterKp=0.873

## 4) God forms do not out-tank (incl. DMZ DEF-cancel)

- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.970 postDef~=0.340 ×54.0
- ✅ SSJG T5 post-DEF ≥12% live — pre=1.409 postDef~=0.493
- ✅ SSJG T7 does not DMZ hard-cancel — dmg=1055 flatMit=2442 ratio=2.31
- ✅ SSJG T7 landing safety-net ≥8% bag — landingFrac=0.224
- ✅ SSJB T5 post-DEF ≥12% live — pre=2.754 postDef~=0.964
- ✅ SSJB T7 does not DMZ hard-cancel — dmg=2063 flatMit=4775 ratio=2.31
- ✅ SSJB T7 landing safety-net ≥8% bag — landingFrac=0.232

## 5) Melee AD parity (feature gates)

- ✅ painted melee shock/slam
- ✅ Awakened+ chase speed
- ✅ Adaptive AI speed from Enhanced+

## 6) Version / formula revision

- ✅ VERSION 1.0.17
- ✅ formula revision 29
- ✅ hpFloorStrength present
- ✅ whitelist combat telemetry present

## Sample numbers (saiyan warrior)

| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |
|-------|---:|---:|------------:|---:|-----------:|
| even | 0.135 | 0.429 | 0.150 | 0.530 | 1.00× |
| vit_dump | 0.032 | 0.347 | 0.121 | 0.513 | 0.81× |
| res_dump | 0.068 | 0.429 | 0.150 | 0.530 | 1.00× |
| str_dump | 0.156 | 0.429 | 0.150 | 0.530 | 1.00× |
| pwr_dump | 0.156 | 0.429 | 0.150 | 0.530 | 1.00× |
| tank class | — | 0.350 | 0.122 | — | 0.82× |

| Form | hitFrac | post-DEF | softHits |
|------|--------:|---------:|---------:|
| androidforms.ssdroid4 | 0.970 | 0.340 | 1.21 |

**Result:** PASS — 42 ok, 0 error(s).
