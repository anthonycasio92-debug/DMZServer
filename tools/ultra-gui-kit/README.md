# Ultra GUI — implement-yourself kit (any Forge mod + KaiPath / JLDK)

Protocol + conformance: [`docs/KAIPATH_GUI_PROTOCOL.md`](../../docs/KAIPATH_GUI_PROTOCOL.md).
Gap matrix / pitfalls: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md).
DMZUltra asset reuse: [`docs/DMZULTRA_ASSET_REUSE.md`](../../docs/DMZULTRA_ASSET_REUSE.md).
JLDK brief: [`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`](../../docs/KAIPATH_JLDK_CLIENT_BRIEF.md).
Non-LM chrome fixture: [`server/BridgeFutureFieldsFixture.json`](server/BridgeFutureFieldsFixture.json).
fixedFooter merge notes: [`kaipath-client/.../FIXED_FOOTER.md`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/FIXED_FOOTER.md).

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
4. Send opens with `style` + `activeTab` (+ `inputsJson` / `playerModel` / `headerBottom` / `palette` when needed). Prefer plain ASCII labels; tint via `palette` / `style`.
5. Handle clicks verbatim (`actionId` + `argsJson` echoed from the button).
6. For chrome (`sound`, `buttonSkin`, `frame`, `paged_book`, `portraitFrame`), prefer fail-open tokens (`minecraft:ui.button.click`, `legacymechanics:*`) — see pitfalls. Never bundle client asset files into your server jar.
7. Pin dual footer with `"footer": true` on Back / Main menu (client must advertise `fixedFooter`).

See [`server/HelloWorldServerSketch.java`](server/HelloWorldServerSketch.java) and
[`server/BridgeFutureFieldsFixture.json`](server/BridgeFutureFieldsFixture.json).

---

## KaiPath / JLDK — merge into next official release

**Released 1.3.13** already honors style / activeTab / titleAlign / playerModel / textInput /
headerBottom / accent / palette / sound / buttonSkin / frame / paged_book / portraitFrame paint.

**Still merge from this kit** (shared renderer for every partner channel):

1. Honor **`footer:true` / `fixedFooter`** — pin footer buttons; raise `contentBottom` above the
   footer band; keep footer widgets **out of** `scrollWidgets`. Helpers:
   [`UltraBridgeFooter`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/UltraBridgeFooter.java)
   + [`FIXED_FOOTER.md`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/FIXED_FOOTER.md)
2. Advertise **`portraitFrame`** + **`fixedFooter`** on every registered channel
3. Keep advertise camelCase **`pagedBook`**; widget type snake_case **`paged_book`**
4. Title vertical centering in the header band (no new field) — confirm gear-icon alignment
5. Multi-mod partner list: [`UltraGuiPartners`](kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/UltraGuiPartners.java)
6. **ASCII `fitLabel`** — `UltraWidgetReflect.fitLabel` must use `...` (not mojibaked `…`). See [`FITLABEL_ASCII.md`](kaipath-client/com/dmzlegacy/kaipath/ultra/internal/FITLABEL_ASCII.md)

### Pitfalls (must-read before release)

Full list: [`docs/KAIPATH_BRIDGE_GAPS.md`](../../docs/KAIPATH_BRIDGE_GAPS.md) § Pitfalls.

1. Server jar ≠ client paint
2. Chrome RLs are enable tokens (no texture bind); missing sound is silent
3. Do not add wire fields for `titleTint` / `contentTop` / `contentBottom` / `contentScroll`
4. Prefer `minecraft:` / `legacymechanics:` defaults so DMZUltra-absent clients stay healthy

### Target advertise

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"portraitFrame\",\"fixedFooter\",\"combined.forms.v1\"]");
```

```java
// Client boot (already wired via LmUltraConsumer.bootstrap):
UltraGuiPartners.registerDefaults();
// Or ad-hoc:
UltraGuiPartners.register("legacymechanics", "myshop", "otheremod");
```

Optional local compile check (does **not** replace the official KaiPath release process):

```bash
KAIPATH_BASE_JAR=mods/kaipath-1.3.13.jar \
KAIPATH_OVERLAY_VERSION=1.3.13-lm-ultra \
  bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# → mods/kaipath-1.3.13-lm-ultra.jar  (dev-only smoke artifact)
```

---

Reference server impl (one partner): `tools/dmz-adaptive-difficulty/…/net/gui/` (**4.6.154**).
