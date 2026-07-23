package com.dbzlegacy.mohistmelee.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * DragonMineZ applies melee inside {@code CombatAttackRequestC2S} via
 * {@code ServerPlayer.attack} ({@code m_5706_}). On Mohist that path (and often
 * {@code LivingEntity.hurt}) routes through a flaky Bukkit EntityDamage bridge
 * until the player entity is recreated by death.
 *
 * For living targets we skip {@code attack}/{@code hurt} and call Forge's
 * patched {@code actuallyHurt} ({@code m_6475_}) directly. On Forge 1.20.1 that
 * method fires {@code LivingHurtEvent} once (so DMZ {@code CombatEvent} can
 * rewrite to {@code getMeleeDamage()}) without entering Bukkit's damage bridge.
 *
 * Do <b>not</b> pre-fire {@code ForgeHooks.onLivingHurt} — {@code actuallyHurt}
 * already does, and a second fire trips DMZ's 35ms duplicate-hit cancel.
 */
@Mixin(targets = "com.dragonminez.common.network.C2S.CombatAttackRequestC2S", remap = false)
public abstract class CombatAttackRequestC2SMixin {
    private static final Logger LOGGER = LogManager.getLogger("dmz_mohist_melee_fix");
    private static final AtomicBoolean LOGGED_FIRST_HIT = new AtomicBoolean(false);

    @Redirect(
            method = "lambda$processAttackRequest$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;m_5706_(Lnet/minecraft/world/entity/Entity;)V",
                    remap = true
            ),
            remap = false,
            require = 1
    )
    private static void dbzlegacy$useHurtInsteadOfAttack(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            player.m_5706_(target);
            return;
        }

        // Clear i-frames so DMZ combo hits can land.
        living.f_19802_ = 0;
        living.f_20916_ = 0;

        DamageSource source = player.m_269291_().m_269075_(player);

        // LivingAttackEvent only (not LivingHurt — actuallyHurt fires that once).
        if (!ForgeHooks.onLivingAttack(living, source, 1.0F)) {
            return;
        }

        float before = living.m_21223_();
        ((LivingEntityInvoker) living).dbzlegacy$invokeActuallyHurt(source, 1.0F);
        float after = living.m_21223_();

        if (LOGGED_FIRST_HIT.compareAndSet(false, true)) {
            LOGGER.info(
                    "[dmz_mohist_melee_fix] Melee redirect active: actuallyHurt Bukkit-bypass delta={}",
                    before - after
            );
        }
    }
}
