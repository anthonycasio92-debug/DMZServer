# AdaptiveDifficulty 1.0.25 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1696 vs noPWR=349
- ❌ ki pre-cap mob dmg > old STR/SKP-only — newRaw=470 vs oldRaw=471
- ✅ high-VIT ki final dmg > STR/SKP-only — new=3315 vs old=471 (capBound=False floorBound=True)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ❌ class+top2 overlay >1 at T5 for warrior — overlay=1.000 top2=STR>VIT
- ❌ counters apply (hit-cap bound) for warrior — overlay=1.000 capped@144
- ✅ class counter bias >1 for berserker — bias=1.066
- ❌ class+top2 overlay >1 at T5 for berserker — overlay=1.000 top2=STR>VIT
- ❌ counters apply (hit-cap bound) for berserker — overlay=1.000 capped@178
- ✅ class counter bias >1 for martialartist — bias=1.060
- ❌ class+top2 overlay >1 at T5 for martialartist — overlay=1.000 top2=SKP>VIT
- ❌ counters apply (hit-cap bound) for martialartist — overlay=1.000 capped@161
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ❌ class+top2 overlay >1 at T5 for spiritualist — overlay=1.000 top2=PWR>ENE
- ❌ counters apply (hit-cap bound) for spiritualist — overlay=1.000 capped@127
- ✅ class counter bias >1 for cleric — bias=1.054
- ❌ class+top2 overlay >1 at T5 for cleric — overlay=1.000 top2=ENE>PWR
- ❌ counters apply (hit-cap bound) for cleric — overlay=1.000 capped@178
- ✅ class counter bias >1 for paladin — bias=1.067
- ❌ class+top2 overlay >1 at T5 for paladin — overlay=1.000 top2=RES>VIT
- ❌ counters apply (hit-cap bound) for paladin — overlay=1.000 capped@178
- ✅ class counter bias >1 for tank — bias=1.067
- ❌ class+top2 overlay >1 at T5 for tank — overlay=1.000 top2=RES>VIT
- ✅ counters raise mob dmg for tank — with=310 noCtr=227

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
- ✅ T4→T5 mob dmg rises — 485 → 534 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 534 → 582 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 582 → 631 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=107 T7=631
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.75 — hitFrac=0.391 cap=579
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=555 cap=579
- ✅ T7 berserker hitFrac ≤ 0.75 — hitFrac=0.393 cap=823
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=793 cap=823
- ✅ T7 tank hitFrac ≤ 0.75 — hitFrac=0.490 cap=1149
- ❌ T7 tank mobDmg ≤ hitCap — dmg=1380 cap=1149
- ❌ T5 even-build hitCapFrac ≥ 0.35 — capFrac=0.337
- ✅ T5 even-build pressure ≥ 25% bag (KP recommended band) — hitFrac=0.411
- ✅ T5 god-form hitFrac ≥ base — base=0.338 god=0.329
- ❌ T5 god-form post-DEF ≥ 12% live bag — pre=0.329 postDef~=0.115

## 6) Archetype challenge feel

- ✅ even: T5 hitFrac > T1 — T1=0.067 T5=0.411
- ❌ even: T7 hitFrac > T5 — T5=0.411 T7=0.399 pierceBound=False
- ✅ even: T5 pressure ≥ 25% bag — pressure=0.411
- ✅ vit_dump: T5 hitFrac > T1 — T1=0.025 T5=0.338
- ✅ vit_dump: T7 hitFrac > T5 — T5=0.338 T7=0.399 pierceBound=False
- ✅ vit_dump: T5 pressure ≥ 25% bag — pressure=0.338
- ✅ res_dump: T5 hitFrac > T1 — T1=0.092 T5=0.429
- ✅ res_dump: T7 hitFrac > T5 — T5=0.429 T7=0.507 pierceBound=False
- ✅ res_dump: T5 pressure ≥ 25% bag — pressure=0.429
- ✅ str_dump: T5 hitFrac > T1 — T1=0.068 T5=0.338
- ✅ str_dump: T7 hitFrac > T5 — T5=0.338 T7=0.399 pierceBound=False
- ✅ str_dump: T5 pressure ≥ 25% bag — pressure=0.338
- ✅ extreme VIT dump HP-floor binds — floor=True cap=False dmg=683
- ✅ VIT dump T5 bag pressure ≥ 28% — hitFrac=0.338 dmg=412
- ✅ VIT dump T5 ≥ 58% of even bag pressure — vit=0.338 even=0.411
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
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.000 | 5399 | 0.374 | 3249 |
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.000 | 4752 | 0.430 | 12293 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.000 | 1064 | 0.351 | 2689 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.000 | 853 | 0.387 | 2663 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.000 | 832 | 0.377 | 2442 |
| human | `android_enhancement.overheat` | STR>VIT | 1.000 | 317 | 0.423 | 2516 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.000 | 303 | 0.405 | 2169 |
| majin | `pureforms.ultra` | STR>VIT | 1.000 | 303 | 0.405 | 2169 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.000 | 303 | 0.405 | 2169 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.000 | 303 | 0.405 | 2169 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.000 | 142 | 0.984× | 0.984× |
| berserker | STR>VIT | 1.000 | 176 | 0.984× | 0.984× |
| martialartist | SKP>VIT | 1.000 | 159 | 0.984× | 0.984× |
| spiritualist | PWR>ENE | 1.000 | 125 | 0.984× | 0.984× |
| cleric | ENE>PWR | 1.000 | 176 | 0.983× | 1.173× |
| paladin | RES>VIT | 1.000 | 224 | 1.254× | 1.254× |
| tank | RES>VIT | 1.000 | 310 | 1.365× | 1.477× |

## Summary

**FAIL** — 18 error(s), 55 ok
- ki pre-cap mob dmg > old STR/SKP-only: newRaw=470 vs oldRaw=471
- class+top2 overlay >1 at T5 for warrior: overlay=1.000 top2=STR>VIT
- counters apply (hit-cap bound) for warrior: overlay=1.000 capped@144
- class+top2 overlay >1 at T5 for berserker: overlay=1.000 top2=STR>VIT
- counters apply (hit-cap bound) for berserker: overlay=1.000 capped@178
- class+top2 overlay >1 at T5 for martialartist: overlay=1.000 top2=SKP>VIT
- counters apply (hit-cap bound) for martialartist: overlay=1.000 capped@161
- class+top2 overlay >1 at T5 for spiritualist: overlay=1.000 top2=PWR>ENE
- counters apply (hit-cap bound) for spiritualist: overlay=1.000 capped@127
- class+top2 overlay >1 at T5 for cleric: overlay=1.000 top2=ENE>PWR
- counters apply (hit-cap bound) for cleric: overlay=1.000 capped@178
- class+top2 overlay >1 at T5 for paladin: overlay=1.000 top2=RES>VIT
- counters apply (hit-cap bound) for paladin: overlay=1.000 capped@178
- class+top2 overlay >1 at T5 for tank: overlay=1.000 top2=RES>VIT
- T7 tank mobDmg ≤ hitCap: dmg=1380 cap=1149
- T5 even-build hitCapFrac ≥ 0.35: capFrac=0.337
- T5 god-form post-DEF ≥ 12% live bag: pre=0.329 postDef~=0.115
- even: T7 hitFrac > T5: T5=0.411 T7=0.399 pierceBound=False
