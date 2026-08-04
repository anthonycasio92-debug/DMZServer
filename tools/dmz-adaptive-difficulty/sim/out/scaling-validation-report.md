# AdaptiveDifficulty 1.0.11 scaling validation


## 1) PWR/ENE in offense

- ✅ ki offense > STR/SKP-only offense — full=1332 vs noPWR=285
- ✅ ki pre-cap mob dmg > old STR/SKP-only — newRaw=2177 vs oldRaw=465 (both hit-capped after)
- ✅ high-VIT ki final dmg > STR/SKP-only — new=2193 vs old=469 (capBound=False)
- ✅ high ENE raises soft offense — highENE=2684 vs lowENE=90

## 2) Class counters

- ✅ class counter bias >1 for warrior — bias=1.060
- ✅ class+top2 overlay >1 at T5 for warrior — overlay=1.145 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for warrior — overlay=1.145 capped@69
- ✅ class counter bias >1 for berserker — bias=1.066
- ✅ class+top2 overlay >1 at T5 for berserker — overlay=1.151 top2=STR>VIT
- ✅ counters apply (hit-cap bound) for berserker — overlay=1.151 capped@86
- ✅ class counter bias >1 for martialartist — bias=1.060
- ✅ class+top2 overlay >1 at T5 for martialartist — overlay=1.145 top2=SKP>VIT
- ✅ counters apply (hit-cap bound) for martialartist — overlay=1.145 capped@78
- ✅ class counter bias >1 for spiritualist — bias=1.054
- ✅ class+top2 overlay >1 at T5 for spiritualist — overlay=1.142 top2=PWR>ENE
- ✅ counters apply (hit-cap bound) for spiritualist — overlay=1.142 capped@61
- ✅ class counter bias >1 for cleric — bias=1.054
- ✅ class+top2 overlay >1 at T5 for cleric — overlay=1.142 top2=ENE>PWR
- ✅ counters apply (hit-cap bound) for cleric — overlay=1.142 capped@86
- ✅ class counter bias >1 for paladin — bias=1.067
- ✅ class+top2 overlay >1 at T5 for paladin — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for paladin — overlay=1.178 capped@86
- ✅ class counter bias >1 for tank — bias=1.067
- ✅ class+top2 overlay >1 at T5 for tank — overlay=1.178 top2=RES>VIT
- ✅ counters apply (hit-cap bound) for tank — overlay=1.178 capped@119

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

- ✅ T1→T2 mob dmg rises — 76 → 107 (pct 0.21→0.42)
- ✅ T2→T3 mob dmg rises — 107 → 137 (pct 0.42→0.65)
- ✅ T3→T4 mob dmg rises — 137 → 183 (pct 0.65→0.9)
- ✅ T4→T5 mob dmg rises — 183 → 228 (pct 0.9→1.35)
- ✅ T5→T6 mob dmg rises — 228 → 259 (pct 1.35→1.6)
- ✅ T6→T7 mob dmg rises — 259 → 289 (pct 1.6→2.0)
- ✅ T7 >> T1 pressure — T1=76 T7=289
- ✅ stock percents

## 5) Hit cap / safety

- ✅ T7 spiritualist hitFrac ≤ 0.42 — hitFrac=0.178 cap=253
- ✅ T7 spiritualist mobDmg ≤ hitCap — dmg=253 cap=253
- ✅ T7 berserker hitFrac ≤ 0.42 — hitFrac=0.178 cap=361
- ✅ T7 berserker mobDmg ≤ hitCap — dmg=361 cap=361
- ✅ T7 tank hitFrac ≤ 0.42 — hitFrac=0.178 cap=503
- ✅ T7 tank mobDmg ≤ hitCap — dmg=503 cap=503

## 6) Full pack race/form sim

- ✅ discovered races — 10 races
- ✅ no hard pack flags — none

### Pack peaks (T5, m100, with counters)

| Race | Form | Top2 | Overlay | MobDmg | HitFrac | Offense |
|------|------|------|--------:|-------:|--------:|--------:|
| ancient_saiyan | `primalssj.primalgod` | STR>VIT | 1.145 | 3318 | 0.300 | 8779 |
| sento_saiyan | `ancestral_bloodline.ancestral_justice` | STR>VIT | 1.151 | 2363 | 0.164 | 2429 |
| viltrumite | `androidforms.conquestfull` | STR>VIT | 1.145 | 412 | 0.136 | 2012 |
| frostdemon | `android_enhancement_a.metal_overheat` | STR>VIT | 1.145 | 363 | 0.164 | 1908 |
| saiyan | `androidforms.ssdroid4` | STR>VIT | 1.145 | 362 | 0.164 | 1805 |
| human | `android_enhancement.overheat` | STR>VIT | 1.145 | 214 | 0.286 | 1842 |
| bioandroid | `legendaryforms.xenomax` | STR>VIT | 1.145 | 195 | 0.260 | 1675 |
| majin | `pureforms.ultra` | STR>VIT | 1.145 | 195 | 0.260 | 1675 |
| monkey | `oozaru.wukongzero` | STR>VIT | 1.145 | 195 | 0.260 | 1675 |
| namekian | `superforms.supernamekian` | STR>VIT | 1.145 | 195 | 0.260 | 1675 |

### Class matrix (T5 base form)

| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |
|-------|------|--------:|-------:|--------------:|----------:|
| warrior | STR>VIT | 1.145 | 69 | 1.000× | 1.000× |
| berserker | STR>VIT | 1.151 | 86 | 1.000× | 1.000× |
| martialartist | SKP>VIT | 1.145 | 78 | 1.000× | 1.000× |
| spiritualist | PWR>ENE | 1.142 | 61 | 1.000× | 1.000× |
| cleric | ENE>PWR | 1.142 | 86 | 1.000× | 1.000× |
| paladin | RES>VIT | 1.178 | 86 | 1.000× | 1.000× |
| tank | RES>VIT | 1.178 | 119 | 1.000× | 1.000× |

## Summary

**PASS** — 49 checks
