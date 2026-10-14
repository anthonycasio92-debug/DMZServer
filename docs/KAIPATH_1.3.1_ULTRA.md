# KaiPath Ultra + LegacyMechanics

| Side | Role |
|------|------|
| LM **4.6.154** | `GuiOpenPacket` (11 fields), styles, `titleAlign`, `playerModel.parts`, text fields, **`headerBottom`**, **`accent`**, button **`footer:true`**, chrome emit (sound/skin/frame/`paged_book`) |
| KaiPath **1.3.13** (live / recommended) | `style`, `activeTab`, `titleAlign`, `playerModel`, `textInput`, `headerBottom`, `accent`, palette/sound/buttonSkin/frame/pagedBook |
| **Pending on KaiPath** | Merge kit `fixedFooter` + advertise `portraitFrame`/`fixedFooter`; title vertical centering (no field) |

Note: CurseForge / live jumped **1.3.1 → 1.3.7 → 1.3.9 → 1.3.13**. Field 11 / `textInput` landed in **1.3.9**. Chrome (`headerBottom` / `accent` / palette…) landed by **1.3.13**.

Guitest on 1.3.13: `missingShipped=none pendingNext=[fixedFooter]`.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
