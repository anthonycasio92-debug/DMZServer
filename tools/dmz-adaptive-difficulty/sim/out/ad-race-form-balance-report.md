# AdaptiveDifficulty race/form simulation (1.0.25)

Source: `config/dragonminez/races/*`.
Model: soft STR/SKP/PWR (+ mild ENE) × tier% + live-bag hit-cap + god-form live-offense pressure.
Rows: 6032.

## Per-race peak (T5, mastery 100%, physical class)

| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |
|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|
| ancient_saiyan | 3 | 80.0 | 0.043 | 1.11 | 1.0 | 0.05 | 0.5 | 0.58 | `primalssj.primalgod` |
| frostdemon | 14 | 75.0 | 0.143 | 3.26 | 1.74 | 0.13 | 0.5 | 0.578 | `android_enhancement_a.metal_overdrive` |
| sento_saiyan | 11 | 57.0 | 0.065 | 3.44 | 1.82 | 0.2 | 0.5 | 0.57 | `ancestral_divinity.primal_evolved` |
| saiyan | 22 | 54.0 | 0.067 | 3.26 | 1.89 | 0.05 | 0.5 | 0.569 | `androidforms.ssdroid4` |
| human | 14 | 50.0 | 0.432 | 1.11 | 1.0 | 0.51 | 0.5 | 0.566 | `android_enhancement.overclock` |
| viltrumite | 8 | 34.5 | 0.088 | 4.2 | 2.2 | 0.06 | 0.47 | 0.556 | `androidforms.conquestfull` |
| bioandroid | 11 | 21.8 | 0.128 | 1.11 | 1.68 | 0.09 | 0.5 | 0.542 | `bioevolution.ultraperfect` |
| majin | 9 | 21.8 | 0.128 | 1.11 | 1.68 | 0.09 | 0.5 | 0.542 | `legendaryforms.superdemon` |
| monkey | 6 | 21.8 | 0.128 | 1.11 | 1.68 | 0.09 | 0.5 | 0.542 | `legendaryforms.gear3` |
| namekian | 8 | 21.8 | 0.128 | 1.11 | 1.68 | 0.09 | 0.5 | 0.542 | `legendaryforms.buffednamek` |

## Hard flags (--check)

None.

## Notes

- **ancient_saiyan**: packs die in 0.05 live hits (glass OK if RES counters)
- **saiyan**: packs die in 0.05 live hits (glass OK if RES counters)
- **viltrumite**: HP jump 2.2× on `androidforms.conquestfull`
- **viltrumite**: packs die in 0.06 live hits (glass OK if RES counters)

CSV: `/opt/cursor/artifacts/ad-race-form-simulation.csv`
