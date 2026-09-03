# AdaptiveDifficulty 1.0.25 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1696 vs noPWR=349
- ✅ ki pre-cap mob dmg > old STR/SKP-only — newRaw=14771 vs oldRaw=956 (both hit-capped after)
- ✅ high-VIT ki final dmg > STR/SKP-only — new=3437 vs old=3437 (capBound=True floorBound=True)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ✅ class+top2 overlay >1 at T5 for warrior — overlay=1.145 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for warrior — overlay=1.145 capped@144
- ✅ class counter bias >1 for berserker — bias=1.066
- ✅ class+top2 overlay >1 at T5 for berserker — overlay=1.151 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for berserker — overlay=1.151 capped@178
- ✅ class counter bias >1 for martialartist — bias=1.060
- ✅ class+top2 overlay >1 at T5 for martialartist — overlay=1.145 top2=SKP>VIT
- ✅ counters apply (hit-cap bound) for martialartist — overlay=1.145 capped@161
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ✅ class+top2 overlay >1 at T5 for spiritualist — overlay=1.142 top2=PWR>ENE
- ✅ counters apply (hit-cap bound) for spiritualist — overlay=1.142 capped@127
- ✅ class counter bias >1 for cleric — bias=1.054
- ✅ class+top2 overlay >1 at T5 for cleric — overlay=1.142 top2=ENE>PWR
- ✅ counters apply (hit-cap bound) for cleric — overlay=1.142 capped@178
- ✅ class counter bias >1 for paladin — bias=1.067
- ✅ class+top2 overlay >1 at T5 for paladin — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for paladin — overlay=1.178 capped@178
- ✅ class counter bias >1 for tank — bias=1.067
- ✅ class+top2 overlay >1 at T5 for tank — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for tank — overlay=1.178 capped@247

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

- ✅ T1→T2 mob dmg rises — 278 → 354 (pct 0.21→0.42)
- ✅ T2→T3 mob dmg rises — 354 → 518 (pct 0.42→0.65)
- ❌ T3→T4 mob dmg rises — 518 → 505 (pct 0.65→0.9)
- ✅ T4→T5 mob dmg rises — 505 → 556 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 556 → 606 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 606 → 657 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=278 T7=657
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.75 — hitFrac=0.405 cap=576
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=576 cap=576
- ✅ T7 berserker hitFrac ≤ 0.75 — hitFrac=0.405 cap=819
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=819 cap=819
- ✅ T7 tank hitFrac ≤ 0.75 — hitFrac=0.490 cap=1143
- ❌ T7 tank mobDmg ≤ hitCap — dmg=1382 cap=1143
- ❌ T5 even-build hitCapFrac ≥ 0.35 — capFrac=0.343
- ✅ T5 even-build pressure ≥ 25% bag (KP recommended band) — hitFrac=0.411
- ✅ T5 god-form hitFrac ≥ base — base=0.343 god=0.343
- ✅ T5 god-form post-DEF ≥ 12% live bag — pre=0.343 postDef~=0.120

## 6) Archetype challenge feel

- ✅ even: T5 hitFrac > T1 — T1=0.172 T5=0.411
- ❌ even: T7 hitFrac > T5 — T5=0.411 T7=0.406 pierceBound=False
- ✅ even: T5 pressure ≥ 25% bag — pressure=0.411
- ✅ vit_dump: T5 hitFrac > T1 — T1=0.050 T5=0.343
- ✅ vit_dump: T7 hitFrac > T5 — T5=0.343 T7=0.406 pierceBound=False
- ✅ vit_dump: T5 pressure ≥ 25% bag — pressure=0.343
- ✅ res_dump: T5 hitFrac > T1 — T1=0.159 T5=0.520
- ✅ res_dump: T7 hitFrac > T5 — T5=0.520 T7=0.620 pierceBound=False
- ✅ res_dump: T5 pressure ≥ 25% bag — pressure=0.520
- ✅ str_dump: T5 hitFrac > T1 — T1=0.172 T5=0.343
- ✅ str_dump: T7 hitFrac > T5 — T5=0.343 T7=0.406 pierceBound=False
- ✅ str_dump: T5 pressure ≥ 25% bag — pressure=0.343
- ✅ extreme VIT dump HP-floor binds — floor=True cap=True dmg=693
- ✅ VIT dump T5 bag pressure ≥ 28% — hitFrac=0.343 dmg=419
- ✅ VIT dump T5 ≥ 60% of even bag pressure — vit=0.343 even=0.411
- ✅ tank class T5 bag pressure ≥ 28% — hitFrac=0.480 dmg=346
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
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.151 | 5489 | 0.381 | 3249 |
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.145 | 4866 | 0.440 | 12293 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.145 | 1080 | 0.357 | 2689 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.145 | 866 | 0.393 | 2663 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.145 | 845 | 0.383 | 2442 |
| human | `android_enhancement.overheat` | STR>VIT | 1.145 | 322 | 0.430 | 2516 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.145 | 308 | 0.411 | 2169 |
| majin | `pureforms.ultra` | STR>VIT | 1.145 | 308 | 0.411 | 2169 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.145 | 308 | 0.411 | 2169 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.145 | 308 | 0.411 | 2169 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.145 | 144 | 1.000× | 1.000× |
| berserker | STR>VIT | 1.151 | 178 | 1.000× | 1.000× |
| martialartist | SKP>VIT | 1.145 | 161 | 1.000× | 1.000× |
| spiritualist | PWR>ENE | 1.142 | 127 | 1.000× | 1.000× |
| cleric | ENE>PWR | 1.142 | 178 | 1.000× | 1.000× |
| paladin | RES>VIT | 1.178 | 270 | 1.000× | 1.515× |
| tank | RES>VIT | 1.178 | 346 | 1.000× | 1.399× |

## Summary

**FAIL** — 4 error(s), 69 ok
- T3→T4 mob dmg rises: 518 → 505 (pct 0.65→0.9)
- T7 tank mobDmg ≤ hitCap: dmg=1382 cap=1143
- T5 even-build hitCapFrac ≥ 0.35: capFrac=0.343
- even: T7 hitFrac > T5: T5=0.411 T7=0.406 pierceBound=False
