# KaiPath 1.3.x + LegacyMechanics 4.6.107

Live clients run **kaipath-1.3.x** with the dynamic Ultra consumer. LM **4.6.107** pairs
with that client:

| Side | Role |
|------|------|
| LM | Builds `GuiOpenPacket` (350×300 default, grow to **420**, `titleAlign=center`, styles, `activeTab`, optional `playerModel`) |
| KaiPath | Renders the packet; must advertise optional features when implemented |

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md) for the JLDK gap list
(`style`, `activeTab`, `titleAlign`, `playerModel`).
