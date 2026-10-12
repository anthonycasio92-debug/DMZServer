# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.112 + KaiPath 1.3.5):** LM already sends styles, text inputs, titleAlign,
activeTab, and `playerModel` (+ optional `parts`). KaiPath 1.3.5 centers titles and
highlights the active tab. **Still needed on KaiPath:** button colors, entity preview
draw, UTF-8 decode, and text-field widgets.

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

## Confirmed on KaiPath 1.3.5

| Feature | Notes |
|---------|-------|
| Decode GuiOpen fields 1–10 | yes |
| Dynamic title / tabs / lines / buttons | yes |
| `layoutJson.titleAlign` = `center` | yes — titles centered |
| `activeTab` highlight | yes |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## LM already sends — KaiPath still needed

| Feature | Where | Client TODO |
|---------|-------|-------------|
| Button `style` | each button | Map `confirm`→green, `danger`→red, `dim`→dark-gray, `normal`→default |
| `playerModel` | `layoutJson.playerModel` | Draw local player in bounds. Honor optional `parts` (schema 1: `headBones`, `form`, `extras`) |
| Text fields | GuiOpenPacket **field 11** `inputsJson` | Render `type=text`; on Enter / `args.bindInput` button click, send `args.value` |
| UTF-8 strings | all GuiOpen string fields | Decode with **UTF-8** (not platform default) — LM encodes with `StandardCharsets.UTF_8` |

## Advertise (after implementing)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\"]");
```

## Text input contract (LM 4.6.108+)

Config TEXT / NUMBER / ENUM detail pages send:

```json
[{
  "id": "@config:input:difficulty|some.path",
  "type": "text",
  "x": 16, "y": 200, "w": 318, "h": 18,
  "value": "current",
  "placeholder": "Type a value…",
  "maxLength": 128,
  "args": { "screen": "config", "page": "detail:…", "system": "config" }
}]
```

Apply button includes `"bindInput":"@config:input:…"`. Before sending the click,
copy that field’s text into `args.value`. Enter on the field may send the input `id`
as `actionId` with `args.value` set.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md); BOOLEAN stays styled buttons for now.
