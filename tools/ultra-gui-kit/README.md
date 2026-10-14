# Ultra GUI — implement-yourself kit (any Forge mod + KaiPath / JLDK)

Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix / pitfalls: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).
JLDK brief: [`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`](../../docs/KAIPATH_JLDK_CLIENT_BRIEF.md).

**This bridge is multi-mod.** Every server mod owns its own `{modId}:gui` channel. Packet
codecs never embed a mod id. LegacyMechanics is one partner example — not the owner of the
protocol. KaiPath registers one client consumer per partner mod id.

| Who you are | What to pull |
|-------------|--------------|
| **Any server mod author** | [`server/`](server/) — copy codecs + channel helper; rename package to yours |
| **KaiPath / JLDK** | [`kaipath-client/`](kaipath-client/) — merge into the next official KaiPath release; add partner mod ids to [`UltraGuiPartners`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/UltraGuiPartners.java) |

**Server mods do not ship or deploy KaiPath updates.** The KaiPath mod developer owns the official jar. This kit is the handoff for what to merge.

---

## Server mod authors (any mod)

1. Copy `server/*.java` into your mod; rename the `com.example.ultragui` package.
2. Register `{yourModId}:gui` protocol `"1"`:
   - msg 0 = open (`UltraGuiOpenPacket`)
   - msg 1 = action (`UltraGuiActionPacket`)
   - msg 2 = features (`UltraGuiFeaturesPacket`, optional C→S)
3. Ask JLDK / KaiPath to add your Forge mod id to `UltraGuiPartners.DEFAULT_PARTNERS` (or call `UltraGuiPartners.register("yourmod")` on the client).
4. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` / `headerBottom` / `accent` when needed). Prefer plain ASCII labels; tint via `accent` / `style`.
5. Handle clicks verbatim (`actionId` + `argsJson` echoed from the button).

See [`server/HelloWorldServerSketch.java`](server/HelloWorldServerSketch.java).

---

## KaiPath / JLDK — merge into next official release

**Released 1.3.9** already has style / activeTab / titleAlign / playerModel / textInput (5 features).

**Still merge from this kit** (shared renderer for every partner channel):

1. Honor **`layoutJson.headerBottom`** — title + yellow divider strictly above this Y
2. Honor **`layoutJson.accent`** + per-button/tab **`accent`**
3. Advertise on **each** registered channel:
   `["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent"]`
4. Title vertical centering in the header band (no new field)
5. Multi-mod partner list: [`UltraGuiPartners`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/UltraGuiPartners.java) — register every server mod that ships Ultra menus, not only `legacymechanics`

```java
// Client boot (already wired via LmUltraConsumer.bootstrap):
UltraGuiPartners.registerDefaults();
// Or ad-hoc:
UltraGuiPartners.register("legacymechanics", "myshop", "otheremod");
```

Optional local compile check (does **not** replace the official KaiPath release process):

```bash
bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# → mods/kaipath-1.3.9-lm-ultra.jar  (dev-only smoke artifact)
```

---

Reference server impl (one partner): `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.151**).
