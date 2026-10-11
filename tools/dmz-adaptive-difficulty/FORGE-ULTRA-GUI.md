# Legacy Mechanics — Ultra GUI via Kai Path (server-only LM)

Ultra menus use **DMZUltra** (`dmzultra`) for textures/widgets. The
**LegacyMechanics** jar stays on the **dedicated server only**.

The **client bridge lives in Kai Path** (`kaipath`), not in LM and not as a
patch to `dmzultra`. Drop-in sources + integrate steps:

→ **`tools/kaipath-lm-ultra-bridge/`** (`INTEGRATE.md`)

## Jars

| Jar | Where | Role |
|-----|--------|------|
| `mods/LegacyMechanics-*.jar` | **Server only** | Gameplay, commands, opens Ultra screens over the network |
| `kaipath-*.jar` (with LM bridge) | **Client modpack** | Registers `legacymechanics:gui`; opens Ultra screens |
| `dmzultra-*.jar` | **Client modpack** | Ultra UI toolkit / textures (optional soft dep) |

Players do **not** install the server LegacyMechanics jar on their client.
Do **not** also ship `LegacyMechanicsUltra` once kaipath includes the bridge
(only one mod may own the channel).

## Server config

```json
"guiBackend": "ultra",
"useNativeTestGui": true
```

`/lm admin reload` after changing.

## Commands

| Command | Behavior when `guiBackend=ultra` |
|---------|----------------------------------|
| `/lm` | Opens native Ultra hub if the client has the kaipath LM bridge |
| `/lm admin testgui` | Same Ultra hub for staff |
| Hub buttons | Server receives an action packet; opens chat menus for subsystems |

If the client lacks the bridge channel, `/lm` falls back to the chat hub.

## Protocol

- Channel: `legacymechanics:gui`
- Protocol: `"1"`
- S2C open: `screenId` (e.g. `hub`)
- C2S action: `screenId`, `actionId`, `argsJson`

## Build (server LM)

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```

Produces the server jar under `mods/`. The optional `LegacyMechanicsUltra`
companion is a **legacy fallback** only; prefer kaipath hosting the bridge.
