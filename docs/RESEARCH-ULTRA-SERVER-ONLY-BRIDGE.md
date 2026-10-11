# Research brief: Server-only LegacyMechanics Ultra GUI without client modpack updates

**Audience:** Another researcher / AI agent investigating whether a path exists.  
**Repo:** `anthonycasio92-debug/DMZServer`  
**Branch context:** `cursor/ultra-server-only-34d6` (PR work for server-only LM + Ultra companion)  
**Date of brief:** 2026-10-11  
**Status:** Architecture documented; zero-client-update Ultra GUI remains an open research problem.

---

## 1. Problem statement

### Desired end state
1. **LegacyMechanics (LM)** runs **server-only** (players do not install LM on clients).
2. Players see **DMZUltra-styled native menus** (textures/widgets from `dmzultra`), **not** CustomNPCs menus.
3. Players can **join without updating their existing client modpack**.

### Conflict
Items (2) and (3) appear mutually exclusive under normal Forge rules: any *new* screen / packet-handler bytecode must already exist on the client. Stock `dmzultra` in the current pack does **not** contain the LM bridge. Putting the bridge into a new `dmzultra` build *is* a client update.

### Success criteria (for a candidate solution)
| Criterion | Pass condition |
|-----------|----------------|
| S1 | Dedicated server ships LM; clients do **not** need `LegacyMechanics-*.jar` |
| S2 | `/lm` (or equivalent) opens Ultra-themed GUI using `com.dmzultra.client.ui.*` look |
| S3 | Clients with the **current** modpack (already including some `dmzultra` version) join and get S2 **without** downloading a new jar / pack revision |
| S4 | No CustomNPCs dependency for this path (`guiBackend=ultra`) |
| S5 | Legal: no unauthorized redistribution of ARR `dmzultra` as a public fork |

If S3 cannot be met, the best known fallback is: **one** client/pack bump that embeds a stable bridge, then LM stays server-only forever (S1+S2+S4, drop S3).

---

## 2. Hard constraints (do not ignore)

### C1 — GUI code executes on the client only
Minecraft `Screen` / widget rendering runs on the physical client. The dedicated server cannot “push” new Java classes into a running client. Server-only jars cannot mixin into client-only code of mods the server does not execute as a client.

### C2 — Forge SimpleChannel presence is negotiated at login
`LmGuiNetwork` uses `CHANNEL.isRemotePresent(connection)`. If the client never registered `legacymechanics:gui`, the server correctly falls back to chat. You cannot open a channel the client does not know.

### C3 — Stock `dmzultra` uses `displayTest = "MATCH_VERSION"`
From `dmzultra-0.2.0` `META-INF/mods.toml`:
- `modId = "dmzultra"`, `version = "0.2.0"`
- `displayTest = "MATCH_VERSION"`
- License: **All Rights Reserved**
- Authors: DarkForest; CurseForge owner username observed: `luigga`
- Mandatory deps: Forge 47.4.10+, MC 1.20.1, `dragonminez` `[2.1.3,2.2)`, `noeabosses` `[1.0.37,)`

Implication: shipping a **newer** `dmzultra` **only on the server** will typically **reject** clients still on 0.2.0, and still would not give those clients new screen classes even if version checks were loosened.

### C4 — ARR / no silent fork
`dmzultra` is All Rights Reserved. Research may analyze the jar; public redistribution of a patched fork needs author permission. Private server-side patches still require clients to receive the patched jar to gain new client code (back to C1/C3).

### C5 — Rejected product directions (user)
- CustomNPCs textured menus as the Ultra replacement: **rejected**.
- Requiring full LM on clients: **rejected** (server-only LM is the goal).

---

## 3. Current working architecture (already in repo)

### Jars
| Jar | Role | Side |
|-----|------|------|
| `mods/LegacyMechanics-*.jar` (e.g. 4.6.76) | Gameplay, commands, opens GUI over network | **Server only** |
| `mods/LegacyMechanicsUltra-*.jar` | Registers channel; opens Ultra screens | **Client** |
| `dmzultra-*.jar` | Ultra UI toolkit + textures + DMZ Ultra content | Client (+ often server for MATCH_VERSION) |

Docs: `tools/dmz-adaptive-difficulty/FORGE-ULTRA-GUI.md`

### Network protocol (stable string IDs — intentional)
- Channel: `legacymechanics:gui`
- Protocol version string: `"1"` (exact match both sides)
- Message 0 S2C: `GuiOpenPacket` — one UTF string `screenId` (max 64), default `"hub"`
- Message 1 C2S: `GuiActionPacket` — `screenId`, `actionId`, `argsJson` (max 64/64/512)

Key sources:
- `tools/dmz-adaptive-difficulty/src/main/java/.../net/gui/LmGuiNetwork.java`
- `GuiOpenPacket.java`, `GuiActionPacket.java`, `NativeGuiActions.java`

### Client companion behavior
- Mod id: `legacymechanicsultra`
- `DisplayTest` = ignore server-only (`NetworkConstants.IGNORESERVERONLY`) so clients without server LM still join
- Reflective soft-dep on:
  - `com.dmzultra.client.ui.UltraUi$Theme` (field `FORGE`)
  - `com.dmzultra.client.ui.UltraButton`
  - `com.dmzultra.client.ui.UltraList`
- Hub actions today: `character`, `difficulty`, `progression`, `prestige`, `spar`, `rival` → server opens **chat** menus (`NativeGuiActions`), not more Ultra screens yet

### Server open path
`MechanicsMenu` / `CnpcStaffTestGui` when `guiBackend=ultra`:
1. `LmGuiNetwork.sendOpen(player, "hub")` if remote has channel
2. Else chat hub + message that Ultra companion is missing

Config keys: `guiBackend=ultra`, `useNativeTestGui=true` in `DifficultyConfig` / `legacymechanics.json`.

---

## 4. What `dmzultra` already contains (local jar)

Downloaded for analysis (not committed; ARR):
- Path: `uploads/dmzultra/dmzultra-0.2.0.jar`
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/dmz-ultra (project id `1717716`)
- Files: `9106675` → `dmzultra-0.2.0.jar`; also `9060681` → `0.1.0`
- CDN (works when CurseForge HTML is Cloudflare-blocked):  
  `https://mediafilez.forgecdn.net/files/9106/675/dmzultra-0.2.0.jar`
- SHA256 (0.2.0): `a86593f26e3312c744fb0c2ebad1bb898a807e69e1d628edfb0b693f8f338d8e`

### Confirmed UI toolkit (public API via `javap`)
- `com.dmzultra.client.ui.UltraUi` (+ `Theme`, `Look`, `Palette`, …)
- `com.dmzultra.client.ui.UltraButton`
- `com.dmzultra.client.ui.UltraList`
- Many first-party screens under `com.dmzultra.client.*` (Angel, Arena, Travel, Journal, Staff, …)

### Confirmed networking (not LM)
- Arena v2: `com.dmzultra.arena.v2.net.V2Network`, `OpenS2CPacket`, `ActionC2SPacket`, `StatusS2CPacket`
  - `OpenS2CPacket` carries Tenkaichi eras + preselect string — **arena-specific**, not a generic “open arbitrary screen by id” API for LM
- Other packets under `com.dmzultra.net.*`, `divine.SparkingNetwork`, etc.
- **No** `legacymechanics:gui` channel in stock 0.2.0
- **No** LM hub screen classes

### Mixins
- `dmzultra.mixins.json`, `dmzultra.dmz.mixins.json`, `NoeaPriorityMixinPlugin`

---

## 5. Approaches already considered

| Approach | Verdict | Why |
|----------|---------|-----|
| A. Server-only LM jar that mixins Ultra | Impossible | Server JVM never loads client Ultra UI; mixins don’t run on client |
| B. Thin client companion (`LegacyMechanicsUltra`) | **Works today** | Meets S1,S2,S4; **fails S3** (pack must add companion) |
| C. Merge bridge into `dmzultra`, ship new CF version | Architecturally sound | Meets S1,S2,S4 after **one** pack bump; **fails S3** for players still on old jar; needs author permission (S5) |
| D. Put patched `dmzultra` only on server | Fails | MATCH_VERSION + no client bytecode → kick / no GUI |
| E. Reuse CNPC menus with Ultra textures | Rejected by product | User wants Ultra, not CNPC |
| F. Chat / book / tellraw “GUI” | Works without client update | Meets S1,S3,S4; **fails S2** (not Ultra native GUI) |
| G. Hijack existing `dmzultra` S2C open packets | Unlikely | Known `OpenS2CPacket` is arena-roster shaped; would need a latent generic dispatcher (research target) |

---

## 6. Research questions for the next investigator

Prioritize falsifiable probes. Goal: find **any** mechanism where **bytecode already on the client** can be driven by the server to show an Ultra-themed LM hub without a new jar.

### Q1 — Latent generic UI / scripting inside stock `dmzultra`
**Probe:** Decompile / string-search 0.2.0 for:
- Generic screen registry, “open by id”, JSON/layout driven UI, kubejs/crafttweaker hooks, command-triggered client screens
- Any `SimpleChannel` message that accepts a free-form screen name and opens `UltraUi`-based UI
- Resource-pack or datapack driven menus

**Pass:** Server LM can emit an existing packet/command and stock clients open a usable hub.  
**Fail:** Only hard-coded first-party screens exist.

### Q2 — Can an already-installed *other* client mod act as the bridge?
Modpack: CurseForge `testing-dmzlegacy` / `docs/MODPACK.md` (DMZ Legacy Reborn 2.1.3).  
**Probe:** List every client-present mod that:
- Already registers a flexible networking API, OR
- Ships Scripting / KubeJS client events that can open screens, OR
- Is under this server’s control (same authors) and could be silently updated… (note: that still violates S3 unless the jar is already identical)

**Pass:** Zero *new* downloads; drive existing mod.  
**Fail:** Nothing in the pack can open Ultra widgets on command.

### Q3 — Forge/Vanilla server→client UI without custom mods
**Probe:** Advancements toast, written books, dialog (1.21.6+ — **not** on 1.20.1), container menus with custom `MenuType` (still needs client registration), map items, boss bars.  
**Pass:** Acceptable Ultra-like UX from vanilla alone (probably fails S2 visual bar).  
**Fail:** Confirms custom client code required for Ultra look.

### Q4 — Soft channel / optional protocol games
**Probe:** Could LM register `legacymechanics:gui` as optional on server while clients without it still join (already true today), and somehow inject a handler?  
**Known:** Clients without the companion simply lack the channel; Forge will not invent handlers.  
**Only interesting if** some already-loaded client class registers that exact channel name (search all pack jars for `legacymechanics`).

### Q5 — Hot-patch / remote class loading
**Probe:** Does any mod in the pack support remote classpath extension, plugin download, or authenticated class loading from the server? (Rare; security nightmare; usually absent.)  
**Pass:** Extremely unusual; document threat model.  
**Fail:** Expected.

### Q6 — Author collaboration path (non-technical but valid)
**Probe:** Contact DarkForest / `luigga` (Discord linked from related DMZU projects, e.g. `https://discord.gg/8h2pkWAte` on DMZUServerCore page) to:
1. Add optional LM bridge packets + thin hub screen to official `dmzultra`, **or**
2. Change `displayTest` / document a client-optional bridge module they ship

This satisfies S1–S4 after **one** official update (S3 only for players who already pulled that update). Closest practical path if Q1–Q5 fail.

### Q7 — Protocol forward-compatibility (if one bump is inevitable)
Design the one client change so **future** LM features need **no** further pack updates:
- Keep `legacymechanics:gui` / protocol `"1"`
- Screen/action as strings + JSON payload
- Client renders a **data-driven** Ultra shell (title, button list from S2C JSON) rather than hard-coded hub only

Then S3 becomes “no *further* updates” after the first bridge lands.

---

## 7. Suggested investigation procedure

1. **Inventory the live client modpack jars** (not just server `mods/`).  
   Unpack / `strings` / search for: `legacymechanics`, `UltraUi`, `SimpleChannel`, `openScreen`, `GuiOpen`.
2. **Deep-decompile `dmzultra` 0.2.0** focusing on `com.dmzultra.net`, `client.ui`, any `*Registry*`, `*Network*`, config-driven screens (`UltraClientConfig`).
3. **Trace `V2Network.useChannel` / `sendOpen`** — confirm no overload that opens arbitrary screens.
4. **Search GitHub / Discord / CF description** for “DMZ Ultra” API, addon hooks, or “external menu” docs (mod is young: 0.1.0 on 2026-10-04, 0.2.0 on 2026-10-09).
5. **If nothing found:** draft a minimal upstream patch PR description for DarkForest (files to add = port of `LegacyMechanicsUltra` into `dmzultra` client, channel registration, data-driven shell). Do **not** publish ARR fork without permission.
6. **Report back** with: hypothesis ID, evidence paths, pass/fail, and whether S3 is still possible.

---

## 8. Key file map (this repo)

```
tools/dmz-adaptive-difficulty/
  FORGE-ULTRA-GUI.md                          # operator doc
  build.sh                                    # builds LM + LegacyMechanicsUltra
  src/main/java/.../net/gui/
    LmGuiNetwork.java                         # channel legacymechanics:gui
    GuiOpenPacket.java / GuiActionPacket.java
    NativeGuiActions.java                     # hub → chat menus
  src/main/java/.../gui/
    MechanicsMenu.java                        # ultra vs cnpc vs chat
    GuiBackend.java
  src/client/java/.../
    LegacyMechanicsUltraMod.java
    LmClientBootstrap.java
    gui/LmHubScreen.java
    gui/UltraWidgetAdapter.java               # reflective UltraUi bridge
uploads/dmzultra/dmzultra-0.2.0.jar           # local CF pull (ARR; untracked)
uploads/jars/LM-GUI-4.6.64-ultra-menu-reference.jar
docs/MODPACK.md                               # client pack: testing-dmzlegacy
```

CurseForge HTML often returns Cloudflare 403 from automation; use:
- `https://api.cfwidget.com/minecraft/mc-mods/dmz-ultra`
- `https://mediafilez.forgecdn.net/files/{id/1000}/{id%1000}/{filename}`

---

## 9. Bottom line for the researcher

**Known solid path:** one client-side bridge (companion jar **or** official `dmzultra` patch), then LM remains server-only with a stable string protocol.

**Unknown / research target:** whether stock clients already contain *any* unused hook that can open Ultra UI from server LM **without** a new download. That is the only way to satisfy S3 today. Prioritize Q1 and Q4 against the real client modpack jar set; treat remote class loading (Q5) as last-resort and security-sensitive.

**Do not claim S3 is solved** until a concrete packet/command path is demonstrated on an unmodified client jar set matching the live pack.
