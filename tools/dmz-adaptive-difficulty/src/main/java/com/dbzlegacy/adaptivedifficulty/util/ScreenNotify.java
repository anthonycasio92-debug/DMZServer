package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Screen title/subtitle feedback for blocked / not-allowed actions.
 * Prefer this over chat for deny messages players need to notice in-world.
 */
public final class ScreenNotify {
    private static final String DEFAULT_CD_KEY = "lm.screen.notify";
    private static final long DEFAULT_COOLDOWN_MS = 10_000L;

    private ScreenNotify() {}

    /** Title + subtitle with default anti-spam cooldown. No chat. */
    public static void blocked(ServerPlayer player, String title, String subtitle) {
        blocked(player, title, subtitle, DEFAULT_CD_KEY, DEFAULT_COOLDOWN_MS);
    }

    /**
     * @param cooldownKey per-player temp key (empty = no cooldown)
     * @param cooldownMs  min ms between shows for this key
     */
    public static void blocked(
            ServerPlayer player, String title, String subtitle, String cooldownKey, long cooldownMs
    ) {
        if (player == null || title == null || title.isBlank()) {
            return;
        }
        try {
            if (cooldownKey != null && !cooldownKey.isBlank() && cooldownMs > 0L) {
                long now = System.currentTimeMillis();
                long last = ProgressionData.tempGetLong(player, cooldownKey, 0L);
                if (last > 0L && now - last < cooldownMs) {
                    return;
                }
                ProgressionData.tempPut(player, cooldownKey, now);
            }
            if (player.f_8906_ == null) {
                return;
            }
            player.f_8906_.m_9829_(new ClientboundClearTitlesPacket(false));
            player.f_8906_.m_9829_(new ClientboundSetTitlesAnimationPacket(5, 45, 10));
            player.f_8906_.m_9829_(new ClientboundSetTitleTextPacket(styled(title, ChatFormatting.RED)));
            if (subtitle != null && !subtitle.isBlank()) {
                player.f_8906_.m_9829_(
                        new ClientboundSetSubtitleTextPacket(styled(subtitle, ChatFormatting.GRAY)));
            }
        } catch (Throwable ignored) {
        }
    }

    /** Strip legacy § codes and apply a solid formatting (titles render § poorly on some clients). */
    private static Component styled(String raw, ChatFormatting color) {
        String plain = raw == null ? "" : raw.replaceAll("§.", "").trim();
        return Component.m_237113_(plain).m_130940_(color);
    }
}
