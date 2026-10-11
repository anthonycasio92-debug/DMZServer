package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Silky cloth and silky modifier crystals become cobwebs. Silky is stripped from tools.
 * Recipes are removed by RecipeStrip. The empty modifier json ships in this mod.
 */
public final class SilkyBan {
    public static final int CLOTH_SCAN_INTERVAL = 20;
    public static final int TOOL_SCAN_INTERVAL = 100;
    private static final ResourceLocation SILKY = new ResourceLocation("tconstruct", "silky");

    private static boolean looked;
    private static Method isInitialized;
    private static Method from;
    private static Method getUpgrades;
    private static Method getLevel;
    private static Method removeModifier;
    private static Method updateStack;
    private static Method updateStackForce;
    private static Method crystalModifier;

    private SilkyBan() {}

    public static void purge(ServerPlayer player, boolean tools) {
        if (player == null) {
            return;
        }
        int changed = ItemIds.editPlayer(player, (container, slot, stack) -> editSlot(container, slot, stack, tools));
        if (changed > 0) {
            HotLog.tell(player, "\u00A77Silky Cloth removed (" + changed
                    + "). Replaced with cobwebs; tools were kept.");
            HotLog.debug("[Tinkers Silky] purged " + changed + " stack(s)");
        }
    }

    public static boolean onUse(PlayerInteractEvent.RightClickItem event, ServerPlayer player) {
        ItemStack stack = event.getItemStack();
        if (stack == null || !"tconstruct:silky_cloth".equals(ItemIds.id(stack))) {
            return false;
        }
        event.setCanceled(true);
        ItemStack replacement = ItemIds.stack("minecraft:cobweb", stack.m_41613_());
        if (event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND) {
            player.m_8061_(net.minecraft.world.entity.EquipmentSlot.OFFHAND, replacement);
        } else {
            player.m_8061_(net.minecraft.world.entity.EquipmentSlot.MAINHAND, replacement);
        }
        HotLog.tell(player, "\u00A77Silky Cloth is disabled and was replaced with cobwebs.");
        return true;
    }

    public static void onItemSpawn(ItemEntity entity, ItemStack stack) {
        if (entity == null || stack == null || stack.m_41619_()) {
            return;
        }
        ItemStack replacement = bannedReplacement(stack);
        if (!replacement.m_41619_()) {
            entity.m_32045_(replacement);
            HotLog.debug("[Tinkers Silky] ground silky item converted to cobweb");
            return;
        }
        String id = ItemIds.id(stack);
        if (mentionsSilky(stack) || id.startsWith("tconstruct:")) {
            stripTool(stack);
        }
    }

    private static boolean editSlot(
            net.minecraft.world.Container container, int slot, ItemStack stack, boolean tools) {
        ItemStack replacement = bannedReplacement(stack);
        if (!replacement.m_41619_()) {
            container.m_6836_(slot, replacement);
            return true;
        }
        if (!tools) {
            return false;
        }
        String id = ItemIds.id(stack);
        if (!mentionsSilky(stack) && !id.startsWith("tconstruct:")) {
            return false;
        }
        if (stripTool(stack)) {
            container.m_6836_(slot, stack);
            return true;
        }
        return false;
    }

    static ItemStack bannedReplacement(ItemStack stack) {
        String id = ItemIds.id(stack);
        if ("tconstruct:silky_cloth".equals(id) || isSilkyCrystal(stack, id)) {
            return ItemIds.stack("minecraft:cobweb", stack.m_41613_());
        }
        return ItemStack.f_41583_;
    }

    private static boolean isSilkyCrystal(ItemStack stack, String id) {
        if (!"tconstruct:modifier_crystal".equals(id)) {
            return false;
        }
        ensureApi();
        if (crystalModifier != null) {
            try {
                Object modifier = crystalModifier.invoke(null, stack);
                if (modifier != null && modifier.toString().toLowerCase().contains("silky")) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return mentionsSilky(stack);
    }

    static boolean mentionsSilky(ItemStack stack) {
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        String lower = tag.toString().toLowerCase();
        return lower.contains("tconstruct:silky")
                || lower.contains("silky_cloth")
                || lower.contains("\"name\":\"tconstruct:silky\"");
    }

    private static boolean stripTool(ItemStack stack) {
        if (stripViaApi(stack)) {
            return true;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        boolean changed = filterModifiers(tag, "tic_upgrades");
        return filterModifiers(tag, "tic_modifiers") || changed;
    }

    private static boolean stripViaApi(ItemStack stack) {
        ensureApi();
        if (isInitialized == null || from == null) {
            return false;
        }
        try {
            Object ready = isInitialized.invoke(null, stack);
            if (!(ready instanceof Boolean) || !((Boolean) ready)) {
                return false;
            }
            Object tool = from.invoke(null, stack);
            if (tool == null || getUpgrades == null || getLevel == null || removeModifier == null) {
                return false;
            }
            Object upgrades = getUpgrades.invoke(tool);
            Object level = getLevel.invoke(upgrades, SILKY);
            int ranks = level instanceof Number ? ((Number) level).intValue() : 0;
            if (ranks <= 0) {
                return false;
            }
            removeModifier.invoke(tool, SILKY, ranks);
            if (updateStack != null) {
                try {
                    updateStack.invoke(tool, stack);
                } catch (Throwable updateFailed) {
                    if (updateStackForce != null) {
                        updateStackForce.invoke(tool, stack, true);
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean filterModifiers(CompoundTag tag, String key) {
        if (!tag.m_128441_(key)) {
            return false;
        }
        ListTag list = tag.m_128437_(key, 10);
        if (list == null) {
            return false;
        }
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            String name = list.m_128728_(i).m_128461_("name").toLowerCase();
            if (name.contains("silky")) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed && list.size() <= 0) {
            tag.m_128473_(key);
        }
        return removed;
    }

    private static void ensureApi() {
        if (looked) {
            return;
        }
        looked = true;
        try {
            Class<?> toolStack = Class.forName("slimeknights.tconstruct.library.tools.nbt.ToolStack");
            isInitialized = toolStack.getMethod("isInitialized", ItemStack.class);
            from = toolStack.getMethod("from", ItemStack.class);
            getUpgrades = toolStack.getMethod("getUpgrades");
            Class<?> upgrades = getUpgrades.getReturnType();
            getLevel = upgrades.getMethod("getLevel", ResourceLocation.class);
            removeModifier = toolStack.getMethod("removeModifier", ResourceLocation.class, int.class);
            try {
                updateStack = toolStack.getMethod("updateStack", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                updateStack = null;
            }
            try {
                updateStackForce = toolStack.getMethod("updateStack", ItemStack.class, boolean.class);
            } catch (NoSuchMethodException ignored) {
                updateStackForce = null;
            }
        } catch (Throwable t) {
            isInitialized = null;
            HotLog.debug("[Tinkers Silky] ToolStack API unavailable: " + t);
        }
        try {
            Class<?> crystal = Class.forName("slimeknights.tconstruct.tools.item.ModifierCrystalItem");
            crystalModifier = crystal.getMethod("getModifier", ItemStack.class);
        } catch (Throwable ignored) {
            crystalModifier = null;
        }
    }
}
