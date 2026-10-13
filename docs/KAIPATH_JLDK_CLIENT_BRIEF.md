# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.120 + live KaiPath 1.3.5 + reference `kaipath-client`):** LM sends styles,
text inputs, titleAlign, activeTab, and `playerModel` (+ optional `parts`). Live **1.3.5**
already centers titles and highlights `activeTab`. The **reference kit** under
[`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) implements the
rest of the optional set — merge into 1.3.5 and rebuild (especially `textInput` for the
Difficulty Weights shop).

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

## Confirmed on KaiPath 1.3.5 (live)

| Feature | Notes |
|---------|-------|
| Decode GuiOpen fields 1–10 | yes |
| Dynamic title / tabs / lines / buttons | yes |
| `layoutJson.titleAlign` = `center` | yes — titles centered |
| `activeTab` highlight | yes |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Reference kit implements — merge into live KaiPath

| Feature | Where | Client behavior |
|---------|-------|-----------------|
| Button `style` | each button | Chrome tint: `confirm`→green, `danger`→red, `dim`→dark-gray, `normal`→default |
| `playerModel` | `layoutJson.playerModel` | Draw local player in bounds; turntable when `rotate`; honor optional `parts` when geo helpers exist |
| Text fields | GuiOpenPacket **field 11** `inputsJson` | Render `type=text`; on Enter / `args.bindInput` click, send `args.value` |
| UTF-8 strings | all GuiOpen string fields | Decode with **UTF-8** (`GuiOpenPacket.readUtf8`) |
| GuiFeaturesPacket | after channel register | Advertise full set below |

## Advertise (reference default)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\"]");
```

(`UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON` already holds this string.)

## Text input contract (LM 4.6.108+)

Config TEXT / NUMBER / ENUM detail pages and Difficulty **Weights** shop send:

```json
[{
  "id": "@difficulty:input:weight_value",
  "type": "text",
  "x": 16, "y": 200, "w": 318, "h": 18,
  "value": "",
  "placeholder": "e.g. 100",
  "maxLength": 5,
  "args": { "screen": "difficulty", "page": "weights_input:turtle_shell", "system": "difficulty" }
}]
```

Apply / Continue includes `"bindInput":"@difficulty:input:weight_value"`. Before sending the
click, copy that field’s text into `args.value`. Enter on the field may send the input `id`
as `actionId` with `args.value` set.

## Merge checklist (JLDK)

1. Copy / merge `tools/ultra-gui-kit/kaipath-client/` over the live KaiPath Ultra consumer package.
2. Ensure `GuiOpenPacket` has **11 fields** including `inputsJson` and UTF-8 helpers.
3. Ensure `LmDynamicScreen` renders styles, text fields, and `playerModel`.
4. Keep `ADVERTISED_FEATURES_JSON` as the full five-feature array.
5. Rebuild and ship a KaiPath jar past **1.3.5** with this kit; confirm `/lm admin guitest` reports no missing optional features.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md); BOOLEAN stays styled buttons for now.
- Full DMZUltra geo application of `playerModel.parts` (headBones / form) — bounds + local entity draw ship first; parts hook is present for geo integration.
