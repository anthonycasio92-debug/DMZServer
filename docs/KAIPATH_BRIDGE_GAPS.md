# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.122** + **KaiPath 1.3.7** (CurseForge latest / live; no 1.3.5 was ever published).

## Still missing on KaiPath 1.3.7 (live)

| Missing part | Blocks | Where to merge |
|--------------|--------|----------------|
| GuiOpen **field 11** `inputsJson` | Weight shop typed entry; Config typed fields | `tools/ultra-gui-kit/kaipath-client/` → next KaiPath past 1.3.7 |
| EditBox / `type=text` widgets | same | same |
| `bindInput` → `args.value` | same | same |
| `textInput` in GuiFeaturesPacket advertise | guitest `pendingNext`; LM treats as expected until next client | expand to `["style","activeTab","titleAlign","playerModel","textInput"]` |

LM 4.6.122+ already ships field 11 and logs `pendingNext=[textInput]` (info), not a broken-client warn. Shipped-on-1.3.7 features (`style` / `activeTab` / `titleAlign` / `playerModel`) warn only when absent.

Deferred (neither side): `inputsJson` `slider` / `toggle`.

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

**Only remaining Ultra gap for Weights / Config typed entry:** merge reference `inputsJson` + `textInput` advertise into a KaiPath build past 1.3.7. LM 4.6.122+ treats style/activeTab/titleAlign/playerModel as shipped on 1.3.7 and logs `textInput` as `pendingNext` (not a broken-client warning).

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. When added, prefer extending field 11 rather than a new packet field.

Until then, BOOLEAN uses styled toggle **buttons**; NUMBER uses **text** + ± step buttons.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
