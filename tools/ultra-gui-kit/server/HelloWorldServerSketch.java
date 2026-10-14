package com.example.ultragui;

/**
 * Sketch only — not compiled by this repo. Minimal server loop after you copy
 * {@link UltraGuiOpenPacket} / {@link UltraGuiActionPacket} / {@link UltraGuiFeaturesPacket} /
 * {@link UltraGuiChannel} into <b>your</b> Forge mod (any {@code modId} — zero coupling to
 * any production partner).
 *
 * <p>JSON twin that exercises every proposed chrome field:
 * {@code BridgeFutureFieldsFixture.json}.
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
 *   .consumerMainThread((m, ctx) -> ctx.get().setPacketHandled(true)).add();
 *
 * // KaiPath client: UltraGuiPartners.register("myshop");
 *
 * // Open — plain ASCII; palette/sound/skin/frame/paged_book are mod-agnostic wire fields
 * String buttons = "["
 *     + "{\"id\":\"@myshop:do:ping\",\"label\":\"Ping\",\"style\":\"confirm\","
 *     + "\"palette\":\"gold\",\"sound\":\"dmzultra:ui_confirm\","
 *     + "\"buttonSkin\":\"dmzultra:arena\",\"x\":20,\"y\":40,\"w\":150,\"h\":18,"
 *     + "\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
 *     + "]";
 * String inputs = "["
 *     + "{\"id\":\"@myshop:book:guide\",\"type\":\"paged_book\",\"x\":8,\"y\":70,"
 *     + "\"w\":220,\"h\":120,\"sound\":\"dmzultra:scene_tick\",\"pages\":["
 *     + "{\"title\":\"Welcome\",\"lines\":[\"Page one.\"]},"
 *     + "{\"title\":\"Tips\",\"lines\":[\"Page two.\"]}]}"
 *     + "]";
 * String layout = "{"
 *     + "\"center\":true,\"margin\":10,\"titleAlign\":\"center\","
 *     + "\"headerBottom\":42,\"palette\":\"gold\",\"frame\":\"dmzultra:card\","
 *     + "\"playerModel\":{\"x\":248,\"y\":60,\"width\":86,\"height\":120,"
 *     + "\"scale\":1.0,\"rotate\":true,\"portraitFrame\":\"dmzultra:portrait\"}"
 *     + "}";
 * UltraGuiOpenPacket open = new UltraGuiOpenPacket(
 *     "hello", "My Shop", "main", "[]", "", "[\"Welcome.\"]",
 *     buttons, 350, 300, layout, inputs);
 * ch.send(PacketDistributor.PLAYER.with(() -> player), open);
 *
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
