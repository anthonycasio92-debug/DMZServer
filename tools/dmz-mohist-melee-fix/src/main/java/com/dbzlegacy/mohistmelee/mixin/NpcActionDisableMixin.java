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
 * Server-side disables for selected DMZ master NPC actions (DragonMineZ 2.1.3 jar):
 * <ul>
 *   <li>Master shadow spar: every master UI sends {@code NPCActionC2S("popo", 1)} →
 *       {@code isAnyMasterInRange} → {@code handlePopo(1)}. Player minigame summons untouched.</li>
 *   <li>Guru potential unlock: {@code NPCActionC2S("guru", 1)} → {@code handleGuru(1)} →
 *       {@code Skills.addSkillLevel("potentialunlock", 1)}. Other skill sources untouched.</li>
 *   <li>Dr. Gero android conversion: {@code NPCActionC2S("gero", 1)} → {@code handleGero(1)} →
 *       {@code Status.setAndroidUpgraded(true)} + androidforms. Also blocks CNPC/script reflection
 *       calls into {@code handleGero}.</li>
 * </ul>
 */
@Mixin(value = NPCActionC2S.class, remap = false)
public abstract class NpcActionDisableMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String MSG_SHADOW =
            "\u00A7cMaster shadow dummy sparring is disabled on this server.";
    private static final String MSG_GURU_POTENTIAL =
            "\u00A7cGuru potential unlock is disabled on this server.";
    private static final String MSG_GERO_ANDROID =
            "\u00A7cDr. Gero android conversion is disabled on this server.";

    @Shadow
    @Final
    private String npcName;

    @Shadow
    @Final
    private int actionId;

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockDisabledNpcActions(
            NPCActionC2S packet,
            ServerPlayer player,
            StatsData stats,
            CallbackInfo ci
    ) {
        if (packet == null) {
            return;
        }
        NpcActionDisableMixin self = (NpcActionDisableMixin) (Object) packet;
        if (self.actionId != 1 || self.npcName == null) {
            return;
        }
        switch (self.npcName) {
            case "popo" -> deny(player, ci, MSG_SHADOW, "shadow-protocol");
            case "guru" -> deny(player, ci, MSG_GURU_POTENTIAL, "guru-potential-protocol");
            case "gero" -> deny(player, ci, MSG_GERO_ANDROID, "gero-android-protocol");
            default -> {
            }
        }
    }

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
        deny(player, ci, MSG_SHADOW, "handlePopo");
    }

    @Inject(method = "handleGuru", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockGuruPotentialUnlock(
            ServerPlayer player,
            StatsData stats,
            int actionId,
            CallbackInfo ci
    ) {
        if (actionId != 1) {
            return;
        }
        deny(player, ci, MSG_GURU_POTENTIAL, "handleGuru");
    }

    @Inject(method = "handleGero", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockGeroAndroidConversion(
            ServerPlayer player,
            StatsData stats,
            int actionId,
            CallbackInfo ci
    ) {
        if (actionId != 1) {
            return;
        }
        deny(player, ci, MSG_GERO_ANDROID, "handleGero");
    }

    private static void deny(ServerPlayer player, CallbackInfo ci, String message, String where) {
        ci.cancel();
        if (player == null) {
            return;
        }
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            Method literal = component.getMethod("m_237113_", String.class);
            Object msg = literal.invoke(null, message);
            Method send = ServerPlayer.class.getMethod("m_5661_", component, boolean.class);
            send.invoke(player, msg, true);
        } catch (Throwable ignored) {
        }
        LOGGER.info(
                "[{}] blocked NPC action ({}) for {}",
                DmzMohistMeleeFix.MOD_ID,
                where,
                player.m_36316_().getName()
        );
    }
}
