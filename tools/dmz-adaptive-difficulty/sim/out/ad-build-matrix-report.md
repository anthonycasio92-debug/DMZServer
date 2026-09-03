# AdaptiveDifficulty build matrix (1.0.25)

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
- ✅ saiyan vit_dump T5 ≥ 58% of even
- ✅ saiyan res_dump T7 > T1 pressure
- ✅ saiyan res_dump T5 ≥ 28% bag
- ✅ saiyan res_dump T5 ≥ 58% of even
- ✅ saiyan str_dump T7 > T1 pressure
- ✅ saiyan str_dump T5 ≥ 28% bag
- ✅ saiyan str_dump T5 ≥ 58% of even
- ✅ saiyan pwr_dump T7 > T1 pressure
- ✅ saiyan pwr_dump T5 ≥ 28% bag
- ✅ saiyan pwr_dump T5 ≥ 58% of even
- ✅ saiyan tank class T5 ≥ 28% bag
- ✅ ancient_saiyan transformed full loadout sponges vs none
- ✅ bioandroid transformed full loadout sponges vs none
- ✅ frostdemon transformed full loadout sponges vs none
- ✅ T5 transformed warrior softHits ≥0.35 for most races

## Sample: saiyan warrior even (base, no skills)

| Tier | HitFrac | After KP0 | SoftHits | MobDmg | MobHp |
|-----:|--------:|----------:|---------:|-------:|------:|
| T1 | 0.129 | 0.129 | 1.27 | 96 | 121 |
| T3 | 0.271 | 0.271 | 1.33 | 203 | 391 |
| T5 | 0.447 | 0.447 | 1.36 | 335 | 834 |
| T7 | 0.583 | 0.583 | 1.36 | 436 | 1236 |

## Sample: skills at T5 saiyan warrior even (base)

| Skills | HitFrac | AfterKP | MobHp | KP save |
|--------|--------:|--------:|------:|--------:|
| none | 0.447 | 0.447 | 834 | 0 |
| kp10 | 0.447 | 0.403 | 834 | 34 |
| inf10 | 0.447 | 0.447 | 1068 | 0 |
| full | 0.447 | 0.403 | 1068 | 34 |

Rows: 9792. Races: ancient_saiyan, bioandroid, frostdemon, human, majin, monkey, namekian, saiyan, sento_saiyan, viltrumite.
