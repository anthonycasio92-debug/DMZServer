# Ultra GUI — implement-yourself kit

**No KaiPath jar is shipped.** This folder is source for you to copy into your project
and implement. Protocol: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).

| Who you are | What to copy |
|-------------|--------------|
| **Server mod author** (any mod, not only LM) | [`server/`](server/) → your mod package |
| **KaiPath / client author (JLDK)** | [`kaipath-client/`](kaipath-client/) → KaiPath `ultra/consumers` |

---

## Server mod authors

1. Copy the three files in `server/` into your mod; rename the package (`com.example.ultragui` → yours).
2. Register a Forge channel on `<yourModId>:gui`, protocol `"1"`:
   - msg 0 = `UltraGuiOpenPacket` (S→C)
   - msg 1 = `UltraGuiActionPacket` (C→S)
3. Send an open packet; handle clicks by echoing `actionId` / `argsJson` (see hello world in the protocol doc).
4. Ask KaiPath to register your channel: `UltraGuiConsumerRegistry.registerClientChannel("yourmodid")`.

Files:

- `UltraGuiChannel.java` — `<modid>:gui` convention + defaults  
- `UltraGuiOpenPacket.java` — 10-field open (encode/decode has **no** mod-id strings)  
- `UltraGuiActionPacket.java` — 3-field click (verbatim echo)

Also mirrored under `docs/ultra-gui-protocol/reference/` for doc links.

---

## KaiPath / JLDK (client)

Merge `kaipath-client/` into the KaiPath tree (packages already match
`com.dmzlegacy.kaipath.ultra.consumers…`).

| Class | Job |
|-------|-----|
| `UltraGuiConsumerRegistry` | Multi-mod registry keyed by `<modid>:gui` |
| `…/legacymechanics/GuiOpenPacket` | Decode all 10 fields |
| `…/legacymechanics/GuiActionPacket` | Encode clicks |
| `…/legacymechanics/LmDynamicScreen` | Render title/tabs/lines/buttons from JSON |
| `…/legacymechanics/LmScreens` | No screenId whitelist |
| `…/legacymechanics/LmGuiNetwork` | Registers LM’s channel via the registry |
| `…/legacymechanics/LmUltraConsumer` | Client bootstrap entry |
| `…/legacymechanics/LmHubScreen` | Thin compat subclass |

Brief checklist: [`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`](../../docs/KAIPATH_JLDK_CLIENT_BRIEF.md).

Other partner mods: call `UltraGuiConsumerRegistry.registerClientChannel("mymod")` the same way as LM.

---

## What we do **not** provide

- A prebuilt `kaipath-*-lm-ultra.jar` for players to drop in  
- A requirement that other mods depend on LegacyMechanics  

LM’s own server implementation remains in `tools/dmz-adaptive-difficulty/…/net/gui/` (4.6.105).
