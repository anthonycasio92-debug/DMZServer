# AdaptiveDifficulty 1.0.25 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1696 vs noPWR=349
- ✅ ki pre-cap mob dmg > old STR/SKP-only — newShare=2290 vs oldShare=471
- ✅ high-VIT ki final dmg > STR/SKP-only — new=3465 vs old=471 (capBound=False floorBound=True)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ✅ class+top2 overlay >1 at T5 for warrior — overlay=1.145 top2=STR>VIT
- ✅ counters raise mob dmg for warrior — with=148 noCtr=129
- ✅ class counter bias >1 for berserker — bias=1.066
- ✅ class+top2 overlay >1 at T5 for berserker — overlay=1.151 top2=STR>VIT
- ✅ counters raise mob dmg for berserker — with=184 noCtr=160
- ✅ class counter bias >1 for martialartist — bias=1.060
- ✅ class+top2 overlay >1 at T5 for martialartist — overlay=1.145 top2=SKP>VIT
- ✅ counters raise mob dmg for martialartist — with=166 noCtr=145
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ✅ class+top2 overlay >1 at T5 for spiritualist — overlay=1.142 top2=PWR>ENE
- ✅ counters raise mob dmg for spiritualist — with=131 noCtr=114
- ✅ class counter bias >1 for cleric — bias=1.054
- ✅ class+top2 overlay >1 at T5 for cleric — overlay=1.142 top2=ENE>PWR
- ✅ counters raise mob dmg for cleric — with=183 noCtr=161
- ✅ class counter bias >1 for paladin — bias=1.067
- ✅ class+top2 overlay >1 at T5 for paladin — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for paladin — overlay=1.178 capped@183
- ✅ class counter bias >1 for tank — bias=1.067
- ✅ class+top2 overlay >1 at T5 for tank — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for tank — overlay=1.178 capped@253

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

- ✅ T1→T2 mob dmg rises — 107 → 145 (pct 0.21→0.42)
- ✅ T2→T3 mob dmg rises — 145 → 494 (pct 0.42→0.65)
- ✅ T3→T4 mob dmg rises — 494 → 485 (pct 0.65→0.9)
- ✅ T4→T5 mob dmg rises — 485 → 558 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 558 → 582 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 582 → 679 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=107 T7=679
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.75 — hitFrac=0.421 cap=598
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=598 bagCap=816
- ✅ T7 berserker hitFrac ≤ 0.75 — hitFrac=0.423 cap=854
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=854 bagCap=1166
- ✅ T7 tank hitFrac ≤ 0.75 — hitFrac=0.490 cap=1171
- ✅ T7 tank mobDmg ≤ hitCap — dmg=1382 bagCap=1593
- ✅ T5 even-build hitCapFrac ≥ 0.35 — capFrac=0.353
- ✅ T5 even-build pressure ≥ 25% bag (KP recommended band) — hitFrac=0.411
- ✅ T5 god-form hitFrac ≥ base — base=0.353 god=0.344
- ✅ T5 god-form post-DEF ≥ 12% live bag — pre=0.344 postDef~=0.121

## 6) Archetype challenge feel

- ✅ even: T5 hitFrac > T1 — T1=0.128 T5=0.353
- ✅ even: T7 hitFrac > T5 — T5=0.353 T7=0.430 pierceBound=False
- ✅ even: T5 pressure ≥ 25% bag — pressure=0.353
- ✅ vit_dump: T5 hitFrac > T1 — T1=0.019 T5=0.353
- ✅ vit_dump: T7 hitFrac > T5 — T5=0.353 T7=0.430 pierceBound=False
- ✅ vit_dump: T5 pressure ≥ 25% bag — pressure=0.353
- ✅ res_dump: T5 hitFrac > T1 — T1=0.062 T5=0.353
- ✅ res_dump: T7 hitFrac > T5 — T5=0.353 T7=0.430 pierceBound=False
- ✅ res_dump: T5 pressure ≥ 25% bag — pressure=0.353
- ✅ str_dump: T5 hitFrac > T1 — T1=0.068 T5=0.353
- ✅ str_dump: T7 hitFrac > T5 — T5=0.353 T7=0.430 pierceBound=False
- ✅ str_dump: T5 pressure ≥ 25% bag — pressure=0.353
- ✅ extreme VIT dump HP-floor binds — floor=True cap=False dmg=714
- ✅ VIT dump T5 bag pressure ≥ 28% — hitFrac=0.353 dmg=431
- ✅ VIT dump T5 ≥ 58% of even bag pressure — vit=0.353 even=0.411
- ✅ tank class T5 bag pressure ≥ 28% — hitFrac=0.430 dmg=310
- ✅ RES dump uses DEF floor (or near-cap) — floor=False cap=True dmg=137
- ✅ STR dump pack sponge ≥ 0.35 soft hits — softHits=0.42 mobHp=1111 softOffShare=2618
- ✅ stock mobHealthScale 0.75 — got 0.75
- ✅ stock transformScaleWeight 0.65 — got 0.65

## 7) Full pack race/form sim

- ✅ discovered races — 10 races
- ✅ no hard pack flags — none

### Pack peaks (T5, m100, with counters)

| Race | Form | Top2 | Overlay | MobDmg | HitFrac | Offense |
|------|------|------|--------:|-------:|--------:|--------:|
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.151 | 5644 | 0.391 | 3249 |
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.145 | 4752 | 0.430 | 12293 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.145 | 1112 | 0.367 | 2689 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.145 | 892 | 0.404 | 2663 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.145 | 870 | 0.394 | 2442 |
| human | `android_enhancement.overheat` | STR>VIT | 1.145 | 324 | 0.432 | 2516 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.145 | 317 | 0.423 | 2169 |
| majin | `pureforms.ultra` | STR>VIT | 1.145 | 317 | 0.423 | 2169 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.145 | 317 | 0.423 | 2169 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.145 | 317 | 0.423 | 2169 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.145 | 148 | 1.145× | 0.984× |
| berserker | STR>VIT | 1.151 | 184 | 1.151× | 0.984× |
| martialartist | SKP>VIT | 1.145 | 166 | 1.145× | 0.984× |
| spiritualist | PWR>ENE | 1.142 | 131 | 1.142× | 0.984× |
| cleric | ENE>PWR | 1.142 | 183 | 1.142× | 1.226× |
| paladin | RES>VIT | 1.178 | 224 | 1.178× | 1.199× |
| tank | RES>VIT | 1.178 | 310 | 1.178× | 1.477× |

## Summary

**PASS** — 73 checks
