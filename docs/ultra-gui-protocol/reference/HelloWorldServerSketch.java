package com.example.ultragui;

/**
 * Sketch only — not compiled by this repo. Shows the minimal server loop after you copy
 * {@link UltraGuiOpenPacket} / {@link UltraGuiActionPacket} / {@link UltraGuiChannel} into your mod.
 *
 * <pre>
 * // Registration (Forge):
 * SimpleChannel ch = NetworkRegistry.newSimpleChannel(
 *     new ResourceLocation(MOD_ID, UltraGuiChannel.PATH),
 *     () -> UltraGuiChannel.PROTOCOL_VERSION,
 *     NetworkRegistry.acceptMissingOr(UltraGuiChannel.PROTOCOL_VERSION),
 *     NetworkRegistry.acceptMissingOr(UltraGuiChannel.PROTOCOL_VERSION));
 * ch.messageBuilder(UltraGuiOpenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
 *   .encoder(UltraGuiOpenPacket::encode).decoder(UltraGuiOpenPacket::decode)
 *   .consumerMainThread((m, ctx) -> ctx.get().setPacketHandled(true)).add();
 * ch.messageBuilder(UltraGuiActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
 *   .encoder(UltraGuiActionPacket::encode).decoder(UltraGuiActionPacket::decode)
 *   .consumerNetworkThread((m, ctx) -> {
 *       ServerPlayer p = ctx.get().getSender();
 *       ctx.get().enqueueWork(() -> onClick(p, m));
 *       ctx.get().setPacketHandled(true);
 *   }).add();
 *
 * // Open:
 * String buttons = "[{\"id\":\"@myshop:do:ping\",\"label\":\"§aPing\","
 *     + "\"x\":20,\"y\":80,\"w\":150,\"h\":18,\"args\":{\"screen\":\"hello\"}}]";
 * UltraGuiOpenPacket open = new UltraGuiOpenPacket(
 *     "hello", "§6My Shop", "main", "[]", "", "[\"Welcome.\"]", buttons,
 *     350, 220, "{\"center\":true,\"margin\":6}");
 * ch.send(PacketDistributor.PLAYER.with(() -> player), open);
 *
 * // Click:
 * void onClick(ServerPlayer p, UltraGuiActionPacket m) {
 *     if ("@myshop:do:ping".equals(m.actionId())) {
 *         p.sendSystemMessage(Component.literal("Pong"));
 *     }
 * }
 * </pre>
 */
public final class HelloWorldServerSketch {
    private HelloWorldServerSketch() {}
}
