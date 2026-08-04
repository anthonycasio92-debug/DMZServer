# AdaptiveDifficulty race/form simulation (1.0.13)

Source: `config/dragonminez/races/*`.
Model: soft STR/SKP/PWR (+ mild ENE) × tier% + VIT/RES floors + class/top-2 + skill sponge + raised VIT hit cap.
Rows: 6032.

## Per-race peak (T5, mastery 100%, physical class)

| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |
|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|
| ancient_saiyan | 3 | 80.0 | 0.031 | 1.39 | 1.0 | 0.04 | 0.43 | 0.43 | `primalssj.primalgod` |
| frostdemon | 14 | 75.0 | 0.118 | 2.25 | 1.63 | 0.11 | 0.236 | 0.428 | `android_enhancement_a.metal_overdrive` |
| sento_saiyan | 11 | 57.0 | 0.048 | 2.3 | 1.7 | 0.17 | 0.238 | 0.421 | `ancestral_divinity.primal_evolved` |
| saiyan | 22 | 54.0 | 0.050 | 2.27 | 1.68 | 0.04 | 0.239 | 0.419 | `androidforms.ssdroid4` |
| human | 14 | 50.0 | 0.429 | 1.35 | 1.0 | 0.47 | 0.417 | 0.417 | `android_enhancement.overclock` |
| viltrumite | 8 | 34.5 | 0.066 | 2.64 | 2.01 | 0.05 | 0.202 | 0.407 | `androidforms.conquestfull` |
| bioandroid | 11 | 21.8 | 0.099 | 1.27 | 1.3 | 0.06 | 0.394 | 0.394 | `bioevolution.ultraperfect` |
| majin | 9 | 21.8 | 0.099 | 1.27 | 1.3 | 0.06 | 0.394 | 0.394 | `legendaryforms.superdemon` |
| monkey | 6 | 21.8 | 0.099 | 1.27 | 1.3 | 0.06 | 0.394 | 0.394 | `legendaryforms.gear3` |
| namekian | 8 | 21.8 | 0.099 | 1.27 | 1.3 | 0.06 | 0.394 | 0.394 | `legendaryforms.buffednamek` |

## Hard flags (--check)

None.

## Notes

- **ancient_saiyan**: packs die in 0.04 live hits (glass OK if RES counters)
- **bioandroid**: packs die in 0.06 live hits (glass OK if RES counters)
- **majin**: packs die in 0.06 live hits (glass OK if RES counters)
- **monkey**: packs die in 0.06 live hits (glass OK if RES counters)
- **namekian**: packs die in 0.06 live hits (glass OK if RES counters)
- **saiyan**: packs die in 0.04 live hits (glass OK if RES counters)
- **viltrumite**: HP jump 2.01× on `androidforms.conquestfull`
- **viltrumite**: packs die in 0.05 live hits (glass OK if RES counters)

CSV: `/opt/cursor/artifacts/ad-race-form-simulation.csv`
