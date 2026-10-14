# Ultra GUI — implement-yourself kit (for KaiPath / JLDK)

Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix / pitfalls: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).
JLDK brief: [`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`](../../docs/KAIPATH_JLDK_CLIENT_BRIEF.md).

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
3. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` / `headerBottom` / `accent` when needed); handle clicks verbatim.

---

## KaiPath / JLDK — merge into next official release

**Released 1.3.9** already has style / activeTab / titleAlign / playerModel / textInput + GuiFeaturesPacket (5 features).

**Still merge from this kit** (clears LM `pendingNext`):

1. Honor **`layoutJson.headerBottom`** — title text + yellow divider stay **strictly above** this panel-relative Y (default **28** with under-title tabs, **42** without)
2. Honor **`layoutJson.accent`** + per-button/tab **`accent`** — single-letter Minecraft color codes; paint title, activeTab highlight, and hub/staff tile tint
3. Advertise the full set:
   `["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent"]`
4. **Title vertical centering** (no new field) — center title in the header band above `headerBottom`

Optional local compile check (does **not** replace the official KaiPath release process):

```bash
bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# → mods/kaipath-1.3.9-lm-ultra.jar  (dev-only smoke artifact)
```

---

LM server impl: `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.151**).
