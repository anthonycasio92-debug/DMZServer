# KaiPath Ultra + LegacyMechanics

| Side | Role |
|------|------|
| LM **4.6.155** | `GuiOpenPacket` (11 fields), styles, `titleAlign`, `playerModel.parts`, text fields, **`headerBottom`**, **`accent`**, button **`footer:true`**, chrome emit (sound/skin/frame/`paged_book`) |
| KaiPath **1.3.17** (recommended) | Prior chrome + **`fixedFooter`** (pins `footer:true`) + advertise **`portraitFrame`** |
| **Pending on KaiPath** | Title vertical centering confirm. Kit adds optional `icon`, partner jar discovery, and documents generic `combined.forms.v1`. |

Note: CurseForge / live jumped **1.3.1 → 1.3.7 → 1.3.9 → 1.3.13 → 1.3.17**. Field 11 / `textInput` landed in **1.3.9**. Chrome (`headerBottom` / `accent` / palette…) by **1.3.13**. `fixedFooter` + `portraitFrame` advertise in **1.3.17**.

Guitest on stock 1.3.17: `missingShipped=none pendingNext=icon`.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
