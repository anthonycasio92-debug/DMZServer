# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.120** — what the bridge already sends vs what the KaiPath reference client implements.

## Cursor / LM (server protocol) — status

| Gap | Status | Notes |
|-----|--------|-------|
| Button `style` on every button | **Done** | Wire always emits `style`. Roles: `confirm` / `danger` / `dim` / `normal`. |
| Text input (`inputsJson` field 11) | **Done** | `type=text` + `args.bindInput` → `args.value` on Apply / Enter. Config + Difficulty Weights shop. |
| `playerModel.parts` (cosmetics) | **Done** (4.6.111) | Schema 1: `headBones` / `form` / `extras`. See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). |
| Slider / toggle as input types | **Deferred** | BOOLEAN stays styled buttons; NUMBER uses text + ±. Reserved below. |

## JLDK / KaiPath (client) — reference kit vs live jar

Sources: [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/).

| Gap | Reference kit | Live KaiPath **1.3.5** | Notes |
|-----|---------------|------------------------|-------|
| Button `style` colors | **Done** | Missing | `confirm`→green, `danger`→red, `dim`→gray chrome + label tint |
| `activeTab` highlight | **Done** | **Done** | Chip under active tab; LM also styles tab labels |
| `titleAlign` | **Done** | **Done** | Centers when `layoutJson.titleAlign=center` |
| `playerModel` draw | **Done** | Missing | Local player in bounds; turntable when `rotate`; `parts` logged / ready for geo |
| UTF-8 GuiOpen strings | **Done** | Use MC `readUtf` (UTF-8) | Reference uses explicit `StandardCharsets.UTF_8` length-prefix (wire-compatible) |
| Text field widgets (field 11) | **Done** | **Missing** | EditBox from `inputsJson`; Enter / `bindInput` → `args.value` — **Weight shop blocker** |
| GuiFeaturesPacket advertise | **Done** | Missing | `["style","activeTab","titleAlign","playerModel","textInput"]` |
| Slider / toggle widgets | N/A | N/A | Reserved until LM ships types |

**Ship path:** merge `tools/ultra-gui-kit/kaipath-client/` into KaiPath **1.3.5** and ship a rebuild (e.g. 1.3.6+). Until then LM Weight shop input pages show a clear “requires KaiPath update” message when `textInput` is not advertised.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. When added, prefer extending field 11 rather than a new packet field:

```json
[
  {
    "id": "@config:input:difficulty|some.bool",
    "type": "toggle",
    "x": 16, "y": 200, "w": 80, "h": 18,
    "value": "true",
    "args": { "screen": "config", "page": "detail:…" }
  },
  {
    "id": "@config:input:difficulty|some.number",
    "type": "slider",
    "x": 16, "y": 230, "w": 240, "h": 18,
    "value": "50",
    "min": 0,
    "max": 100,
    "step": 1,
    "args": { "screen": "config", "page": "detail:…" }
  }
]
```

Until then, BOOLEAN uses styled toggle **buttons**; NUMBER uses **text** + ± step buttons.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
