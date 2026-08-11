package com.dbzlegacy.adaptivedifficulty.scaling;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Field;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Vanilla clamps {@code generic.max_health} to 1024, {@code generic.armor} to 30,
 * and {@code generic.attack_damage} to 2048. Raise those ceilings so adaptive
 * difficulty can scale without a hard attribute wall.
 */
public final class AttributeLimits {
    /** High enough for multi-million difficulty curves; still finite for attribute math. */
    private static final double UNCAP_MAX = 1.0e9;

    private AttributeLimits() {}

    public static void uncapOffenseAttributes() {
        raiseMax(Attributes.f_22276_, UNCAP_MAX); // MAX_HEALTH (vanilla 1024)
        raiseMax(Attributes.f_22284_, UNCAP_MAX); // ARMOR
        raiseMax(Attributes.f_22281_, UNCAP_MAX); // ATTACK_DAMAGE
    }

    private static void raiseMax(Attribute attribute, double newMax) {
        if (!(attribute instanceof RangedAttribute ranged)) {
            return;
        }
        try {
            Field maxField = RangedAttribute.class.getDeclaredField("f_22308_"); // maxValue
            maxField.setAccessible(true);
            double current = maxField.getDouble(ranged);
            if (current >= newMax) {
                return;
            }
            maxField.setDouble(ranged, newMax);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] raised {} attribute max {} → {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    attribute.m_22087_(), // getDescriptionId
                    current,
                    newMax
            );
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] failed to raise attribute max for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    attribute,
                    t.toString()
            );
        }
    }
}
