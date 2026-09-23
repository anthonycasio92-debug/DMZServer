package com.dbzlegacy.adaptivedifficulty.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Mohist/Bukkit-forwarded commands sometimes drop {@link CommandSourceStack} feedback — mirror to the player. */
public final class LmCommandFeedback {
    private LmCommandFeedback() {}

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        if (source == null) {
            return null;
        }
        ServerPlayer player = source.m_230896_();
        if (player != null) {
            return player;
        }
        try {
            Entity entity = source.m_81375_();
            return entity instanceof ServerPlayer sp ? sp : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void tellLines(CommandSourceStack source, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        ServerPlayer player = playerOrNull(source);
        if (player != null) {
            for (String line : raw.split("\n")) {
                if (line != null && !line.isEmpty()) {
                    player.m_213846_(Component.m_237113_(line));
                }
            }
            return;
        }
        if (source != null) {
            source.m_81352_(Component.m_237113_(raw.replace('\n', ' ')));
        }
    }

    public static void tell(CommandSourceStack source, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        ServerPlayer player = playerOrNull(source);
        if (player != null) {
            player.m_213846_(Component.m_237113_(line));
            return;
        }
        if (source != null) {
            source.m_81352_(Component.m_237113_(line));
        }
    }
}
