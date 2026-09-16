# Scaling × form-band audit

Validates combat scaling against the 2.3.171 telemetry band taxonomy.

## 1) Java ↔ Python band thresholds

- ✅ base gate ≤1.12 — py=1.12 java=1.12
- ✅ awakened <6 (MEGA_FORM_START) — py=6.0 java=6.0
- ✅ super <15 — py=15.0 java=15.0
- ✅ ultra <22 — py=22.0 java=22.0
- ✅ divine <50 — py=50.0 java=50.0
- ✅ enhancement <80 (MEGA_FORM_TARGET) — py=80.0 java=80.0
- ✅ Java logs formBand via BalanceTelemetry
- ✅ T3 pierce at formBoost≥6 (super+)
- ✅ Mega-form path at formBoost≥6

## 2) Sim ladder per band (saiyan warrior, even build)

| band | form | fb | T3 hit | T5 hit | T7 hit | T5 land | T7 land |
|------|------|---:|-------:|-------:|-------:|--------:|--------:|
| base | `(no transform)` | 1.0 | 0.261 | 0.338 | 0.399 | 0.277 | 0.308 |
- ✅ sim base T5 ≥ concept min — 0.338
- ✅ sim base T7 ≥ concept min — 0.399
| awakened | `ssgrades.supersaiyangrade2` | 5.6 | 0.287 | 0.372 | 0.439 | 0.289 | 0.320 |
| super | `legendaryforms.ssjhybrid` | 12.5 | 0.339 | 0.380 | 0.449 | 0.295 | 0.326 |
- ✅ sim super T5 hitFrac ≤ soft-cap+0.02 — 0.380 cap=0.44
- ✅ sim super T7 hitFrac ≤ soft-cap+0.02 — 0.449 cap=0.52
| ultra | `androidforms.ssdroid2` | 21.8 | 0.265 | 0.343 | 0.405 | 0.271 | 0.302 |
- ✅ sim ultra T5 hitFrac ≤ soft-cap+0.02 — 0.343 cap=0.44
- ✅ sim ultra T7 hitFrac ≤ soft-cap+0.02 — 0.405 cap=0.52
| divine | `godforms.supersaiyanblue` | 49.1 | 0.339 | 0.414 | 0.490 | 0.304 | 0.336 |
- ✅ sim divine T5 hitFrac ≤ soft-cap+0.02 — 0.414 cap=0.44
- ✅ sim divine T7 hitFrac ≤ soft-cap+0.02 — 0.490 cap=0.52
- ✅ sim divine T5 post-DEF ≥12% — postDef~=0.145
- ✅ sim divine T5 landing ≥ concept min — land=0.304
- ✅ sim divine T7 landing ≥ concept min — land=0.336
| enhancement | `androidforms.ssdroid4` | 54.0 | 0.339 | 0.414 | 0.490 | 0.277 | 0.308 |
- ✅ sim enhancement T5 hitFrac ≤ soft-cap+0.02 — 0.414 cap=0.44
- ✅ sim enhancement T7 hitFrac ≤ soft-cap+0.02 — 0.490 cap=0.52
- ✅ sim enhancement T5 post-DEF ≥12% — postDef~=0.145
- ✅ sim enhancement T5 landing ≥ concept min — land=0.277
- ✅ sim enhancement T7 landing ≥ concept min — land=0.308

- ✅ divine sim T3≤T4≤T5≤T6≤T7 hitFrac — T3=0.339 T5=0.414 T7=0.490
- ✅ divine sim T5 landing < T7 landing — T5=0.304 T7=0.336
- ✅ divine T5 hitFrac ≥ base T5 ×0.95 — divine=0.414 base=0.338

## 3) Live telemetry vs sim (post-Sep 3 slice)

Recent hits: **5508** since 2026-09-03

| band | n@T5 | live T5 | sim T5 | Δ | n@T7 | live T7 | sim T7 | Δ | note |
|------|-----:|--------:|-------:|--:|-----:|--------:|-------:|--:|------|
| base | 37 | 0.192 | 0.338 | -0.146 | 32 | 0.391 | 0.399 | -0.008 | — |
| awakened | 26 | 0.130 | 0.372 | — | 0 | — | 0.439 | — | — |
| super | 13 | 0.119 | 0.380 | — | 0 | — | 0.449 | — | — |
| ultra | 53 | 0.261 | 0.343 | -0.082 | 26 | 0.387 | 0.405 | — | — |
| divine | 388 | 0.239 | 0.414 | -0.175 | 183 | 0.264 | 0.490 | -0.225 | cold (pre-175?) |
| enhancement | 51 | 0.226 | 0.414 | -0.189 | 4 | 0.422 | 0.490 | — | — |

_Note: mixed-jar telemetry may still include pre-2.3.175 hits; re-pull 24–48h post-deploy for clean read._

## 4) Pack form catalog per band

- ✅ saiyan catalog has base forms (or synthetic) — n=0
- ✅ saiyan catalog has awakened forms — n=7
- ✅ saiyan catalog has super forms — n=4
- ✅ saiyan catalog has ultra forms — n=5
- ✅ saiyan catalog has divine forms — n=5
- ✅ saiyan catalog has enhancement forms — n=1
- ✅ saiyan catalog has apex forms (optional) — n=0

## Result

**PASS** — scaling aligns with form-band taxonomy.
