# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.121** + **KaiPath 1.3.7** (CurseForge latest; no 1.3.5 was ever published).

## Cursor / LM (server protocol) — status

| Gap | Status | Notes |
|-----|--------|-------|
| Button `style` on every button | **Done** | Wire always emits `style`. Roles: `confirm` / `danger` / `dim` / `normal`. |
| Text input (`inputsJson` field 11) | **Done** | `type=text` + `args.bindInput` → `args.value`. Config + Difficulty Weights shop. |
| `playerModel.parts` (cosmetics) | **Done** (4.6.111) | Schema 1: `headBones` / `form` / `extras`. See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). |
| Slider / toggle as input types | **Deferred** | BOOLEAN stays styled buttons; NUMBER uses text + ±. Reserved below. |

## JLDK / KaiPath (client) — 1.3.7 vs reference kit

Sources: [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/).
Vendored jar: `mods/kaipath-1.3.7.jar`.

| Gap | KaiPath **1.3.7** (released) | Reference kit | Notes |
|-----|------------------------------|---------------|-------|
| Button `style` colors | **Done** | Done | `UltraUiAccess.ButtonStyle` CONFIRM/DANGER/DIM/SELECTED |
| `activeTab` highlight | **Done** | Done | |
| `titleAlign` | **Done** | Done | |
| `playerModel` draw | **Done** | Done | `LmDynamicScreen$Preview` + InventoryScreen |
| UTF-8 GuiOpen strings | MC `readUtf` | Explicit UTF-8 helpers | Wire-compatible |
| Text field widgets (field 11) | **Missing** | **Done** | No `inputsJson` / EditBox / `bindInput` — **Weight shop blocker** |
| GuiFeaturesPacket advertise | **Partial** | Full | 1.3.7: `["style","activeTab","titleAlign","playerModel"]` — no `textInput` |
| Slider / toggle widgets | N/A | N/A | Reserved until LM ships types |

**Only remaining Ultra gap for Weights typed entry:** merge reference `inputsJson` + `textInput` advertise into a KaiPath build past 1.3.7.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. When added, prefer extending field 11 rather than a new packet field.

Until then, BOOLEAN uses styled toggle **buttons**; NUMBER uses **text** + ± step buttons.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
