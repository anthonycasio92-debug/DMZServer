package com.dbzlegacy.adaptivedifficulty.progression.util;

import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * Soft reflection bridge to AttributesLib / Apothic Attributes.
 * Returns 0 when the jar is absent — never hard-depends at compile time.
 */
public final class ApothicAttributes {
    private static final String[] ATTR_HOLDERS = {
            "dev.shadowsoffire.attributeslib.api.ALObjects$Attributes",
            "dev.shadowsoffire.apothic_attributes.api.ALObjects$Attributes"
    };

    private static volatile boolean resolved;
    private static Attribute fireDamage;
    private static Attribute coldDamage;
    private static Attribute protPierce;
    private static Attribute armorPierce;
    private static Attribute armorShred;
    private static Attribute experienceGained;

    private ApothicAttributes() {}

    public static boolean available() {
        ensure();
        return fireDamage != null || coldDamage != null || protPierce != null
                || armorPierce != null || armorShred != null || experienceGained != null;
    }

    public static double fireDamage(LivingEntity entity) {
        return value(entity, fire());
    }

    public static double coldDamage(LivingEntity entity) {
        return value(entity, cold());
    }

    public static double protPierce(LivingEntity entity) {
        return value(entity, pierce());
    }

    public static double armorPierce(LivingEntity entity) {
        return value(entity, armorP());
    }

    public static double armorShred(LivingEntity entity) {
        return value(entity, shred());
    }

    public static double experienceGained(LivingEntity entity) {
        return value(entity, xp());
    }

    private static Attribute fire() {
        ensure();
        return fireDamage;
    }

    private static Attribute cold() {
        ensure();
        return coldDamage;
    }

    private static Attribute pierce() {
        ensure();
        return protPierce;
    }

    private static Attribute armorP() {
        ensure();
        return armorPierce;
    }

    private static Attribute shred() {
        ensure();
        return armorShred;
    }

    private static Attribute xp() {
        ensure();
        return experienceGained;
    }

    private static double value(LivingEntity entity, Attribute attr) {
        if (entity == null || attr == null) {
            return 0.0;
        }
        try {
            AttributeInstance inst = entity.m_21051_(attr); // getAttribute
            if (inst == null) {
                return 0.0;
            }
            double v = inst.m_22115_(); // getValue
            return Double.isFinite(v) ? v : 0.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    private static void ensure() {
        if (resolved) {
            return;
        }
        synchronized (ApothicAttributes.class) {
            if (resolved) {
                return;
            }
            for (String holder : ATTR_HOLDERS) {
                try {
                    Class<?> cls = Class.forName(holder);
                    fireDamage = resolve(cls, "FIRE_DAMAGE");
                    coldDamage = resolve(cls, "COLD_DAMAGE");
                    protPierce = resolve(cls, "PROT_PIERCE");
                    armorPierce = resolve(cls, "ARMOR_PIERCE");
                    armorShred = resolve(cls, "ARMOR_SHRED");
                    experienceGained = resolve(cls, "EXPERIENCE_GAINED");
                    if (fireDamage != null || coldDamage != null || protPierce != null) {
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            resolved = true;
        }
    }

    private static Attribute resolve(Class<?> holder, String field) {
        try {
            Object holderField = holder.getField(field).get(null);
            if (holderField == null) {
                return null;
            }
            // RegistryObject / Holder / Supplier style
            Method get = null;
            try {
                get = holderField.getClass().getMethod("get");
            } catch (NoSuchMethodException ignored) {
            }
            Object attr = get != null ? get.invoke(holderField) : holderField;
            return attr instanceof Attribute a ? a : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
