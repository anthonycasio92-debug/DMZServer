package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Deletes blueprint and overpowered capsules. Recipes are removed by RecipeStrip. */
public final class CapsuleBan {
    public static final int PLAYER_PURGE_INTERVAL = 100;
    private static Method isBlueprint;
    private static Method isOverpowered;
    private static boolean looked;

    private CapsuleBan() {}

    public static void purge(ServerPlayer player) {
        if (player == null || !HotPathConfig.get().capsules.enabled) {
            return;
        }
        int removed = ItemIds.editPlayer(player, (container, slot, stack) -> {
            if (!isBanned(stack)) {
                return false;
            }
            ItemStack again = container.m_8020_(slot);
            if (!isBanned(again)) {
                return false;
            }
            container.m_6836_(slot, ItemStack.f_41583_);
            return true;
        });
        if (removed > 0) {
            HotLog.tell(
                    player,
                    net.minecraft.ChatFormatting.RED,
                    "Blueprint / Overpowered capsules are disabled and were removed.");
            HotLog.debug("[Capsule Disable] Removed " + removed + " banned capsule stack(s)");
        }
    }

    public static boolean onUse(PlayerInteractEvent.RightClickItem event, ServerPlayer player) {
        if (!HotPathConfig.get().capsules.enabled) {
            return false;
        }
        ItemStack stack = event.getItemStack();
        if (!isBanned(stack)) {
            return false;
        }
        event.setCanceled(true);
        stack.m_41764_(0);
        HotLog.tell(
                player,
                net.minecraft.ChatFormatting.RED,
                "Blueprint / Overpowered capsules are disabled on this server.");
        return true;
    }

    public static boolean onItemSpawn(EntityJoinLevelEvent event, ItemEntity entity, ItemStack stack) {
        if (!HotPathConfig.get().capsules.enabled) {
            return false;
        }
        if (!isBanned(stack)) {
            return false;
        }
        event.setCanceled(true);
        try {
            entity.m_146870_();
        } catch (Throwable ignored) {
        }
        return true;
    }

    static boolean isBanned(ItemStack stack) {
        if (!"capsule:capsule".equals(ItemIds.id(stack))) {
            return false;
        }
        ensureApi();
        if (invoke(isBlueprint, stack) || invoke(isOverpowered, stack)) {
            return true;
        }
        CompoundTag tag = stack.m_41783_();
        if (isOverpoweredNbt(tag)) {
            return true;
        }
        return tag != null && tag.m_128441_("sourceInventory");
    }

    private static boolean isOverpoweredNbt(CompoundTag tag) {
        if (tag == null || !tag.m_128441_("overpowered")) {
            return false;
        }
        try {
            if (tag.m_128445_("overpowered") == 1
                    || tag.m_128448_("overpowered") == 1
                    || tag.m_128451_("overpowered") == 1) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String raw = tag.toString();
        return raw.contains("overpowered:1b")
                || raw.contains("overpowered:1,")
                || raw.contains("overpowered:1}")
                || raw.contains("overpowered:1 ");
    }

    private static void ensureApi() {
        if (looked) {
            return;
        }
        looked = true;
        try {
            Class<?> type = Class.forName("capsule.items.CapsuleItem");
            isBlueprint = type.getMethod("isBlueprint", ItemStack.class);
            isOverpowered = type.getMethod("isOverpowered", ItemStack.class);
        } catch (Throwable t) {
            isBlueprint = null;
            isOverpowered = null;
            HotLog.debug("[Capsule Disable] CapsuleItem class missing: " + t);
        }
    }

    private static boolean invoke(Method method, ItemStack stack) {
        if (method == null) {
            return false;
        }
        try {
            Object value = method.invoke(null, stack);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
