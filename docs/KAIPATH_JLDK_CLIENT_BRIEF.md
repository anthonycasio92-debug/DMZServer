# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.151 + KaiPath 1.3.9):** Live KaiPath **1.3.9** honors `style`, `activeTab`,
`titleAlign`, `playerModel`, and **`textInput`**. LM already emits **`headerBottom`** and
**`accent`** — those are the open advertise-able bridge gaps (plus title vertical centering,
which needs no new field).

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
| `activeTab` highlight | yes (shared yellow until `accent`) |
| `playerModel` entity preview | yes |
| Text fields + `bindInput` → `args.value` | yes |
| GuiFeaturesPacket | yes — five features (see below) |
| Verbatim GuiAction echo | yes |
| Any `screenId` | yes |

## Open gaps (do these next)

### 1. `layoutJson.headerBottom` — tabs under the yellow divider

LM places tabs as regular buttons at Y=`headerBottom` with `tabsUnderTitle=true`.
**Keep title text + yellow divider strictly above `headerBottom`.** Defaults: **28** with
tabs, **42** without. Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) §4.

Advertise: `"headerBottom"`.

### 2. Title vertical centering — JLDK-only

Center the title text in the header band between the gear icons. No protocol field.

### 3. `layoutJson.accent` + button/tab `accent`

Single-letter Minecraft color codes (no section-sign in title/button labels).

| System | Code |
|--------|------|
| Difficulty | `c` |
| Rival | `5` |
| Sparring | `6` |
| Prestige | `b` |
| Character Services | `a` |
| Skill Check | `e` |
| Staff Admin | `4` |
| Main hub title | *(omit — shared gold)* |

- Paint **title** from `layoutJson.accent`.
- Paint **`activeTab` highlight** from `layoutJson.accent` (replaces shared yellow).
- Paint hub tile / staff control tint from per-button `accent` when present.
- Button `style` stays semantic (`confirm` / `danger` / `dim` / `normal`).

Advertise: `"accent"`.

## Advertise (after implementing gaps 1 + 3)

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\",\"headerBottom\",\"accent\"]");
```

LM guitest then moves from `pendingNext=[headerBottom, accent]` → `pendingNext=none`.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.

## Practical pitfalls

See [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Practical pitfalls.
