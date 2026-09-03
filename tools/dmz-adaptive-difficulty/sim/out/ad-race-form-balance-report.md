# AdaptiveDifficulty race/form simulation (1.0.25)

Source: `config/dragonminez/races/*`.
Model: soft STR/SKP/PWR (+ mild ENE) × tier% + live-bag hit-cap + god-form live-offense pressure.
Rows: 6032.

## Per-race peak (T5, mastery 100%, physical class)

| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |
|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|
| ancient_saiyan | 3 | 80.0 | 0.043 | 1.23 | 1.0 | 0.03 | 0.414 | 0.419 | `primalssj.primalgod` |
| frostdemon | 14 | 75.0 | 0.143 | 3.61 | 1.74 | 0.09 | 0.414 | 0.417 | `android_enhancement_a.metal_overdrive` |
| sento_saiyan | 11 | 57.0 | 0.065 | 3.67 | 1.82 | 0.13 | 0.414 | 0.411 | `ancestral_divinity.primal_evolved` |
| saiyan | 22 | 54.0 | 0.067 | 3.61 | 1.89 | 0.03 | 0.414 | 0.41 | `androidforms.ssdroid4` |
| human | 14 | 50.0 | 0.432 | 1.25 | 1.0 | 0.33 | 0.423 | 0.423 | `android_enhancement.overclock` |
| viltrumite | 8 | 34.5 | 0.088 | 4.06 | 2.2 | 0.04 | 0.339 | 0.401 | `androidforms.conquestfull` |
| bioandroid | 11 | 21.8 | 0.128 | 1.23 | 1.68 | 0.06 | 0.414 | 0.391 | `bioevolution.ultraperfect` |
| majin | 9 | 21.8 | 0.128 | 1.23 | 1.68 | 0.06 | 0.414 | 0.391 | `legendaryforms.superdemon` |
| monkey | 6 | 21.8 | 0.128 | 1.23 | 1.68 | 0.06 | 0.414 | 0.391 | `legendaryforms.gear3` |
| namekian | 8 | 21.8 | 0.128 | 1.23 | 1.68 | 0.06 | 0.414 | 0.391 | `legendaryforms.buffednamek` |

## Hard flags (--check)

- **viltrumite** `androidforms.conquestfull`: KP10 pre-cap 0.360 vs none 0.339
- **viltrumite** android `androidforms.conquestfull`: KP10 0.360 vs none 0.339

## Notes

- **ancient_saiyan**: packs die in 0.03 live hits (glass OK if RES counters)
- **bioandroid**: packs die in 0.06 live hits (glass OK if RES counters)
- **majin**: packs die in 0.06 live hits (glass OK if RES counters)
- **monkey**: packs die in 0.06 live hits (glass OK if RES counters)
- **namekian**: packs die in 0.06 live hits (glass OK if RES counters)
- **saiyan**: packs die in 0.03 live hits (glass OK if RES counters)
- **viltrumite**: HP jump 2.2× on `androidforms.conquestfull`
- **viltrumite**: packs die in 0.04 live hits (glass OK if RES counters)

CSV: `/opt/cursor/artifacts/ad-race-form-simulation.csv`
