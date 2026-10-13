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

**Released 1.3.7** (vendored at `mods/kaipath-1.3.7.jar`) already has:

1. Button `style` chrome
2. `activeTab` highlight
3. `layoutJson.titleAlign`
4. `layoutJson.playerModel` preview
5. `GuiFeaturesPacket` advertise `["style","activeTab","titleAlign","playerModel"]`

**Still merge from this kit for the next release:**

1. GuiOpenPacket **field 11** `inputsJson` text fields + `bindInput` → `args.value`
2. Add `textInput` to the advertise list

Weight shop typed entry stays blocked until that ships.

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.121**).
