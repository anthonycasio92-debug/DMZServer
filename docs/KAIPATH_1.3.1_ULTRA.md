# KaiPath Ultra + LegacyMechanics

| Side | Role |
|------|------|
| LM **4.6.122** | `GuiOpenPacket` (11 fields: +`inputsJson`), styles, `titleAlign`, `playerModel.parts`, Config + Weights text fields |
| KaiPath **1.3.7** (released / vendored / live) | `style`, `activeTab`, `titleAlign`, `playerModel`, GuiFeaturesPacket (4 features) |
| Still missing on 1.3.7 (JLDK owns) | Field 11 / `textInput` — merge `tools/ultra-gui-kit/kaipath-client/` into next official KaiPath |

Note: CurseForge never published **1.3.5**; versions jumped **1.3.1 → 1.3.7**.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
