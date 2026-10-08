package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.DMZDamageSource;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.techniques.KiAttackData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.network.chat.Component;
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

    /** Filled on the first successful lookup. A miss is not cached, so the next attack tries again. */
    private static volatile Method ASSIGNMENT;
    private static volatile Method RANK;
    private static volatile Method HAS_ENERGY;

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
     * the damage-type id, {@code DMZDamageSource.messageId}, and
     * {@code techniqueName}. The handler is static because the target is static.
     * A copied official id in that message is still this name path. A real
     * Hakai projectile is authorized earlier and is left alone when the owner
     * is a Destroyer.
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
            if (source == null || cir == null) {
                return;
            }
            String id = techniqueId(source);
            String name = techniqueName(source);
            if (!namedHakai(id, name)) {
                return;
            }
            ServerPlayer player = attacker(source);
            if (officialHakaiId(id) || officialHakaiId(name)) {
                if (authorizedOfficialProjectile(source) && isDestroyer(player)) {
                    return;
                }
            }
            if (!isDestroyer(player)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
            if (cir != null) {
                cir.setReturnValue(false);
            }
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
            if (cir != null) {
                cir.setReturnValue(false);
            }
        }
    }

    /** Same match as Noea {@code isHakai}: either string contains {@code hakai}. */
    static boolean namedHakai(String id, String name) {
        return containsHakai(id) || containsHakai(name);
    }

    private static boolean containsHakai(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("hakai");
    }

    /**
     * {@code DestroyerRoleService.assignment(player).rank().hasDestructionEnergy()}.
     * The three methods are cached after the first successful lookup. A missing
     * class, a missing method, or any other reflection failure is not a Destroyer,
     * and that failure is not cached.
     */
    static boolean isDestroyer(Player player) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        try {
            Method assignmentMethod = ASSIGNMENT;
            if (assignmentMethod == null) {
                Class<?> service = Class.forName(
                        "com.butterjaffa.noeabosses.DestroyerRoleService",
                        false,
                        HakaiDestroyerGateMixin.class.getClassLoader());
                assignmentMethod = service.getMethod("assignment", ServerPlayer.class);
                ASSIGNMENT = assignmentMethod;
            }
            Object assignment = assignmentMethod.invoke(null, server);
            if (assignment == null) {
                return false;
            }
            Method rankMethod = RANK;
            if (rankMethod == null) {
                rankMethod = assignment.getClass().getMethod("rank");
                RANK = rankMethod;
            }
            Object rank = rankMethod.invoke(assignment);
            if (rank == null) {
                return false;
            }
            Method energyMethod = HAS_ENERGY;
            if (energyMethod == null) {
                energyMethod = rank.getClass().getMethod("hasDestructionEnergy");
                HAS_ENERGY = energyMethod;
            }
            Object energy = energyMethod.invoke(rank);
            return Boolean.TRUE.equals(energy);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Projectile technique id, else the DMZ message id, else the damage message id. */
    static String techniqueId(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity direct = source.m_7640_();
        if (direct instanceof AbstractKiProjectile ki) {
            String id = ki.getTechniqueId();
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }
        String messageId = dmzStringField(source, "messageId");
        if (messageId != null && !messageId.isEmpty()) {
            return messageId;
        }
        return source.m_19385_();
    }

    /** Technique name component, else the damage-type path. */
    static String techniqueName(DamageSource source) {
        if (source == null) {
            return null;
        }
        String technique = dmzTechniqueName(source);
        if (technique != null && !technique.isEmpty()) {
            return technique;
        }
        return damageTypePath(source);
    }

    /** The three shipped Noea ids, plus {@code DivineTechniques.isHakaiTechnique}. */
    static boolean officialHakaiId(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if ("noea_hakai_sphere".equals(value)
                || "noea_hakai_beam".equals(value)
                || "noea_hakai_erasure_grab".equals(value)) {
            return true;
        }
        try {
            Class<?> techniques = Class.forName(
                    "com.butterjaffa.noeabosses.DivineTechniques",
                    false,
                    HakaiDestroyerGateMixin.class.getClassLoader());
            Object result = techniques.getMethod("isHakaiTechnique", String.class).invoke(null, value);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean authorizedOfficialProjectile(DamageSource source) {
        Entity direct = source == null ? null : source.m_7640_();
        if (!(direct instanceof AbstractKiProjectile ki)) {
            return false;
        }
        if (PersistentDataAccess.flag(ki, "NoeaAuthorizedHakaiProjectile")) {
            return true;
        }
        return officialHakaiId(ki.getTechniqueId());
    }

    private static String dmzStringField(DamageSource source, String fieldName) {
        if (!(source instanceof DMZDamageSource)) {
            return null;
        }
        try {
            Field field = DMZDamageSource.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(source);
            return value instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String dmzTechniqueName(DamageSource source) {
        if (!(source instanceof DMZDamageSource)) {
            return null;
        }
        try {
            Field field = DMZDamageSource.class.getDeclaredField("techniqueName");
            field.setAccessible(true);
            Object value = field.get(source);
            if (value instanceof Component component) {
                return component.getString();
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String damageTypePath(DamageSource source) {
        try {
            Object holder = source.m_269150_();
            if (holder == null) {
                return null;
            }
            Object optional = holder.getClass().getMethod("m_203543_").invoke(holder);
            if (!(optional instanceof java.util.Optional<?> key) || key.isEmpty()) {
                return null;
            }
            Object location = key.get().getClass().getMethod("m_135782_").invoke(key.get());
            Object path = location.getClass().getMethod("m_135815_").invoke(location);
            return path instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
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
