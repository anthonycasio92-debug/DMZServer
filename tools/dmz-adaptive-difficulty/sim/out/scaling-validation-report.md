# AdaptiveDifficulty 1.0.24 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1696 vs noPWR=349
- ✅ ki pre-cap mob dmg > old STR/SKP-only — newRaw=11977 vs oldRaw=833 (both hit-capped after)
- ✅ high-VIT ki final dmg > STR/SKP-only — new=4531 vs old=3671 (capBound=True floorBound=True)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ✅ class+top2 overlay >1 at T5 for warrior — overlay=1.145 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for warrior — overlay=1.145 capped@190
- ✅ class counter bias >1 for berserker — bias=1.066
- ✅ class+top2 overlay >1 at T5 for berserker — overlay=1.151 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for berserker — overlay=1.151 capped@235
- ✅ class counter bias >1 for martialartist — bias=1.060
- ✅ class+top2 overlay >1 at T5 for martialartist — overlay=1.145 top2=SKP>VIT
- ✅ counters apply (hit-cap bound) for martialartist — overlay=1.145 capped@213
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ✅ class+top2 overlay >1 at T5 for spiritualist — overlay=1.142 top2=PWR>ENE
- ✅ counters apply (hit-cap bound) for spiritualist — overlay=1.142 capped@167
- ✅ class counter bias >1 for cleric — bias=1.054
- ✅ class+top2 overlay >1 at T5 for cleric — overlay=1.142 top2=ENE>PWR
- ✅ counters apply (hit-cap bound) for cleric — overlay=1.142 capped@235
- ✅ class counter bias >1 for paladin — bias=1.067
- ✅ class+top2 overlay >1 at T5 for paladin — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for paladin — overlay=1.178 capped@235
- ✅ class counter bias >1 for tank — bias=1.067
- ✅ class+top2 overlay >1 at T5 for tank — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for tank — overlay=1.178 capped@326

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

- ✅ T1→T2 mob dmg rises — 328 → 429 (pct 0.21→0.42)
- ✅ T2→T3 mob dmg rises — 429 → 556 (pct 0.42→0.65)
- ✅ T3→T4 mob dmg rises — 556 → 707 (pct 0.65→0.9)
- ✅ T4→T5 mob dmg rises — 707 → 733 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 733 → 783 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 783 → 884 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=328 T7=884
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.75 — hitFrac=0.546 cap=775
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=775 cap=775
- ✅ T7 berserker hitFrac ≤ 0.75 — hitFrac=0.546 cap=1102
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=1102 cap=1102
- ✅ T7 tank hitFrac ≤ 0.75 — hitFrac=0.546 cap=1539
- ✅ T7 tank mobDmg ≤ hitCap — dmg=1539 cap=1539
- ✅ T5 even-build hitCapFrac ≥ 0.35 — capFrac=0.452
- ✅ T5 even-build pressure ≥ 25% bag (KP recommended band) — hitFrac=0.452
- ✅ T5 god-form hitFrac ≥ base — base=0.452 god=0.452
- ✅ T5 god-form post-DEF ≥ 12% live bag — pre=0.452 postDef~=0.158

## 6) Archetype challenge feel

- ✅ even: T5 hitFrac > T1 — T1=0.189 T5=0.452
- ✅ even: T7 hitFrac > T5 — T5=0.452 T7=0.546 pierceBound=False
- ✅ even: T5 pressure ≥ 25% bag — pressure=0.452
- ✅ vit_dump: T5 hitFrac > T1 — T1=0.050 T5=0.441
- ✅ vit_dump: T7 hitFrac > T5 — T5=0.441 T7=0.546 pierceBound=False
- ✅ vit_dump: T5 pressure ≥ 25% bag — pressure=0.441
- ✅ res_dump: T5 hitFrac > T1 — T1=0.159 T5=0.567
- ✅ res_dump: T7 hitFrac > T5 — T5=0.567 T7=0.567 pierceBound=True
- ✅ res_dump: T5 pressure ≥ 25% bag — pressure=0.567
- ✅ str_dump: T5 hitFrac > T1 — T1=0.203 T5=0.452
- ✅ str_dump: T7 hitFrac > T5 — T5=0.452 T7=0.546 pierceBound=False
- ✅ str_dump: T5 pressure ≥ 25% bag — pressure=0.452
- ✅ extreme VIT dump HP-floor binds — floor=True cap=False dmg=897
- ✅ VIT dump T5 bag pressure ≥ 28% — hitFrac=0.444 dmg=542
- ✅ VIT dump T5 ≥ 60% of even bag pressure — vit=0.444 even=0.452
- ✅ tank class T5 bag pressure ≥ 28% — hitFrac=0.452 dmg=326
- ✅ RES dump uses DEF floor (or near-cap) — floor=True cap=True dmg=518
- ✅ STR dump pack sponge ≥ 0.35 soft hits — softHits=0.65 mobHp=1703 softOffShare=2618
- ✅ stock mobHealthScale 1.15 — got 1.15
- ✅ stock transformScaleWeight 0.65 — got 0.65

## 7) Full pack race/form sim

- ✅ discovered races — 10 races
- ✅ no hard pack flags — none

### Pack peaks (T5, m100, with counters)

| Race | Form | Top2 | Overlay | MobDmg | HitFrac | Offense |
|------|------|------|--------:|-------:|--------:|--------:|
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.151 | 7235 | 0.502 | 3249 |
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.145 | 6415 | 0.580 | 12293 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.145 | 1423 | 0.470 | 2689 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.145 | 1142 | 0.517 | 2663 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.145 | 1114 | 0.505 | 2442 |
| human | `android_enhancement.overheat` | STR>VIT | 1.145 | 424 | 0.566 | 2516 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.145 | 406 | 0.542 | 2169 |
| majin | `pureforms.ultra` | STR>VIT | 1.145 | 406 | 0.542 | 2169 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.145 | 406 | 0.542 | 2169 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.145 | 406 | 0.542 | 2169 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.145 | 190 | 1.000× | 1.000× |
| berserker | STR>VIT | 1.151 | 235 | 1.000× | 1.000× |
| martialartist | SKP>VIT | 1.145 | 213 | 1.000× | 1.000× |
| spiritualist | PWR>ENE | 1.142 | 167 | 1.000× | 1.000× |
| cleric | ENE>PWR | 1.142 | 235 | 1.000× | 1.000× |
| paladin | RES>VIT | 1.178 | 302 | 1.000× | 1.285× |
| tank | RES>VIT | 1.178 | 326 | 1.000× | 1.000× |

## Summary

**PASS** — 73 checks
