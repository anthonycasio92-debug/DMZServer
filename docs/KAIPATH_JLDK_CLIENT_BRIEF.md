# JLDK brief — KaiPath Ultra GUI client conformance

**Status (KaiPath 1.3.9 + multi-mod protocol):** Live KaiPath **1.3.9** honors `style`,
`activeTab`, `titleAlign`, `playerModel`, and **`textInput`** on every registered
`{modId}:gui` channel. Partner servers (LM **4.6.152** and any other mod using the kit)
already emit **`headerBottom`** and **`accent`** — those are the open advertise-able bridge
gaps (plus title vertical centering, which needs no new field).

**Multi-mod rule:** codecs and the dynamic screen are shared. Register every partner mod id
via [`UltraGuiPartners`](../tools/ultra-gui-kit/kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/UltraGuiPartners.java)
— do not hardcode a single `legacymechanics` consumer.

Full gap matrix: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
| Bridge gap matrix | [`docs/KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |
| DMZUltra asset reuse (bridge-only) | [`docs/DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md) |
| KaiPath client sources (reference) | [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) |
| Server codecs (**any** Forge mod) | [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) |
| Non-LM chrome fixture | [`BridgeFutureFieldsFixture.json`](../tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json) |

---

## Governing rule

The client renders what the server sends — **no hardcoded screen/tab/button/action lists**.
Each server mod owns `{modId}:gui`; clicks return on the channel that opened the screen.

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

## Open gaps (merge kit — then done)

Reference implementation is ready in
[`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/).
Merge into the next official KaiPath jar; live **1.3.9** still lacks these.

### 1. `layoutJson.headerBottom` — tabs under the yellow divider

LM places tabs as regular buttons at Y=`headerBottom` with `tabsUnderTitle=true`.
**Keep title text + yellow divider strictly above `headerBottom`.** Defaults: **28** with
tabs, **42** without. Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) §4.

Advertise: `"headerBottom"`. **Done in kit** (`LmDynamicScreen`).

### 2. Title vertical centering — JLDK-only

Center the title text in the header band between the gear icons. No protocol field.
**Done in kit** (title centered in `[0, headerBottom)`).

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

Advertise: `"accent"`. **Done in kit**.

## Advertise (kit default — every partner channel)

```java
// Boot: register all partners (not only LM)
UltraGuiPartners.registerDefaults();
// Or: UltraGuiPartners.register("legacymechanics", "myshop");

// Registry advertises the same JSON on each channel automatically:
// ["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent",
//  "palette","sound","buttonSkin","frame","pagedBook","combined.forms.v1"]
```

After merge, partner guitests (e.g. LM `/lm admin guitest`) move from
`pendingNext=[headerBottom, accent, palette]` → `pendingNext=none`.

## Also merge: ASCII `fitLabel` (weird A/E glyphs)

Stock `UltraWidgetReflect.fitLabel` uses a mojibaked ellipsis. Replace with ASCII `...` and
never prefix labels with section-sign color codes. Details:
[`FITLABEL_ASCII.md`](../tools/ultra-gui-kit/kaipath-client/com/dmzlegacy/kaipath/ultra/internal/FITLABEL_ASCII.md).

## Chrome honor (in kit — texture blit still JLDK)

Kit already honors protocol § Proposed chrome + [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md):
`palette` (ARGB map), `sound` (reflection play), `buttonSkin`/`frame`/`portraitFrame`
(chrome rects until texture blit), `paged_book` (Prev/Next). Verify against
[`BridgeFutureFieldsFixture.json`](../tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json)
(`myshop:gui` — not a production partner). Map namespaced ids → DMZUltra textures in the
official KaiPath jar.

## Deferred (not blocking)

- `inputsJson` types `slider` / `toggle` — reserved in [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).
- Full DMZUltra geo application of `playerModel.parts` beyond the entity preview.

## Practical pitfalls

See [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) § Practical pitfalls.
