# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.122 + KaiPath 1.3.7):** Live/released KaiPath **1.3.7** (CurseForge) already
honors `style`, `activeTab`, `titleAlign`, and `playerModel`, and advertises those four via
`GuiFeaturesPacket`. **Still missing:** GuiOpen field 11 `inputsJson` text fields +
`textInput` advertise (blocks Difficulty Weights typed entry).

Reference kit under [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/)
implements the remaining text-input path for the next KaiPath rebuild.

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

## Confirmed on KaiPath 1.3.7 (released)

| Feature | Notes |
|---------|-------|
| Decode GuiOpen fields 1–10 | yes |
| Dynamic title / tabs / lines / buttons | yes |
| Button `style` | yes — `UltraUiAccess.ButtonStyle` |
| `layoutJson.titleAlign` = `center` | yes |
| `activeTab` highlight | yes |
| `playerModel` entity preview | yes — `LmDynamicScreen$Preview` |
| GuiFeaturesPacket | yes — `["style","activeTab","titleAlign","playerModel"]` |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Still needed (next KaiPath after 1.3.7)

| Feature | Where | Client TODO |
|---------|-------|-------------|
| Text fields | GuiOpenPacket **field 11** `inputsJson` | Render `type=text`; on Enter / `args.bindInput` click, send `args.value` |
| Advertise `textInput` | GuiFeaturesPacket | Expand to `["style","activeTab","titleAlign","playerModel","textInput"]` |

## Advertise (after textInput lands)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\"]");
```

## Text input contract (LM 4.6.108+)

Config + Difficulty Weights send field 11. 1.3.7 ignores the trailing field. See reference
`LmDynamicScreen` for the EditBox + `bindInput` implementation to merge.

## Merge checklist (JLDK) — textInput only

1. Add `inputsJson` as GuiOpen field 11 (append-tolerant decode).
2. Render `type=text` EditBoxes; Enter / `bindInput` → `args.value`.
3. Add `textInput` to `ADVERTISED_FEATURES_JSON`.
4. Ship KaiPath past **1.3.7**; confirm `/lm admin guitest` reports no missing optional features.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview 1.3.7 already draws.
