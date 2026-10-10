package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.noea.AbsorptionWipeHelper;
import com.dbzlegacy.adaptivedifficulty.noea.GodKiWipeHelper;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dbzlegacy.adaptivedifficulty.progression.PrestigeResourceRecovery;
import com.dbzlegacy.adaptivedifficulty.progression.StatsDataLoadContext;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.stats.StatsData;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Default-priority StatsData hooks that share one target: load guarding,
 * owner resolution, secondary-attribute player lookup, resource clamp after
 * restore, and the prestige/absorption wipe after a reset.
 * Higher-priority caps stay in their own mixins so their return values still win.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataGuardsMixin {
    @Shadow(remap = false)
    private Player player;

    private static final Map<StatsData, UUID> OWNER_UUID =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<StatsData, Long> MISS_UNTIL =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final long NEGATIVE_TTL_NANOS = 500_000_000L;
    /** Stops the cancelled head call from re-entering the load guard. */
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * A RETURN inject does not run when {@code load} throws, which left the
     * load flag stuck on that thread. Cancel the outer call and run the real
     * load inside try/finally.
     */
    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$loadGuarded(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) throws ClassNotFoundException {
        if (REENTRY.get()) {
            return;
        }
        ci.cancel();
        REENTRY.set(Boolean.TRUE);
        try {
            StatsDataLoadContext.enter();
            try {
                ((StatsData) (Object) this).load(tag);
            } finally {
                StatsDataLoadContext.exit();
            }
        } finally {
            REENTRY.set(Boolean.FALSE);
        }
    }

    @Inject(method = "getPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$resolveOwner(CallbackInfoReturnable<Player> cir) {
        if (StatsDataLoadContext.inLoad()) {
            return;
        }
        Player existing = cir.getReturnValue();
        StatsData self = (StatsData) (Object) this;
        if (existing instanceof ServerPlayer sp && sp.m_6084_()) {
            OWNER_UUID.put(self, sp.m_20148_());
            MISS_UNTIL.remove(self);
            return;
        }
        if (existing != null && existing.m_6084_()) {
            MISS_UNTIL.remove(self);
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        UUID cached = OWNER_UUID.get(self);
        if (cached != null) {
            ServerPlayer sp = server.m_6846_().m_11259_(cached);
            if (sp != null && sp.m_6084_()) {
                MISS_UNTIL.remove(self);
                cir.setReturnValue(sp);
                return;
            }
        }
        long now = System.nanoTime();
        Long missUntil = MISS_UNTIL.get(self);
        if (missUntil != null && now < missUntil) {
            return;
        }
        for (ServerPlayer sp : server.m_6846_().m_11314_()) {
            if (sp == null || !sp.m_6084_()) {
                continue;
            }
            try {
                if (DmzProgression.stats(sp) == self) {
                    OWNER_UUID.put(self, sp.m_20148_());
                    MISS_UNTIL.remove(self);
                    cir.setReturnValue(sp);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        MISS_UNTIL.put(self, now + NEGATIVE_TTL_NANOS);
    }

    @Redirect(
            method = "getSecondaryAttributeValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$secondaryAttrPlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    @Redirect(
            method = "getSecondaryAttributeBaseValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$secondaryBasePlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    @Redirect(
            method = "getArmorToughnessValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$armorToughnessPlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    private static Player lm$playerForSecondary(StatsData instance) {
        if (instance == null) {
            return null;
        }
        Player resolved = instance.getPlayer();
        if (resolved != null && resolved.m_6084_()) {
            return resolved;
        }
        return ((StatsDataGuardsMixin) (Object) instance).player;
    }

    @Inject(method = "restoreMultiplierGains", at = @At("RETURN"), remap = false)
    private void lm$clampAfterRestore(ServerPlayer player, float[] snapshot, CallbackInfo ci) {
        if (StatsDataLoadContext.inLoad() || !LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return;
        }
        try {
            DmzResourcePoolClamp.clamp((StatsData) (Object) this);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ki/stamina clamp skipped after restoreMultiplierGains: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    @Inject(method = "resetPlayerProgress", at = @At("RETURN"), remap = false)
    private void lm$syncOverhaulPrestigeAfterReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        AbsorptionWipeHelper.wipe(player, null);
        GodKiWipeHelper.wipe(player, null);
        try {
            PrestigeResourceRecovery.afterDmzStatsReset(player);
        } catch (Throwable ignored) {
        }
        try {
            DmzRevampPrestigeBridge.scheduleSyncAfterStatsReset(player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Character creation applies the race at the start of this method.
     * A different race drops the stored absorption bonus first.
     */
    @Inject(method = "initializeWithRaceAndClass", at = @At("HEAD"), remap = false, require = 0)
    private void lm$wipeAbsorptionBeforeRaceInit(
            String race,
            String characterClass,
            String gender,
            int hairId,
            CustomHair customHair,
            int bodyType,
            int eyesType,
            int noseType,
            int mouthType,
            int tattooType,
            float boobScale,
            String activeHeadBone,
            String hairColor,
            String bodyColor,
            String bodyColor2,
            String bodyColor3,
            String eye1Color,
            String eye2Color,
            String auraColor,
            CallbackInfo ci
    ) {
        AbsorptionWipeHelper.wipeIfRaceChanges((StatsData) (Object) this, race);
        GodKiWipeHelper.wipeIfRaceChanges((StatsData) (Object) this, race);
    }
}
