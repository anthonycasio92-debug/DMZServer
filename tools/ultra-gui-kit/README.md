# Ultra GUI — implement-yourself kit

**No KaiPath jar is shipped.** Copy these sources into your project.
Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).

| Who you are | What to copy |
|-------------|--------------|
| **Server mod author** | [`server/`](server/) |
| **KaiPath / JLDK** | [`kaipath-client/`](kaipath-client/) |

---

## Server mod authors

1. Copy `server/*.java`; rename package.
2. Register `<modId>:gui` protocol `"1"`:
   - msg 0 = open, msg 1 = action, msg 2 = features (optional C→S)
3. Send opens with `style` + `activeTab`; handle clicks verbatim.

---

## KaiPath / JLDK

Merge `kaipath-client/` into KaiPath.

**1.3.1 baseline (done):** 10-field decode, dynamic UI, any screenId, verbatim actions.

**Next (conformance gap LM already detects):**

1. Honor button `style`: `normal` / `confirm` (green) / `danger` (red) / `dim` (dark gray).
2. Honor `activeTab` — highlight the active tab.
3. Set `UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON = "[\"style\",\"activeTab\"]"` and
   call `advertiseFeatures` when the server channel is present.

Until then LM logs warnings and `/lm admin guitest` reports missing features.

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.106**).
