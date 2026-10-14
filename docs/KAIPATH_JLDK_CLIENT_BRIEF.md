# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.153 + KaiPath 1.3.13):** Live KaiPath **1.3.13** honors `style`, `activeTab`,
`titleAlign`, `playerModel`, `textInput`, **`headerBottom`**, **`accent`** (plus palette /
sound / buttonSkin / frame / pagedBook). LM already emits button **`footer:true`** —
that is the open advertise-able bridge gap (plus title vertical centering, which needs no
new field).

Full gap matrix: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
| Bridge gap matrix | [`docs/KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |
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
| GuiFeaturesPacket | yes — see advertise set in gap matrix |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Open gaps (do these next)

### 1. Button `footer:true` / `fixedFooter`

LM pins dual footer controls with `"footer": true` on Back + Main menu (or contextual Back).
**Keep those buttons in fixed chrome** so they do not scroll with the body. Spec:
[`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md).

Advertise: `"fixedFooter"`.

### 2. Title vertical centering — JLDK-only

Center the title text in the header band between the gear icons. No protocol field.

## Advertise (after implementing fixedFooter)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"fixedFooter\",\"combined.forms.v1\"]");
```

LM guitest then moves from `pendingNext=[fixedFooter]` → `pendingNext=none`.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.

## Practical pitfalls

See [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Practical pitfalls.
