# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.122** + **KaiPath 1.3.7** (CurseForge / live). Official KaiPath updates are owned by the KaiPath mod developer — LM does not ship or deploy KaiPath feature jars.

## Still missing on KaiPath 1.3.7 (for JLDK)

Stock CurseForge **1.3.7** still lacks field 11 / `textInput`. Merge [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) into the next official KaiPath release:

| Part | Stock 1.3.7 | Reference kit |
|------|-------------|-----------------|
| GuiOpen **field 11** `inputsJson` | Missing | **Done** |
| EditBox / `type=text` + `bindInput` → `args.value` | Missing | **Done** |
| `textInput` in GuiFeaturesPacket advertise | Missing | **Done** (`[…,"textInput"]`) |
| Explicit UTF-8 GuiOpen helpers | MC `readUtf` | **Done** (kit helpers) |

LM 4.6.122+ already ships field 11 and logs `pendingNext=[textInput]` on stock 1.3.7 (info, not a broken-client warn). After the official KaiPath merge, guitest should show `pendingNext=none`.

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
| UTF-8 GuiOpen strings | MC `readUtf` | Explicit UTF-8 helpers | Wire-compatible; still saw `â€` mojibake — LM uses ASCII dashes |
| Text field widgets (field 11) | **Missing** | **Done** | No `inputsJson` / EditBox / `bindInput` — **Weight shop blocker** |
| GuiFeaturesPacket advertise | **Partial** | Full | 1.3.7: `["style","activeTab","titleAlign","playerModel"]` — no `textInput` |
| Slider / toggle widgets | N/A | N/A | Reserved until LM ships types |

**Only hard blocker for Weights / Config typed entry:** merge reference `inputsJson` + `textInput` advertise into a KaiPath build past 1.3.7. LM 4.6.122+ treats style/activeTab/titleAlign/playerModel as shipped on 1.3.7 and logs `textInput` as `pendingNext` (not a broken-client warning). Everything below is “works but watch out.”

## Practical pitfalls (beyond text input)

### 1. Text input on stock 1.3.7 fails silently on the client

When LM sends field 11, stock KaiPath **1.3.7** ignores it — no crash, no EditBox. **Mitigation:** LM 4.6.122+ shows a danger line when `textInput` is missing; **fix:** next official KaiPath merges the kit (JLDK).

### 2. UTF-8 is “wire-compatible” but still caused mojibake

Docs call MC `readUtf` and LM’s explicit UTF-8 helpers compatible, but live screenshots showed `â€` garbage on en-dashes / fancy punctuation. Workaround since **4.6.116:** ASCII dashes / plain punctuation in Ultra copy. Any new special characters can break again until JLDK fixes decode properly on the client.

### 3. Player model custom parts may not fully render

1.3.7 draws `playerModel`, but schema v1 `playerModel.parts` (`headBones` / `form` / `extras`) is LM-side. Unclear whether 1.3.7 renders every custom head/form part or mostly the base entity preview. Demon horns appeared in at least one screenshot — verify all part types before relying on cosmetics.

### 4. Limited graceful degradation for missing features

Older KaiPath (or no KaiPath) still hits install / broken-menu paths more than per-widget hide. `GuiFeaturesPacket` negotiation exists; LM uses it for staff `guitest` + login gap logs and for Weights/Config textInput copy, but does **not** yet hide every optional chrome (styles, tabs, preview) feature-by-feature when the client omits it. Expect degraded look, not a tailored subset UI.

### 5. Slider / toggle types are deferred

Booleans stay styled buttons; numbers stay text + ± steppers. Both sides need work before `inputsJson` can carry real `slider` / `toggle` types.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. When added, prefer extending field 11 rather than a new packet field.

Until then, BOOLEAN uses styled toggle **buttons**; NUMBER uses **text** + ± step buttons.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
