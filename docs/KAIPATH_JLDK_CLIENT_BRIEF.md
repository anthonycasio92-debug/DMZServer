# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.154 live + KaiPath 1.3.13):** Client honors style / activeTab / titleAlign /
playerModel / textInput / headerBottom / accent / palette / sound / buttonSkin / frame /
paged_book. LM emits button **`footer:true`** and **`portraitFrame`**.

**Open gaps for next KaiPath:** honor + advertise `fixedFooter`; advertise `portraitFrame`
(already paints); confirm title vertical centering in the gear-icon band.

Full matrix + pitfalls: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
| Bridge gap matrix + pitfalls | [`docs/KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |
| KaiPath client sources (reference) | [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) |
| Server codecs (any mod) | [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) |

---

## Governing rule

The client renders what the server sends — **no hardcoded screen/tab/button/action lists**.

## Confirmed on KaiPath 1.3.13

| Feature | Notes |
|---------|-------|
| Decode GuiOpen fields 1–11 | yes (incl. `inputsJson`) |
| Dynamic title / tabs / lines / buttons | yes |
| Button `style` | yes |
| `layoutJson.titleAlign` = `center` | yes |
| `activeTab` highlight | yes (tint from `accent`) |
| `playerModel` entity preview | yes |
| Text fields + `bindInput` → `args.value` | yes |
| `layoutJson.headerBottom` / `tabsUnderTitle` | yes |
| `layoutJson.accent` + button/tab `accent` | yes (`UltraBridgeChrome`) |
| `palette` / `sound` / `buttonSkin` / `frame` / `paged_book` | yes |
| `playerModel.portraitFrame` | paints when present — **not advertised yet** |
| GuiFeaturesPacket | yes — see gap matrix advertise set |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Open gaps (do these next)

### 1. Button `footer:true` / `fixedFooter` — **blocking**

LM pins dual footer controls with `"footer": true` on Back + Main menu (or contextual Back).
**Keep those buttons in fixed chrome** so they do not scroll with the body. Today
`contentBottom = panelY + panelH - 28` ignores `footer` — raise the content band above the
footer height when any `footer:true` button is present.

Advertise: `"fixedFooter"`.

### 2. Advertise `portraitFrame`

Already painted when `playerModel.portraitFrame` is a valid `ns:path`. Add `"portraitFrame"`
to GuiFeatures so guitest stops treating it as unsupported.

### 3. Title vertical centering — confirm

1.3.13 already centers in the header band via `(headerBottom - 2 - 9) / 2`. Confirm
alignment between the gear icons on all themes. No new protocol field.

## Advertise (after fixedFooter + portraitFrame)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"portraitFrame\",\"fixedFooter\",\"combined.forms.v1\"]");
```

LM guitest then moves from `pendingNext=[fixedFooter]` → `pendingNext=none`.

## Pitfalls (must-read)

Full list: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Pitfalls & traps.

1. **Server jar ≠ client** — `mods/kaipath-*.jar` on the dedicated server does not paint Ultra; players need the client mod.
2. **Chrome RLs are enable tokens** — `buttonSkin` / `frame` / `portraitFrame` do not load textures; missing `sound` is a silent no-op. LM uses `minecraft:ui.button.click` + `legacymechanics:*` so DMZUltra is not required.
3. **Spelling** — advertise `pagedBook`; widget `type` is `paged_book`.
4. **Do not add wire fields** for `titleTint` / `contentTop` / `contentBottom` / `contentScroll` — fix footer band on the client via `fixedFooter`.
5. Keep ASCII `fitLabel` ellipsis (`...`).

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in the gap matrix.
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.
