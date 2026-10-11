# Legacy Mechanics — Ultra GUI (no CNPC, server-only LM)

Ultra menus use **DMZUltra** (`dmzultra`) for textures and widgets. The full
`LegacyMechanics` jar stays on the **dedicated server only**.

## Two jars

| Jar | Where | Role |
|-----|--------|------|
| `mods/LegacyMechanics-*.jar` | **Server only** | Gameplay, commands, opens Ultra screens over the network |
| `mods/LegacyMechanicsUltra-*.jar` | **Client modpack** | Opens Ultra screens; **pulls textures from dmzultra** |

Players do **not** install the server LegacyMechanics jar on their client.

## Client requirements

1. `dmzultra` (DMZUltra) — provides Ultra UI textures/widgets  
2. `LegacyMechanicsUltra-*.jar` — thin bridge (this repo builds it)  
3. CustomNPCs is **not** required for `guiBackend=ultra`

## Server config

`config/legacymechanics.json` (or DifficultyConfig defaults):

```json
"guiBackend": "ultra",
"useNativeTestGui": true
```

`/lm admin reload` after changing.

## Commands

| Command | Behavior when `guiBackend=ultra` |
|---------|----------------------------------|
| `/lm` | Opens native Ultra hub if the client has LegacyMechanicsUltra |
| `/lm admin testgui` | Same Ultra hub for staff |
| Hub buttons | Server receives an action packet; opens chat menus for subsystems not yet on Ultra screens |

If the client lacks the Ultra companion, `/lm` falls back to the chat hub with a short message.

## Why a client jar is still needed

Minecraft draws GUIs on the client. DMZUltra’s textures already live in `dmzultra`.
LegacyMechanicsUltra only calls those widgets and talks to the server. It does **not**
duplicate gameplay or require the server LM jar on the client.

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```

Produces both jars under `mods/`.
