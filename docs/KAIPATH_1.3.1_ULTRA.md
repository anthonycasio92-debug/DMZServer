# KaiPath Ultra + LegacyMechanics

| Side | Role |
|------|------|
| LM **4.6.151** | `GuiOpenPacket` (11 fields), styles, `titleAlign`, `playerModel.parts`, text fields, **`headerBottom`**, **`accent`** |
| KaiPath **1.3.9** (live / recommended) | `style`, `activeTab`, `titleAlign`, `playerModel`, `textInput` (5 GuiFeatures) |
| **Pending on KaiPath** | Merge kit: honor + advertise `headerBottom`, `accent`; title vertical centering (no field) |
| **Kit (handoff)** | [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/) implements all three — merge into next official jar |

Note: CurseForge jumped **1.3.1 → 1.3.7 → 1.3.9**. Field 11 / `textInput` landed in **1.3.9**.

Guitest on 1.3.9: `missingShipped=none pendingNext=[headerBottom, accent]`.
After merging the kit: `pendingNext=none`.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
