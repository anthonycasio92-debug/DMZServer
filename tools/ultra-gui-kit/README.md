# Ultra GUI — implement-yourself kit + KaiPath overlay builder

Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix / pitfalls: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).

| Who you are | What to use |
|-------------|-------------|
| **Server mod author** | [`server/`](server/) |
| **KaiPath / JLDK** | [`kaipath-client/`](kaipath-client/) sources (merge into official KaiPath) |
| **Ops / interim client** | `bash tools/ultra-gui-kit/build-kaipath-overlay.sh` → `mods/kaipath-1.3.7-lm-ultra.jar` |

---

## Server mod authors

1. Copy `server/*.java`; rename package.
2. Register `<modId>:gui` protocol `"1"`:
   - msg 0 = open, msg 1 = action, msg 2 = features (optional C→S)
3. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` when needed); handle clicks verbatim.

---

## KaiPath / JLDK

**Released 1.3.7** already has style / activeTab / titleAlign / playerModel + GuiFeaturesPacket (4 features).

**This kit adds (and the overlay jar ships):**

1. GuiOpenPacket **field 11** `inputsJson` text fields + `bindInput` → `args.value`
2. `textInput` in the advertise list: `["style","activeTab","titleAlign","playerModel","textInput"]`
3. Explicit UTF-8 GuiOpen string helpers (same wire shape as MC `writeUtf`)

Merge these sources into the next official KaiPath. Until then, build the overlay:

```bash
bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# → mods/kaipath-1.3.7-lm-ultra.jar  (Implementation-Version 1.3.7-lm-ultra)
DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-kaipath-live.sh   # prefers *-lm-ultra
```

Players replace stock `kaipath-1.3.7.jar` with the overlay jar (do not run both).

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.122**).
