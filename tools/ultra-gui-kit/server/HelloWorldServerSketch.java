package com.example.ultragui;

/**
 * Sketch only — not compiled by this repo. Minimal server loop after you copy
 * {@link UltraGuiOpenPacket} / {@link UltraGuiActionPacket} / {@link UltraGuiFeaturesPacket} /
 * {@link UltraGuiChannel} into <b>your</b> Forge mod (any mod id — not LegacyMechanics-specific).
 *
 * <pre>
 * // Registration (Forge) — channel = YOUR_MOD_ID:gui
 * SimpleChannel ch = NetworkRegistry.newSimpleChannel(
 *     new ResourceLocation(MOD_ID, UltraGuiChannel.PATH),
 *     () -> UltraGuiChannel.PROTOCOL_VERSION,
 *     remote -> true,
 *     remote -> true);
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
 * ch.messageBuilder(UltraGuiFeaturesPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
 *   .encoder(UltraGuiFeaturesPacket::encode).decoder(UltraGuiFeaturesPacket::decode)
 *   .consumerMainThread((m, ctx) -> {
 *       // Optional: remember client features for this player
 *       ctx.get().setPacketHandled(true);
 *   }).add();
 *
 * // Tell KaiPath to listen (client side, once per partner mod):
 * // UltraGuiPartners.register("mymod");  // or add "mymod" to DEFAULT_PARTNERS
 *
 * // Open — plain ASCII labels; tint via accent / style (avoid section-sign in title/buttons)
 * String buttons = "["
 *     + "{\"id\":\"@mymod:do:ping\",\"label\":\"Ping\",\"x\":20,\"y\":40,"
 *     + "\"w\":150,\"h\":18,\"style\":\"confirm\",\"accent\":\"a\","
 *     + "\"args\":{\"screen\":\"hello\"}}"
 *     + "]";
 * String layout = "{\"center\":true,\"margin\":6,\"titleAlign\":\"center\","
 *     + "\"headerBottom\":42,\"accent\":\"6\"}";
 * UltraGuiOpenPacket open = new UltraGuiOpenPacket(
 *     "hello", "My Shop", "main", "[]", "", "[\"Welcome.\"]", buttons,
 *     350, 220, layout, "[]");
 * ch.send(PacketDistributor.PLAYER.with(() -> player), open);
 *
 * // Click — actionId / argsJson echoed verbatim from the button
 * void onClick(ServerPlayer p, UltraGuiActionPacket m) {
 *     if ("@mymod:do:ping".equals(m.actionId())) {
 *         p.sendSystemMessage(Component.literal("Pong"));
 *     }
 * }
 * </pre>
 */
public final class HelloWorldServerSketch {
    private HelloWorldServerSketch() {}
}
