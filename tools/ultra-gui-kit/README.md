# Ultra GUI — implement-yourself kit (for KaiPath / JLDK)

Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix / pitfalls: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).

| Who you are | What to use |
|-------------|-------------|
| **Server mod author** | [`server/`](server/) |
| **KaiPath / JLDK** | [`kaipath-client/`](kaipath-client/) — merge into the next official KaiPath release |

**LegacyMechanics does not ship or deploy KaiPath updates.** The KaiPath mod developer owns the official jar. This kit is the handoff for what to merge.

---

## Server mod authors

1. Copy `server/*.java`; rename package.
2. Register `<modId>:gui` protocol `"1"`:
   - msg 0 = open, msg 1 = action, msg 2 = features (optional C→S)
3. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` when needed); handle clicks verbatim.

---

## KaiPath / JLDK — merge into next official release

**Released 1.3.7** already has style / activeTab / titleAlign / playerModel + GuiFeaturesPacket (4 features).

**Still merge from this kit:**

1. GuiOpenPacket **field 11** `inputsJson` text fields + `bindInput` → `args.value`
2. `textInput` in the advertise list: `["style","activeTab","titleAlign","playerModel","textInput"]`
3. Explicit UTF-8 GuiOpen string helpers (same wire shape as MC `writeUtf`)

Optional local compile check (does **not** replace the official KaiPath release process):

```bash
bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# → mods/kaipath-1.3.7-lm-ultra.jar  (dev-only smoke artifact)
```

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.136**).
