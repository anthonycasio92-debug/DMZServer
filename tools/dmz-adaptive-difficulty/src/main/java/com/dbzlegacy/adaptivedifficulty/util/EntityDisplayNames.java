package com.dbzlegacy.adaptivedifficulty.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Human-readable entity type labels for nameplates.
 * Avoids raw translation keys like {@code entity.minecraft.zombie}.
 */
public final class EntityDisplayNames {
    private EntityDisplayNames() {}

    public static String of(LivingEntity entity) {
        if (entity == null) {
            return "Mob";
        }
        return of(entity.m_6095_());
    }

    public static String of(EntityType<?> type) {
        if (type == null) {
            return "Mob";
        }
        try {
            Component desc = type.m_20676_(); // getDescription()
            if (desc != null) {
                String text = desc.getString();
                if (text != null) {
                    text = text.trim();
                    if (!text.isEmpty() && !looksLikeTranslationKey(text)) {
                        return text;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null && id.m_135815_() != null && !id.m_135815_().isEmpty()) {
            return prettyPath(id.m_135815_()); // getPath()
        }
        try {
            String shortName = type.m_147048_(); // toShortString()
            if (shortName != null && !shortName.isBlank()) {
                return prettyPath(shortName.contains(":")
                        ? shortName.substring(shortName.indexOf(':') + 1)
                        : shortName);
            }
        } catch (Throwable ignored) {
        }
        return "Mob";
    }

    private static boolean looksLikeTranslationKey(String text) {
        return text.startsWith("entity.") || text.indexOf('.') >= 0 && text.equals(text.toLowerCase());
    }

    private static String prettyPath(String path) {
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder(path.length() + 4);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.length() == 0 ? "Mob" : sb.toString();
    }
}
