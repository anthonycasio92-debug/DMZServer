# Fabled Prestige skill purchase fix

## Problem
Players with **Ancient Saiyan** could not buy **Permanent Majin** / **Permanent Mutant**.

Root causes:
1. **GUI slot collision** — `gui.yml` `stPrestige` put Permanent Majin on page-1 slot 4 and Ancient Saiyan on page-2 slot 4. Same slot index blocked purchasing Majin after Ancient.
2. **Class level gate** — Majin/Mutant had `level-base: 3` while Ancient unlocks at class level 1 (with LP), so Ancient holders below Prestige class level 3 could not buy them.

## Intended rules
| Skill | Exclusive with | Combines with |
|-------|----------------|---------------|
| Permanent Majin | Permanent Mutant | Ancient Saiyan, Sento Saiyan |
| Permanent Mutant | Permanent Majin | Ancient Saiyan, Sento Saiyan |
| Ancient Saiyan | Sento Saiyan | Majin **or** Mutant |
| Sento Saiyan | Ancient Saiyan | Majin **or** Mutant |

## Files changed
- `plugins/Fabled/gui.yml` — unique Prestige tree slots (single page)
- `plugins/Fabled/dynamic/skill/Permanent Majin.yml` — `level-base: 1`, incompat Mutant only
- `plugins/Fabled/dynamic/skill/Permanent Mutant.yml` — `level-base: 1`, incompat Majin only
- `plugins/Fabled/dynamic/skill/Ancient Saiyan.yml` — incompat Sento only
- `plugins/Fabled/dynamic/skill/Sento Saiyan.yml` — incompat Ancient only

Also patch live `plugins/Fabled/dynamic/skills.yml` to match (Fabled may load the aggregate).

## Reload
```
/class reload
```
or restart. Players should reopen the Prestige skill tree.
