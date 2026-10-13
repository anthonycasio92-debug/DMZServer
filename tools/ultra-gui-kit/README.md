# Ultra GUI — implement-yourself kit

**No KaiPath jar is shipped from this folder.** Copy these sources into your project.
Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).

| Who you are | What to copy |
|-------------|--------------|
| **Server mod author** | [`server/`](server/) |
| **KaiPath / JLDK** | [`kaipath-client/`](kaipath-client/) |

---

## Server mod authors

1. Copy `server/*.java`; rename package.
2. Register `<modId>:gui` protocol `"1"`:
   - msg 0 = open, msg 1 = action, msg 2 = features (optional C→S)
3. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` when needed); handle clicks verbatim.

---

## KaiPath / JLDK

Merge `kaipath-client/` into KaiPath and rebuild.

**Reference kit (current — merge this):**

1. Button `style` chrome (`confirm` / `danger` / `dim` / `normal`)
2. `activeTab` highlight chip
3. `layoutJson.titleAlign`
4. `layoutJson.playerModel` local-player preview (+ turntable)
5. GuiOpenPacket **field 11** `inputsJson` text fields + `bindInput` → `args.value`
6. Explicit UTF-8 string codec
7. `ADVERTISED_FEATURES_JSON = ["style","activeTab","titleAlign","playerModel","textInput"]`

**Live jar note:** `mods/kaipath-1.3.1.jar` is behind this kit (no field 11 / no textInput
advertise). Weight shop typed entry stays blocked until a rebuilt KaiPath ships.

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.120**).
