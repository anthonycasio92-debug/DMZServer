package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerPlayer;

/** Item id and inventory helpers for the native hot path. */
public final class ItemIds {
    private ItemIds() {}

    public static String id(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return "";
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return key == null ? "" : key.toString();
    }

    public static Item item(String id) {
        if (id == null) {
            return null;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon >= id.length() - 1) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id.substring(0, colon), id.substring(colon + 1)));
    }

    public static ItemStack stack(String id, int count) {
        Item item = item(id);
        if (item == null) {
            return ItemStack.f_41583_;
        }
        int n = count < 1 ? 1 : count;
        ItemStack stack = new ItemStack((ItemLike) item, n);
        return stack.m_41619_() ? ItemStack.f_41583_ : stack;
    }

    public interface SlotEdit {
        /** @return true when the slot contents changed */
        boolean edit(Container container, int slot, ItemStack stack);
    }

    public static int editPlayer(ServerPlayer player, SlotEdit edit) {
        if (player == null || edit == null) {
            return 0;
        }
        int changed = 0;
        changed += editContainer(player.m_150109_(), edit);
        changed += editContainer(player.m_36327_(), edit);
        return changed;
    }

    public static int editContainer(Container container, SlotEdit edit) {
        if (container == null || edit == null) {
            return 0;
        }
        int changed = 0;
        int size = container.m_6643_();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = container.m_8020_(slot);
            if (stack == null || stack.m_41619_()) {
                continue;
            }
            try {
                if (edit.edit(container, slot, stack)) {
                    changed++;
                }
            } catch (Throwable ignored) {
            }
        }
        return changed;
    }
}
