# AdaptiveDifficulty build matrix (1.0.15)

Race × class × archetype × skill loadout × tier.
Skills: kiprotection / ki_infusion / potentialunlock (DMZ combat.json rates).

## Concept checklist

- ✅ human even T1→T5 hitFrac rises
- ✅ human even T5→T7 hitFrac rises
- ✅ human even T4+ unprotected ≥25% bag
- ✅ human god-form T5 post-DEF ≥12% live
- ✅ saiyan even T1→T5 hitFrac rises
- ✅ saiyan even T5→T7 hitFrac rises
- ✅ saiyan even T4+ unprotected ≥25% bag
- ✅ saiyan god-form T5 post-DEF ≥12% live
- ✅ namekian even T1→T5 hitFrac rises
- ✅ namekian even T5→T7 hitFrac rises
- ✅ namekian even T4+ unprotected ≥25% bag
- ✅ namekian god-form T5 post-DEF ≥12% live
- ✅ ancient_saiyan KP10 reduces landing dmg at T5
- ✅ bioandroid KP10 reduces landing dmg at T5
- ✅ frostdemon KP10 reduces landing dmg at T5
- ✅ ancient_saiyan infusion sponges more HP at T5
- ✅ bioandroid infusion sponges more HP at T5
- ✅ frostdemon infusion sponges more HP at T5
- ✅ saiyan vit_dump T7 > T1 pressure
- ✅ saiyan vit_dump T5 ≥ 28% bag
- ✅ saiyan vit_dump T5 ≥ 60% of even
- ✅ saiyan res_dump T7 > T1 pressure
- ✅ saiyan res_dump T5 ≥ 28% bag
- ✅ saiyan res_dump T5 ≥ 60% of even
- ✅ saiyan str_dump T7 > T1 pressure
- ✅ saiyan str_dump T5 ≥ 28% bag
- ✅ saiyan str_dump T5 ≥ 60% of even
- ✅ saiyan pwr_dump T7 > T1 pressure
- ✅ saiyan pwr_dump T5 ≥ 28% bag
- ✅ saiyan pwr_dump T5 ≥ 60% of even
- ✅ saiyan tank class T5 ≥ 28% bag
- ✅ ancient_saiyan transformed full loadout sponges vs none
- ✅ bioandroid transformed full loadout sponges vs none
- ✅ frostdemon transformed full loadout sponges vs none
- ✅ T5 transformed warrior softHits ≥0.35 for most races

## Sample: saiyan warrior even (base, no skills)

| Tier | HitFrac | After KP0 | SoftHits | MobDmg | MobHp |
|-----:|--------:|----------:|---------:|-------:|------:|
| T1 | 0.135 | 0.135 | 1.98 | 101 | 188 |
| T3 | 0.281 | 0.281 | 2.09 | 210 | 616 |
| T5 | 0.429 | 0.429 | 2.09 | 321 | 1279 |
| T7 | 0.530 | 0.530 | 2.09 | 397 | 1895 |

## Sample: skills at T5 saiyan warrior even (base)

| Skills | HitFrac | AfterKP | MobHp | KP save |
|--------|--------:|--------:|------:|--------:|
| none | 0.429 | 0.429 | 1279 | 0 |
| kp10 | 0.429 | 0.386 | 1279 | 32 |
| inf10 | 0.429 | 0.429 | 1637 | 0 |
| full | 0.429 | 0.386 | 1637 | 32 |

Rows: 9792. Races: ancient_saiyan, bioandroid, frostdemon, human, majin, monkey, namekian, saiyan, sento_saiyan, viltrumite.
