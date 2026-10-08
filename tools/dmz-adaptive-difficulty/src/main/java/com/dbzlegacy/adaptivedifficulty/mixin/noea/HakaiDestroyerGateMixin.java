package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.techniques.KiAttackData;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Noea treats any ki id or name containing {@code hakai} as Hakai and then
 * insta-kills. Official technique ids ({@code noea_hakai_sphere},
 * {@code noea_hakai_beam}, {@code noea_hakai_erasure_grab}) skip that name
 * path and already go through {@code DestroyerRoleService.canDamageWithHakaiProjectile}.
 * This gate leaves those alone and blocks the name match unless the attacker
 * is an apprentice or appointed Destroyer, the same rank check as
 * {@code DestroyerRank.hasDestructionEnergy()}.
 *
 * <p>Destroyer types are resolved by reflection when an attack fires. Importing
 * them here loads those classes while this mixin is transforming
 * {@code DivineImmortalityEvents}, and Noea then fails to register that listener.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.DivineImmortalityEvents", remap = false)
public abstract class HakaiDestroyerGateMixin {

    @Inject(
            method = "onKiAttackFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/butterjaffa/noeabosses/DivineImmortalityEvents;isHakai(Ljava/lang/String;Ljava/lang/String;)Z"
            ),
            cancellable = true,
            require = 0,
            remap = false)
    private static void lm$gateHakaiByDestroyer(DMZEvent.KiAttackFireEvent event, CallbackInfo ci) {
        try {
            if (event == null) {
                return;
            }
            KiAttackData attack = event.getKiAttack();
            if (attack == null || !namedHakai(attack.getId(), attack.getName())) {
                return;
            }
            if (!isDestroyer(event.getPlayer())) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * {@code sourceSaysHakai} calls {@code isHakai} on the damage message,
     * the damage-type id, and the technique name. A true result authorizes
     * the kill. Official projectiles return earlier, inside
     * {@code authorizedHakaiSource}, and never reach this method.
     * The handler is static because the target method is static.
     */
    @Inject(
            method = "sourceSaysHakai",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/butterjaffa/noeabosses/DivineImmortalityEvents;isHakai(Ljava/lang/String;Ljava/lang/String;)Z"
            ),
            cancellable = true,
            require = 0,
            remap = false)
    private static void lm$gateSourceSaysHakai(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!isDestroyer(attacker(source))) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * The damage-type id is checked in {@code lambda$sourceSaysHakai$0},
     * which has no {@code DamageSource}. This return gate covers that call
     * and any other true result from the name path.
     */
    @Inject(method = "sourceSaysHakai", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void lm$gateNamedHakaiSource(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir == null || !Boolean.TRUE.equals(cir.getReturnValue())) {
                return;
            }
            if (!isDestroyer(attacker(source))) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Same match as Noea {@code isHakai}: either string contains {@code hakai}. */
    static boolean namedHakai(String id, String name) {
        return containsHakai(id) || containsHakai(name);
    }

    private static boolean containsHakai(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("hakai");
    }

    static boolean isDestroyer(Player player) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        try {
            Class<?> service = Class.forName(
                    "com.butterjaffa.noeabosses.DestroyerRoleService",
                    false,
                    HakaiDestroyerGateMixin.class.getClassLoader());
            Object assignment = service.getMethod("assignment", ServerPlayer.class).invoke(null, server);
            if (assignment == null) {
                return false;
            }
            Object rank = assignment.getClass().getMethod("rank").invoke(assignment);
            if (rank == null) {
                return false;
            }
            Object energy = rank.getClass().getMethod("hasDestructionEnergy").invoke(rank);
            return Boolean.TRUE.equals(energy);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ServerPlayer attacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        ServerPlayer causing = asServer(source.m_7639_());
        if (causing != null) {
            return causing;
        }
        Entity direct = source.m_7640_();
        ServerPlayer directPlayer = asServer(direct);
        if (directPlayer != null) {
            return directPlayer;
        }
        if (direct instanceof Projectile projectile) {
            return asServer(projectile.m_19749_());
        }
        return null;
    }

    private static ServerPlayer asServer(Entity entity) {
        return entity instanceof ServerPlayer server ? server : null;
    }
}
