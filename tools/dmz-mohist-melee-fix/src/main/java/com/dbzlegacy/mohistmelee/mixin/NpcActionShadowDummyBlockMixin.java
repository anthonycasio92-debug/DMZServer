package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Masters (Mr. Popo actionId 1) used to spawn ShadowDummyEntity clones for sparring.
 * That path is disabled server-side — player minigame summons are untouched.
 */
@Mixin(value = NPCActionC2S.class, remap = false)
public abstract class NpcActionShadowDummyBlockMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String BLOCK_MSG =
            "\u00A7cMaster shadow dummy sparring is disabled on this server.";

    @Inject(method = "handlePopo", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockMasterShadowDummy(
            ServerPlayer player,
            StatsData stats,
            int actionId,
            CallbackInfo ci
    ) {
        if (actionId != 1 || player == null) {
            return;
        }
        ci.cancel();
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            Method literal = component.getMethod("m_237113_", String.class);
            Object msg = literal.invoke(null, BLOCK_MSG);
            Method send = ServerPlayer.class.getMethod("m_5661_", component, boolean.class);
            send.invoke(player, msg, true);
        } catch (Throwable ignored) {
        }
        LOGGER.info(
                "[{}] blocked master shadow dummy spawn for {}",
                DmzMohistMeleeFix.MOD_ID,
                player.m_36316_().getName()
        );
    }
}
