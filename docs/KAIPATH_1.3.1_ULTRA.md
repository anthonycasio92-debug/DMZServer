# KaiPath Ultra + LegacyMechanics

| Side | Role |
|------|------|
| LM **4.6.121** | `GuiOpenPacket` (11 fields: +`inputsJson`), styles, `titleAlign`, `playerModel.parts`, Config + Weights text fields |
| KaiPath **1.3.7** (released / vendored) | `style`, `activeTab`, `titleAlign`, `playerModel`, GuiFeaturesPacket (4 features) |
| Still missing on 1.3.7 | Field 11 `inputsJson` / `textInput` advertise — merge from `tools/ultra-gui-kit/kaipath-client/` |

Note: CurseForge never published **1.3.5**; versions jumped **1.3.1 → 1.3.7**.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
