package com.dbzlegacy.mohistmelee.mixin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * DragonMineZ melee calls {@code ServerPlayer.attack}. On Mohist, {@code attack},
 * {@code hurt}, and even Forge's {@code actuallyHurt} can still be tied into a
 * flaky Bukkit EntityDamage bridge until the player entity is recreated by death.
 *
 * This redirect:
 * <ol>
 *   <li>Fires Forge {@link LivingHurtEvent} once so DMZ {@code CombatEvent} rewrites
 *       damage to {@code getMeleeDamage()}</li>
 *   <li>Applies the resulting amount with {@code setHealth} + death — never calling
 *       {@code attack}/{@code hurt}/{@code actuallyHurt}</li>
 * </ol>
 */
@Mixin(targets = "com.dragonminez.common.network.C2S.CombatAttackRequestC2S", remap = false)
public abstract class CombatAttackRequestC2SMixin {
    private static final Logger LOGGER = LogManager.getLogger("dmz_mohist_melee_fix");
    private static final AtomicInteger GLOBAL_LOGS = new AtomicInteger();
    private static final Map<UUID, Integer> PLAYER_LOGS = new ConcurrentHashMap<>();

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

        living.f_19802_ = 0;
        living.f_20916_ = 0;

        DamageSource source = player.m_269291_().m_269075_(player);

        if (!ForgeHooks.onLivingAttack(living, source, 1.0F)) {
            logHit(player, living, 0.0F, "livingAttackCancelled");
            return;
        }

        // Single LivingHurtEvent — DMZ CombatEvent rewrites amount here.
        LivingHurtEvent hurtEvent = new LivingHurtEvent(living, source, 1.0F);
        if (MinecraftForge.EVENT_BUS.post(hurtEvent)) {
            logHit(player, living, 0.0F, "livingHurtCancelled");
            return;
        }
        float amount = hurtEvent.getAmount();
        if (amount <= 0.0F) {
            logHit(player, living, 0.0F, "livingHurtZero");
            return;
        }

        LivingDamageEvent damageEvent = new LivingDamageEvent(living, source, amount);
        if (MinecraftForge.EVENT_BUS.post(damageEvent)) {
            logHit(player, living, 0.0F, "livingDamageCancelled");
            return;
        }
        amount = damageEvent.getAmount();
        if (amount <= 0.0F) {
            logHit(player, living, 0.0F, "livingDamageZero");
            return;
        }

        float before = living.m_21223_();
        float next = Math.max(0.0F, before - amount);
        living.m_21153_(next);
        living.f_20917_ = 10;
        living.f_20916_ = 10;
        living.f_19802_ = 20;

        if (next <= 0.0F && living.m_6084_()) {
            living.m_6667_(source);
        }

        logHit(player, living, before - living.m_21223_(), "applied amount=" + amount);
    }

    private static void logHit(ServerPlayer player, LivingEntity living, float delta, String detail) {
        int global = GLOBAL_LOGS.incrementAndGet();
        int perPlayer = PLAYER_LOGS.merge(player.m_20148_(), 1, Integer::sum);
        if (global > 20 && perPlayer > 3) {
            return;
        }
        LOGGER.info(
                "[dmz_mohist_melee_fix] hit player={} target={} delta={} {}",
                player.m_36316_().getName(),
                living.m_6095_().m_20675_(),
                delta,
                detail
        );
    }
}
