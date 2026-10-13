# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.129 + KaiPath 1.3.9):** Live KaiPath **1.3.9** honors `style`, `activeTab`,
`titleAlign`, `playerModel`, and **`textInput`** (GuiOpen field 11 / EditBox / `bindInput`).
Weights shop typed entry is unblocked.

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

## Confirmed on KaiPath 1.3.9

| Feature | Notes |
|---------|-------|
| Decode GuiOpen fields 1–11 | yes (incl. `inputsJson`) |
| Dynamic title / tabs / lines / buttons | yes |
| Button `style` | yes |
| `layoutJson.titleAlign` = `center` | yes |
| `activeTab` highlight | yes |
| `playerModel` entity preview | yes |
| Text fields + `bindInput` → `args.value` | yes |
| GuiFeaturesPacket | yes — `["style","activeTab","titleAlign","playerModel","textInput"]` |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Advertise

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\"]");
```

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.

## Practical pitfalls

See [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Practical pitfalls.
