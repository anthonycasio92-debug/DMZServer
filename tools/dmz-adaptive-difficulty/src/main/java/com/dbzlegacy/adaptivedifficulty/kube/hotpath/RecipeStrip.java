package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Removes the recipes the old KubeJS scripts deleted, and leaves the
 * LegacyMechanics redstone-control spawner recipes in place.
 */
public final class RecipeStrip {
    private static final Set<String> EXACT_IDS = Set.of(
            "capsule:blueprint",
            "capsule:blueprint_change",
            "capsule:aggregate_all_prefabs",
            "capsule:capsule_op",
            "tconstruct:tools/modifiers/ability/silky",
            "tconstruct:tools/modifiers/salvage/ability/silky",
            "tconstruct:tools/modifiers/silky_cloth",
            "tconstruct:smeltery/melting/metal/rose_gold/silky_cloth");
    private static final Set<String> CAPSULE_TYPES = Set.of(
            "capsule:blueprint_capsule",
            "capsule:blueprint_change",
            "capsule:aggregate_all_prefabs");

    private RecipeStrip() {}

    public static void strip() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        RecipeManager manager = server.m_129894_();
        if (manager == null) {
            return;
        }
        Collection<Recipe<?>> all = manager.m_44051_();
        if (all == null || all.size() < 50) {
            return;
        }
        RegistryAccess access = server.m_206579_();
        ArrayList<Recipe<?>> kept = new ArrayList<>(all.size());
        int removed = 0;
        for (Recipe<?> recipe : all) {
            if (recipe != null && drop(recipe, access)) {
                removed++;
            } else if (recipe != null) {
                kept.add(recipe);
            }
        }
        if (removed <= 0) {
            return;
        }
        manager.m_44024_(kept);
        HotLog.debug("[LM] removed " + removed + " hot-path recipes");
    }

    private static boolean drop(Recipe<?> recipe, RegistryAccess access) {
        ResourceLocation id = recipe.m_6423_();
        String key = id == null ? "" : id.toString();
        String lower = key.toLowerCase();
        if (lower.contains("heartstop") || EXACT_IDS.contains(key)) {
            return true;
        }
        String type = typeId(recipe);
        if (CAPSULE_TYPES.contains(type)) {
            return true;
        }
        if ("apotheosis:spawner_modifier".equals(type) && key.startsWith("apotheosis:")) {
            return true;
        }
        if (lower.contains("silky_cloth") || lower.contains("/silky")) {
            return true;
        }
        return "tconstruct:silky_cloth".equals(outputId(recipe, access));
    }

    private static String typeId(Recipe<?> recipe) {
        try {
            ResourceLocation type = ForgeRegistries.RECIPE_TYPES.getKey(recipe.m_6671_());
            return type == null ? "" : type.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static String outputId(Recipe<?> recipe, RegistryAccess access) {
        try {
            ItemStack stack = recipe.m_8043_(access);
            if (stack == null || stack.m_41619_()) {
                return "";
            }
            ResourceLocation item = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            return item == null ? "" : item.toString();
        } catch (Throwable t) {
            return "";
        }
    }
}
