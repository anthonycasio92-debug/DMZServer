# AdaptiveDifficulty build matrix (1.0.13)

Race × class × archetype × skill loadout × tier.
Skills: kiprotection / ki_infusion / potentialunlock (DMZ combat.json rates).

## Concept checklist

- ✅ human even T1→T5 hitFrac rises
- ✅ human even T5→T7 hitFrac rises
- ✅ human even T4+ unprotected ≥18% bag
- ✅ saiyan even T1→T5 hitFrac rises
- ✅ saiyan even T5→T7 hitFrac rises
- ✅ saiyan even T4+ unprotected ≥18% bag
- ✅ namekian even T1→T5 hitFrac rises
- ✅ namekian even T5→T7 hitFrac rises
- ✅ namekian even T4+ unprotected ≥18% bag
- ✅ ancient_saiyan KP10 reduces landing dmg at T5
- ✅ bioandroid KP10 reduces landing dmg at T5
- ✅ frostdemon KP10 reduces landing dmg at T5
- ✅ ancient_saiyan infusion sponges more HP at T5
- ✅ bioandroid infusion sponges more HP at T5
- ✅ frostdemon infusion sponges more HP at T5
- ✅ saiyan vit_dump T7 > T1 pressure
- ✅ saiyan res_dump T7 > T1 pressure
- ✅ saiyan str_dump T7 > T1 pressure
- ✅ saiyan pwr_dump T7 > T1 pressure
- ✅ T5 transformed warrior softHits ≥0.35 for most races

## Sample: saiyan warrior even (base, no skills)

| Tier | HitFrac | After KP0 | SoftHits | MobDmg | MobHp |
|-----:|--------:|----------:|---------:|-------:|------:|
| T1 | 0.108 | 0.108 | 1.81 | 81 | 172 |
| T3 | 0.194 | 0.194 | 1.91 | 146 | 562 |
| T5 | 0.310 | 0.310 | 1.91 | 232 | 1168 |
| T7 | 0.403 | 0.403 | 1.91 | 302 | 1730 |

## Sample: skills at T5 saiyan warrior even (base)

| Skills | HitFrac | AfterKP | MobHp | KP save |
|--------|--------:|--------:|------:|--------:|
| none | 0.310 | 0.310 | 1168 | 0 |
| kp10 | 0.310 | 0.279 | 1168 | 23 |
| inf10 | 0.310 | 0.310 | 1495 | 0 |
| full | 0.310 | 0.279 | 1495 | 23 |

Rows: 9792. Races: ancient_saiyan, bioandroid, frostdemon, human, majin, monkey, namekian, saiyan, sento_saiyan, viltrumite.
