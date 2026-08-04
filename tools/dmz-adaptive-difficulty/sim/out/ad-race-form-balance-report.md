# AdaptiveDifficulty race/form simulation (1.0.12)

Source: `config/dragonminez/races/*`.
Model: soft STR/SKP/PWR (+ mild ENE) × tier% + VIT/RES floors + class/top-2 + raised VIT hit cap.
Rows: 6032.

## Per-race peak (T5, mastery 100%, physical class)

| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |
|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|
| ancient_saiyan | 3 | 80.0 | 0.031 | 1.43 | 1.0 | 0.04 | 0.44 | 0.44 | `primalssj.primalgod` |
| frostdemon | 14 | 75.0 | 0.118 | 2.31 | 1.63 | 0.1 | 0.242 | 0.438 | `android_enhancement_a.metal_overdrive` |
| sento_saiyan | 11 | 57.0 | 0.048 | 2.37 | 1.7 | 0.14 | 0.244 | 0.43 | `ancestral_divinity.primal_evolved` |
| saiyan | 22 | 54.0 | 0.050 | 2.33 | 1.68 | 0.03 | 0.244 | 0.428 | `androidforms.ssdroid4` |
| human | 14 | 50.0 | 0.429 | 1.38 | 1.0 | 0.4 | 0.426 | 0.426 | `android_enhancement.overclock` |
| viltrumite | 8 | 34.5 | 0.066 | 2.71 | 2.01 | 0.04 | 0.206 | 0.415 | `androidforms.conquestfull` |
| bioandroid | 11 | 21.8 | 0.099 | 1.3 | 1.07 | 0.04 | 0.401 | 0.401 | `bioevolution.ultraperfect` |
| majin | 9 | 21.8 | 0.099 | 1.3 | 1.07 | 0.04 | 0.401 | 0.401 | `legendaryforms.superdemon` |
| monkey | 6 | 21.8 | 0.099 | 1.3 | 1.07 | 0.04 | 0.401 | 0.401 | `legendaryforms.gear3` |
| namekian | 8 | 21.8 | 0.099 | 1.3 | 1.07 | 0.04 | 0.401 | 0.401 | `legendaryforms.buffednamek` |

## Hard flags (--check)

None.

## Notes

- **ancient_saiyan**: packs die in 0.04 live hits (glass OK if RES counters)
- **bioandroid**: packs die in 0.04 live hits (glass OK if RES counters)
- **majin**: packs die in 0.04 live hits (glass OK if RES counters)
- **monkey**: packs die in 0.04 live hits (glass OK if RES counters)
- **namekian**: packs die in 0.04 live hits (glass OK if RES counters)
- **saiyan**: packs die in 0.03 live hits (glass OK if RES counters)
- **viltrumite**: HP jump 2.01× on `androidforms.conquestfull`
- **viltrumite**: packs die in 0.04 live hits (glass OK if RES counters)

CSV: `/opt/cursor/artifacts/ad-race-form-simulation.csv`
