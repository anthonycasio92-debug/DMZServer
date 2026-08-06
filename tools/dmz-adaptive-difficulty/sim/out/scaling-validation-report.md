# AdaptiveDifficulty 1.0.25 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1696 vs noPWR=349
- ✅ ki pre-cap mob dmg > old STR/SKP-only — newRaw=13973 vs oldRaw=924 (both hit-capped after)
- ✅ high-VIT ki final dmg > STR/SKP-only — new=4687 vs old=3671 (capBound=True floorBound=True)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ✅ class+top2 overlay >1 at T5 for warrior — overlay=1.145 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for warrior — overlay=1.145 capped@197
- ✅ class counter bias >1 for berserker — bias=1.066
- ✅ class+top2 overlay >1 at T5 for berserker — overlay=1.151 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for berserker — overlay=1.151 capped@243
- ✅ class counter bias >1 for martialartist — bias=1.060
- ✅ class+top2 overlay >1 at T5 for martialartist — overlay=1.145 top2=SKP>VIT
- ✅ counters apply (hit-cap bound) for martialartist — overlay=1.145 capped@220
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ✅ class+top2 overlay >1 at T5 for spiritualist — overlay=1.142 top2=PWR>ENE
- ✅ counters apply (hit-cap bound) for spiritualist — overlay=1.142 capped@173
- ✅ class counter bias >1 for cleric — bias=1.054
- ✅ class+top2 overlay >1 at T5 for cleric — overlay=1.142 top2=ENE>PWR
- ✅ counters apply (hit-cap bound) for cleric — overlay=1.142 capped@243
- ✅ class counter bias >1 for paladin — bias=1.067
- ✅ class+top2 overlay >1 at T5 for paladin — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for paladin — overlay=1.178 capped@243
- ✅ class counter bias >1 for tank — bias=1.067
- ✅ class+top2 overlay >1 at T5 for tank — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for tank — overlay=1.178 capped@337

## 3) Top-2 stats

- ✅ top-2 for warrior — got STR>VIT
- ✅ top-2 for berserker — got STR>VIT
- ✅ top-2 for martialartist — got SKP>VIT
- ✅ top-2 for spiritualist — got PWR>ENE
- ✅ top-2 for cleric — got ENE>PWR
- ✅ top-2 for paladin — got RES>VIT
- ✅ top-2 for tank — got RES>VIT
- ✅ top-2 secondary adds pressure — top1=1.0520 top2=1.0832 (secondary×0.6)

## 4) Tier ladder

- ✅ T1→T2 mob dmg rises — 379 → 467 (pct 0.21→0.42)
- ✅ T2→T3 mob dmg rises — 467 → 568 (pct 0.42→0.65)
- ✅ T3→T4 mob dmg rises — 568 → 707 (pct 0.65→0.9)
- ✅ T4→T5 mob dmg rises — 707 → 758 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 758 → 859 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 859 → 935 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=379 T7=935
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.75 — hitFrac=0.577 cap=819
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=819 cap=819
- ✅ T7 berserker hitFrac ≤ 0.75 — hitFrac=0.577 cap=1165
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=1165 cap=1165
- ✅ T7 tank hitFrac ≤ 0.75 — hitFrac=0.577 cap=1627
- ✅ T7 tank mobDmg ≤ hitCap — dmg=1627 cap=1627
- ✅ T5 even-build hitCapFrac ≥ 0.35 — capFrac=0.468
- ✅ T5 even-build pressure ≥ 25% bag (KP recommended band) — hitFrac=0.468
- ✅ T5 god-form hitFrac ≥ base — base=0.468 god=0.468
- ✅ T5 god-form post-DEF ≥ 12% live bag — pre=0.468 postDef~=0.164

## 6) Archetype challenge feel

- ✅ even: T5 hitFrac > T1 — T1=0.189 T5=0.468
- ✅ even: T7 hitFrac > T5 — T5=0.468 T7=0.577 pierceBound=False
- ✅ even: T5 pressure ≥ 25% bag — pressure=0.468
- ✅ vit_dump: T5 hitFrac > T1 — T1=0.050 T5=0.441
- ✅ vit_dump: T7 hitFrac > T5 — T5=0.441 T7=0.577 pierceBound=False
- ✅ vit_dump: T5 pressure ≥ 25% bag — pressure=0.441
- ✅ res_dump: T5 hitFrac > T1 — T1=0.159 T5=0.520
- ✅ res_dump: T7 hitFrac > T5 — T5=0.520 T7=0.620 pierceBound=False
- ✅ res_dump: T5 pressure ≥ 25% bag — pressure=0.520
- ✅ str_dump: T5 hitFrac > T1 — T1=0.234 T5=0.468
- ✅ str_dump: T7 hitFrac > T5 — T5=0.468 T7=0.577 pierceBound=False
- ✅ str_dump: T5 pressure ≥ 25% bag — pressure=0.468
- ✅ extreme VIT dump HP-floor binds — floor=True cap=False dmg=897
- ✅ VIT dump T5 bag pressure ≥ 28% — hitFrac=0.444 dmg=542
- ✅ VIT dump T5 ≥ 60% of even bag pressure — vit=0.444 even=0.468
- ✅ tank class T5 bag pressure ≥ 28% — hitFrac=0.468 dmg=337
- ✅ RES dump uses DEF floor (or near-cap) — floor=True cap=True dmg=166
- ✅ STR dump pack sponge ≥ 0.35 soft hits — softHits=0.42 mobHp=1111 softOffShare=2618
- ✅ stock mobHealthScale 0.75 — got 0.75
- ✅ stock transformScaleWeight 0.65 — got 0.65

## 7) Full pack race/form sim

- ✅ discovered races — 10 races
- ✅ no hard pack flags — none

### Pack peaks (T5, m100, with counters)

| Race | Form | Top2 | Overlay | MobDmg | HitFrac | Offense |
|------|------|------|--------:|-------:|--------:|--------:|
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.151 | 7485 | 0.519 | 3249 |
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.145 | 5751 | 0.520 | 12293 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.145 | 1472 | 0.486 | 2689 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.145 | 1148 | 0.520 | 2663 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.145 | 1148 | 0.520 | 2442 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.145 | 389 | 0.520 | 2169 |
| human | `android_enhancement.overheat` | STR>VIT | 1.145 | 389 | 0.520 | 2516 |
| majin | `pureforms.ultra` | STR>VIT | 1.145 | 389 | 0.520 | 2169 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.145 | 389 | 0.520 | 2169 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.145 | 389 | 0.520 | 2169 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.145 | 197 | 1.000× | 1.000× |
| berserker | STR>VIT | 1.151 | 243 | 1.000× | 1.000× |
| martialartist | SKP>VIT | 1.145 | 220 | 1.000× | 1.000× |
| spiritualist | PWR>ENE | 1.142 | 173 | 1.000× | 1.000× |
| cleric | ENE>PWR | 1.142 | 243 | 1.000× | 1.003× |
| paladin | RES>VIT | 1.178 | 270 | 1.000× | 1.111× |
| tank | RES>VIT | 1.178 | 337 | 1.000× | 1.000× |

## Summary

**PASS** — 73 checks
