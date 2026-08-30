# Androidify (console / Saga)

Dr. Gero's built-in conversion is blocked on this server. Use this command
instead — same DragonMineZ upgrade path as the old CNPC trigger 45 script.

## Commands

```
androidify <playerName>
androidify <playerName> force
androidification <playerName>
```

Player must be **online**.

- Default: humans only (or races that have `androidforms` configured)
- `force`: skip the human-only race gate (still blocks `bioandroid`)

## Saga / Fabled / console reward

Run as a **console** command (not as the player):

```
androidify {player}
```

or with your placeholder:

```
androidify %player%
androidify @p
```

Examples that work from the server console:

```
androidify SirVampireII
androidify SirVampireII force
```

## Reload

```
/kubejs reload server_scripts
```

Look for: `[Androidify] /androidify + /androidification on live dispatcher`

Full restart also registers via `commandRegistry`.

## What it does

1. `setAndroidUpgraded(true)`
2. Unlock `androidforms` skill level 1
3. Remove `superforms` + `legendaryforms`
4. Activate `androidforms.androidbase`
5. Sync DMZ stats to the client

## Related

- Old CNPC path: `noppes script trigger 45 <player>` (`AndrioidConversion.js`) — still valid if CNPC triggers are installed
- Melee-fix still blocks the Gero NPC packet / `handleGero` reflection path
