# AdaptiveDifficulty race/form simulation (1.0.11)

Source: `config/dragonminez/races/*`.
Model: soft STR/SKP/PWR (+ mild ENE) × tier% + class/top-2 counters + VIT hit cap.
Rows: 6032.

## Per-race peak (T5, mastery 100%, physical class)

| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |
|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|
| ancient_saiyan | 3 | 80.0 | 0.031 | 1.82 | 1.0 | 0.03 | 0.3 | 0.3 | `primalssj.primalgod` |
| frostdemon | 14 | 75.0 | 0.118 | 2.94 | 1.63 | 0.07 | 0.164 | 0.298 | `android_enhancement_a.metal_overdrive` |
| sento_saiyan | 11 | 57.0 | 0.048 | 2.97 | 1.7 | 0.1 | 0.164 | 0.29 | `ancestral_divinity.primal_evolved` |
| saiyan | 22 | 54.0 | 0.050 | 2.93 | 1.68 | 0.02 | 0.164 | 0.288 | `androidforms.ssdroid4` |
| human | 14 | 50.0 | 0.429 | 1.73 | 1.0 | 0.29 | 0.286 | 0.286 | `android_enhancement.overclock` |
| viltrumite | 8 | 34.5 | 0.066 | 3.34 | 2.01 | 0.03 | 0.136 | 0.274 | `androidforms.conquestfull` |
| bioandroid | 11 | 21.8 | 0.099 | 1.57 | 1.0 | 0.03 | 0.26 | 0.26 | `bioevolution.ultraperfect` |
| majin | 9 | 21.8 | 0.099 | 1.57 | 1.0 | 0.03 | 0.26 | 0.26 | `legendaryforms.superdemon` |
| monkey | 6 | 21.8 | 0.099 | 1.57 | 1.0 | 0.03 | 0.26 | 0.26 | `legendaryforms.gear3` |
| namekian | 8 | 21.8 | 0.099 | 1.57 | 1.0 | 0.03 | 0.26 | 0.26 | `legendaryforms.buffednamek` |

## Hard flags (--check)

None.

## Notes

- **ancient_saiyan**: packs die in 0.03 live hits (glass OK if RES counters)
- **bioandroid**: packs die in 0.03 live hits (glass OK if RES counters)
- **frostdemon**: packs die in 0.07 live hits (glass OK if RES counters)
- **majin**: packs die in 0.03 live hits (glass OK if RES counters)
- **monkey**: packs die in 0.03 live hits (glass OK if RES counters)
- **namekian**: packs die in 0.03 live hits (glass OK if RES counters)
- **saiyan**: packs die in 0.02 live hits (glass OK if RES counters)
- **viltrumite**: HP jump 2.01× on `androidforms.conquestfull`
- **viltrumite**: packs die in 0.03 live hits (glass OK if RES counters)

CSV: `/opt/cursor/artifacts/ad-race-form-simulation.csv`
