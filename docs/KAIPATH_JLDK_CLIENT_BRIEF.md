# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.154 live + KaiPath 1.3.17):** Client honors style / activeTab / titleAlign /
playerModel / textInput / headerBottom / accent / palette / sound / buttonSkin / frame /
paged_book / **fixedFooter** / **portraitFrame** (advertised). LM emits button **`footer:true`**
(unchanged wire — matches 1.3.17).

**Open gaps:** confirm title vertical centering in the gear-icon band only.

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

## Confirmed on KaiPath 1.3.17

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
| `playerModel.portraitFrame` | paints + **advertised** |
| Button `footer:true` / `fixedFooter` | pins + auto spacing + **advertised** |
| GuiFeaturesPacket | yes — see gap matrix advertise set |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Remaining (confirm only)

### Title vertical centering

1.3.13+ centers in the header band via `(headerBottom - 2 - 9) / 2`. Confirm alignment
between the gear icons on all themes. No new protocol field.

## Advertise (1.3.17)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"portraitFrame\",\"fixedFooter\",\"combined.forms.v1\"]");
```

LM guitest: `missingShipped=none pendingNext=none`.

## Pitfalls (must-read)

Full list: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Pitfalls & traps.

1. **Server jar ≠ client** — `mods/kaipath-*.jar` on the dedicated server does not paint Ultra; players need the client mod.
2. **Chrome RLs are enable tokens** — `buttonSkin` / `frame` / `portraitFrame` do not load textures; missing `sound` is a silent no-op. LM uses `minecraft:ui.button.click` + `legacymechanics:*` so DMZUltra is not required.
3. **Spelling** — advertise `pagedBook`; widget `type` is `paged_book`.
4. **Do not add wire fields** for `titleTint` / `contentTop` / `contentBottom` / `contentScroll`.
5. Keep ASCII `fitLabel` ellipsis (`...`).

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in the gap matrix.
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.
