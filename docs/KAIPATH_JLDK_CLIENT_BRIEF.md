# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.108 + KaiPath 1.3.5):** Title centering and active-tab highlight are on
both sides. Button styles, player model draw, and text inputs still need KaiPath work
(LM already sends the data).

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
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
| `playerModel` | `layoutJson.playerModel` | Draw local player in bounds (Character). Honor optional `parts` (schema 1: `headBones`, `form`, `extras`) — see protocol doc |
| Text fields | GuiOpenPacket **field 11** `inputsJson` | Render `type=text`; on Enter / `args.bindInput` button click, send `args.value` |

## Advertise (after implementing)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\"]");
```

## Text input contract (new in LM 4.6.108)

`inputsJson` example:

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
