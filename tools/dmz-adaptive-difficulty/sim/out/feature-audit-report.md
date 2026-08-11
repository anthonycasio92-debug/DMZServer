# AdaptiveDifficulty feature audit (1.0.14)

See `feature-audit-report.txt` for the full fail-closed checklist.

## Combat model (1.0.14) — god-form pressure

- Soft offense: **STR/SKP/PWR** + mild **ENE**, higher form inherit (`transformScaleWeight` **0.65**)
- Hit-cap blends **soft↔live HP** so god forms cannot out-tank on soft peel alone
- Live-offense transform floor pulls bounded live threat when forms compress soft stats
- Stronger T4–T7 transform nudges; less mega-form compression
- Hit budgets sized for DMZ DEF (~65% mit): T5 transformed ~15%+ of live bag post-DEF
- `mobHealthScale` **1.15** + skill sponge (infusion/PU)
