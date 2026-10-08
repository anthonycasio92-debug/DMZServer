package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.DestroyerRank;
import com.butterjaffa.noeabosses.DestroyerRoleSavedData;
import com.butterjaffa.noeabosses.DestroyerRoleService;
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
            require = 0)
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
     * Damage whose message or technique name merely contains {@code hakai}
     * also force-kills. Official projectiles never reach this method.
     */
    @Inject(method = "sourceSaysHakai", at = @At("RETURN"), cancellable = true, require = 0)
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
            DestroyerRoleSavedData.Assignment assignment = DestroyerRoleService.assignment(server);
            if (assignment == null) {
                return false;
            }
            DestroyerRank rank = assignment.rank();
            return rank != null && rank.hasDestructionEnergy();
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
