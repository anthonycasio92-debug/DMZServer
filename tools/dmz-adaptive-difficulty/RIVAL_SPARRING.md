# Rival + Sparring in AdaptiveDifficulty 1.0.44

Port of live CNPC scripts **Rival System 4.7.10** and **Sparring Tp System 3.2.11**
(+ their command handlers) into the Adaptive Difficulty Forge mod.

## Test install

1. Build / install `AdaptiveDifficulty-1.0.44.jar` on the **test** server.
2. **Disable** CNPC Global Player scripts to avoid double TP:
   - `Rival System`
   - `Sparring Tp System`
   - `Rival Command Handler`
   - (optional) `Sparring Command Handler`
3. Do **not** deploy to live until tested.

## Data files

| System | Path |
|--------|------|
| Rivalry | `config/adaptivedifficulty/rivalry-v4.json` |
| Sparring / mentor | `config/adaptivedifficulty/sparring.json` |
| Feature flags | `config/adaptivedifficulty.json` |

Flags (defaults `true`):

- `enableRivalSystem`
- `enableSparringSystem`
- `rivalPresenceTp`
- `rivalInstinct`
- `rivalChallenges`

Admin: `/difficulty admin set enableRivalSystem true|false` (and the other keys).

## Commands

### `/rival`

- `/rival` / `help` — help
- `/rival <player>` — silent rival (Unknown → Declared when mutual silent)
- `/rival declare|accept|decline|remove <player>`
- `/rival list` / `stats` / `top`
- `/rival tpmsg [on|off]`
- `/rival challenge send <player> [minutes]` (1–10, default 1)
- `/rival challenge accept|decline|cancel`
- Ops: `/rival refresh` / `/rival save`

### `/spar`

- `/spar` / `help` / `stats` / `end`
- `/spar mentor <player>|accept|decline|remove`
- `/spar apprentice <player>|remove`
- Ops: `/spar admin mentor resetcd [player]` / `/spar save`

Spar sessions start automatically when both players trade hits within 15s, same dimension, ≤30 blocks.

## Intentional simplifications vs scripts

- No Proving Grounds, Hall of Fame, weekly quests, spectator mode, or season UI.
- Rival kill-near / underdog / anti-gank offense bonuses skipped (`RP_OFFENSE_ENABLED=false` already).
- Rival level-based TP drip curve simplified to global `0.60` scale on proximity / challenge / surpass awards.
- Instinct covers arrive / BP / charging / aura / fusion / form-surge basics.
- Spar beam-kind classification is coarse (`basic` / clash); full projectile taxonomy not ported.
