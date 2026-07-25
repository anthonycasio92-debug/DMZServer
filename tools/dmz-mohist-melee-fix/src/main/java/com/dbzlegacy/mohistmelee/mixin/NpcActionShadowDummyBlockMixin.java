package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Jar fact (DragonMineZ 2.1.3):
 * <ul>
 *   <li>Only {@code NPCActionC2S} + {@code SummonPlayerShadowDummyC2S} create ShadowDummyEntity</li>
 *   <li>Master spar from <b>any</b> master/quest-NPC training UI sends
 *       {@code new NPCActionC2S("popo", 1)} (MasterTextScreen + QuestNPCDialogueScreen)</li>
 *   <li>Server treats that as shadow-spar: {@code isAnyMasterInRange()} then {@code handlePopo(actionId=1)}</li>
 *   <li>Player minigame uses {@code SummonPlayerShadowDummyC2S} (left alone)</li>
 * </ul>
 */
@Mixin(value = NPCActionC2S.class, remap = false)
public abstract class NpcActionShadowDummyBlockMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String BLOCK_MSG =
            "\u00A7cMaster shadow dummy sparring is disabled on this server.";

    @Shadow
    @Final
    private String npcName;

    @Shadow
    @Final
    private int actionId;

    /** Protocol entry used by every master UI that offers shadow spar. */
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockMasterShadowSparProtocol(
            NPCActionC2S packet,
            ServerPlayer player,
            StatsData stats,
            CallbackInfo ci
    ) {
        if (packet == null || !isMasterShadowSparPacket(packet)) {
            return;
        }
        deny(player, ci, "protocol");
    }

    /** Spawn body — backup if the protocol inject misses a remap/name change. */
    @Inject(method = "handlePopo", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockMasterShadowDummySpawn(
            ServerPlayer player,
            StatsData stats,
            int actionId,
            CallbackInfo ci
    ) {
        if (actionId != 1) {
            return;
        }
        deny(player, ci, "handlePopo");
    }

    private static boolean isMasterShadowSparPacket(NPCActionC2S packet) {
        NpcActionShadowDummyBlockMixin self = (NpcActionShadowDummyBlockMixin) (Object) packet;
        return self.actionId == 1 && "popo".equals(self.npcName);
    }

    private static void deny(ServerPlayer player, CallbackInfo ci, String where) {
        ci.cancel();
        if (player == null) {
            return;
        }
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            Method literal = component.getMethod("m_237113_", String.class);
            Object msg = literal.invoke(null, BLOCK_MSG);
            Method send = ServerPlayer.class.getMethod("m_5661_", component, boolean.class);
            send.invoke(player, msg, true);
        } catch (Throwable ignored) {
        }
        LOGGER.info(
                "[{}] blocked master shadow dummy ({}) for {}",
                DmzMohistMeleeFix.MOD_ID,
                where,
                player.m_36316_().getName()
        );
    }
}
