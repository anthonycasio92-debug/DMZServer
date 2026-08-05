# AdaptiveDifficulty concept audit (1.0.24)

Fail-closed checks against the player's stated balance concept.

## 1) Tier ladder (even build)

- ✅ stock percents 21→200%
- ✅ stock tankDamageHealthRatio 0.28 — got 0.28
- ✅ even T1→T5 hitFrac rises ≥1.4× — T1=0.135 T5=0.452
- ✅ even T5→T7 hitFrac rises ≥1.10× — T5=0.452 T7=0.546
- ✅ even T5 unprotected ≥25% bag (KP recommended) — hitFrac=0.452
- ✅ even T7 unprotected ≥40% bag — hitFrac=0.546

## 2) Dump builds feel the ladder (not shrug)

- ✅ vit_dump T5 ≥ 28% bag — hitFrac=0.441
- ✅ vit_dump T5 ≥ 60% of even — 0.441 vs even 0.452
- ✅ vit_dump T7 > T1 ×1.8 — T1=0.050 T7=0.546
- ✅ vit_dump T5 post-DEF ≥ 10% live — postDef~=0.154
- ✅ res_dump T5 ≥ 28% bag — hitFrac=0.452
- ✅ res_dump T5 ≥ 60% of even — 0.452 vs even 0.452
- ✅ res_dump T7 > T1 ×1.8 — T1=0.068 T7=0.546
- ✅ res_dump T5 post-DEF ≥ 10% live — postDef~=0.158
- ✅ str_dump T5 ≥ 28% bag — hitFrac=0.452
- ✅ str_dump T5 ≥ 60% of even — 0.452 vs even 0.452
- ✅ str_dump T7 > T1 ×1.8 — T1=0.203 T7=0.546
- ✅ str_dump T5 post-DEF ≥ 10% live — postDef~=0.158
- ✅ pwr_dump T5 ≥ 28% bag — hitFrac=0.452
- ✅ pwr_dump T5 ≥ 60% of even — 0.452 vs even 0.452
- ✅ pwr_dump T7 > T1 ×1.8 — T1=0.203 T7=0.546
- ✅ pwr_dump T5 post-DEF ≥ 10% live — postDef~=0.158
- ✅ tank class T5 ≥ 28% bag — hitFrac=0.445
- ✅ tank class T5 ≥ 60% of even — tank=0.445 even=0.452

## 3) Skills matter

- ✅ KP10 reduces landing dmg (pre hitFrac unchanged) — none=0.452 afterKp=0.407
- ✅ Ki Infusion sponges more pack HP — none=1341 inf=1716
- ✅ Potential Unlock sponges when transformed — androidforms.ssdroid4 none=2309 pu=2586
- ✅ KP still saves on god form — pre=0.970 afterKp=0.873

## 4) God forms do not out-tank (incl. DMZ DEF-cancel)

- ✅ god-form T5 post-DEF ≥12% (androidforms.ssdroid4) — pre=0.970 postDef~=0.340 ×54.0
- ✅ SSJG T5 post-DEF ≥12% live — pre=1.409 postDef~=0.493
- ✅ SSJG T7 does not DMZ hard-cancel — dmg=1055 flatMit=2442 ratio=2.31
- ✅ SSJG T7 landing safety-net ≥35% bag — landingFrac=0.544
- ✅ SSJB T5 post-DEF ≥12% live — pre=2.754 postDef~=0.964
- ✅ SSJB T7 does not DMZ hard-cancel — dmg=2063 flatMit=4775 ratio=2.31
- ✅ SSJB T7 landing safety-net ≥35% bag — landingFrac=0.553

## 5) Melee AD parity (feature gates)

- ✅ painted melee shock/slam
- ✅ Awakened+ chase speed
- ✅ Adaptive AI speed from Enhanced+

## 6) Version / formula revision

- ✅ VERSION 1.0.24
- ✅ formula revision 32
- ✅ hpFloorStrength present
- ✅ T1–T3 god-form floors raised
- ✅ T7 incoming soft-cap in events
- ✅ T5 soft-cap ≤50%
- ✅ whitelist combat telemetry present
- ✅ god-form landing T1≥10% — landingFrac=0.122
- ✅ god-form landing T5≥28% — landingFrac=0.421
- ✅ god-form landing T7≥40% — landingFrac=0.553
- ✅ god-form landing T7>T1×2.5 — T1=0.122 T7=0.553

## Sample numbers (saiyan warrior)

| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |
|-------|---:|---:|------------:|---:|-----------:|
| even | 0.135 | 0.452 | 0.158 | 0.546 | 1.00× |
| vit_dump | 0.050 | 0.441 | 0.154 | 0.546 | 0.97× |
| res_dump | 0.068 | 0.452 | 0.158 | 0.546 | 1.00× |
| str_dump | 0.203 | 0.452 | 0.158 | 0.546 | 1.00× |
| pwr_dump | 0.203 | 0.452 | 0.158 | 0.546 | 1.00× |
| tank class | — | 0.445 | 0.156 | — | 0.98× |

| Form | hitFrac | post-DEF | softHits |
|------|--------:|---------:|---------:|
| androidforms.ssdroid4 | 0.970 | 0.340 | 1.21 |

**Result:** PASS — 49 ok, 0 error(s).
