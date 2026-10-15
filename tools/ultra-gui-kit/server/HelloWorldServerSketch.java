package com.example.ultragui;

/**
 * Sketch only — not compiled by this repo. Minimal server loop after you copy
 * {@link UltraGuiOpenPacket} / {@link UltraGuiActionPacket} / {@link UltraGuiFeaturesPacket} /
 * {@link UltraGuiChannel} into <b>your</b> Forge mod (any {@code modId} — zero coupling to
 * any production partner).
 *
 * <p>JSON twin: {@code BridgeFutureFieldsFixture.json}. Buttons use the shape
 * LegacyMechanics actually sends: {@code id}, {@code label}, {@code style}, {@code x},
 * {@code y}, {@code w}, {@code h}, {@code pad}, optional {@code footer}, {@code args}.
 * Panel {@code palette} and {@code frame} stay on {@code layoutJson}. Do not put a
 * palette, click sound, or skin on a button, and do not set a portrait frame on
 * {@code playerModel} (a non-empty id paints a border around the preview).
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
 * // Open — plain ASCII. Button objects are style + geometry + args only.
 * String buttons = "["
 *     + "{\"id\":\"@myshop:do:ping\",\"label\":\"Ping\",\"style\":\"confirm\","
 *     + "\"x\":20,\"y\":40,\"w\":150,\"h\":18,\"pad\":8,"
 *     + "\"args\":{\"screen\":\"hello\",\"page\":\"main\"}},"
 *     + "{\"id\":\"@myshop:nav:hub\",\"label\":\"Main menu\",\"style\":\"dim\","
 *     + "\"x\":174,\"y\":272,\"w\":150,\"h\":18,\"pad\":8,\"footer\":true,"
 *     + "\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
 *     + "]";
 * String inputs = "["
 *     + "{\"id\":\"@myshop:book:guide\",\"type\":\"paged_book\",\"x\":8,\"y\":70,"
 *     + "\"w\":220,\"h\":120,\"page\":0,\"pages\":["
 *     + "{\"title\":\"Welcome\",\"lines\":[\"Page one.\"]},"
 *     + "{\"title\":\"Tips\",\"lines\":[\"Page two.\"]}]}"
 *     + "]";
 * String layout = "{"
 *     + "\"center\":true,\"margin\":10,\"titleAlign\":\"center\","
 *     + "\"headerBottom\":42,\"palette\":\"gold\",\"frame\":\"legacymechanics:panel\","
 *     + "\"playerModel\":{\"x\":248,\"y\":60,\"width\":86,\"height\":120,"
 *     + "\"scale\":1.0,\"rotate\":true,"
 *     + "\"parts\":{\"schema\":1,\"subject\":\"local\",\"bodyParts\":["
 *     + "{\"id\":\"shop_tail\",\"source\":\"myshop\",\"slot\":\"tail\"}]}}"
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
