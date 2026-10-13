# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.112** — what the bridge already sends vs what KaiPath must still implement.

## Cursor / LM (server protocol) — status

| Gap | Status | Notes |
|-----|--------|-------|
| Button `style` on every button | **Done** | Wire always emits `style`. Roles: `confirm` / `danger` / `dim` / `normal`. Destructive actions use `danger`; unavailable use `dim`; positive confirms use `confirm`. |
| Text input (`inputsJson` field 11) | **Done** | `type=text` + `args.bindInput` → `args.value` on Apply / Enter. Config TEXT, NUMBER, and ENUM detail pages use text fields. |
| `playerModel.parts` (cosmetics) | **Done** (4.6.111) | Schema 1: `headBones` / `form` / `extras`. See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). |
| Slider / toggle as input types | **Deferred** | Nice-to-have. Config BOOLEAN stays button toggles (`confirm`/`dim` by state). Reserved types documented below; not shipped on wire yet. |

## JLDK / KaiPath (client) — still needed

| Gap | Status | Notes |
|-----|--------|-------|
| Button color rendering | **Missing** | Map `confirm`→green, `danger`→red, `dim`→dark gray, `normal`→default. LM already sends styles. |
| Player model draw | **Missing** | Bounds (+ optional `parts`) are in `layoutJson.playerModel`; KaiPath parses but does not draw the entity. |
| UTF-8 decode | **Missing on client** | LM encodes/decodes GuiOpen with explicit `StandardCharsets.UTF_8`. Mojibake (é/à) is KaiPath platform-default decode — fix on JLDK side. |
| Text field widgets | **Reference done / live jar missing** | `kaipath-client` `LmDynamicScreen` renders field 11 + `bindInput`→`args.value`. Live `kaipath-1.3.1.jar` still lacks this — **blocker for Weight shop typed entry**. |
| GuiFeaturesPacket advertise | **Partial** | Reference advertises `["titleAlign","activeTab","textInput"]`. Live jar still baseline until rebuilt. |
| Slider / toggle widgets | N/A until LM ships types | Reserved; do not block text input. |

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
