# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.149 + KaiPath 1.3.9):** Live KaiPath **1.3.9** honors `style`, `activeTab`,
`titleAlign`, `playerModel`, and **`textInput`** (GuiOpen field 11 / EditBox / `bindInput`).
LM 4.6.149 adds **`layoutJson.headerBottom`** + **`layoutJson.accent`** / button **`accent`**
— KaiPath must honor these next (see below).

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
| `activeTab` highlight | yes (currently shared yellow — see accent) |
| `playerModel` entity preview | yes |
| Text fields + `bindInput` → `args.value` | yes |
| GuiFeaturesPacket | yes — `["style","activeTab","titleAlign","playerModel","textInput"]` |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Open — protocol geometry + accent (LM 4.6.149 ships the fields)

### 1. `layoutJson.headerBottom` (tabs under the yellow divider)

LM places tabs as regular buttons at Y=`headerBottom` with `tabsUnderTitle=true`, avoiding
KaiPath's native title-chrome tab strip. **Client must keep title text + yellow divider
strictly above `headerBottom`.** Default with tabs: **28**; without: **42**.

This is the only clean fix for tab/divider collision. Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) §4.

### 2. Title vertical centering (JLDK-only)

Title Y is 100% KaiPath chrome. Center the title text in the header band between the gear
icons. No further protocol field.

### 3. `layoutJson.accent` + button `accent`

Single-letter Minecraft color codes (no section-sign in title/button labels — those mojibake).

| System | Code |
|--------|------|
| Difficulty | `c` |
| Rival | `5` |
| Sparring | `6` |
| Prestige | `b` |
| Character Services | `a` |
| Skill Check | `e` |
| Staff Admin | `4` |
| Hub title | `6` (gold) |

- Paint **title** from `layoutJson.accent`.
- Paint **`activeTab` highlight** from `layoutJson.accent` (replaces shared yellow).
- Paint hub tile / staff control tint from per-button `accent` when present.
- Button `style` stays semantic (`confirm` / `danger` / `dim` / `normal`).

## Advertise (when accent + headerBottom ship client-side)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\",\"headerBottom\",\"accent\"]");
```

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.

## Practical pitfalls

See [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Practical pitfalls.
