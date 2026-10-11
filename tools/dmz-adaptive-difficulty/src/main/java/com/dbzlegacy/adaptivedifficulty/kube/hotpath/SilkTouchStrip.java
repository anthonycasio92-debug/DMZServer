package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Strips silk touch and turns a now-empty enchanted book into a normal book. */
public final class SilkTouchStrip {
    public static final int PLAYER_SCAN_INTERVAL = 100;
    private static Enchantment silk;
    private static boolean looked;

    private SilkTouchStrip() {}

    public static void purge(ServerPlayer player) {
        if (player == null || !HotPathConfig.get().silkTouch.enabled) {
            return;
        }
        int changed = ItemIds.editPlayer(player, (container, slot, stack) -> {
            if (!mentionsSilk(stack)) {
                return false;
            }
            String before = ItemIds.id(stack);
            boolean stripped = strip(stack);
            if ("minecraft:enchanted_book".equals(before) && enchantmentsEmpty(stack)) {
                ItemStack book = ItemIds.stack("minecraft:book", stack.m_41613_());
                if (!book.m_41619_()) {
                    container.m_6836_(slot, book);
                    return true;
                }
            }
            if (stripped) {
                container.m_6836_(slot, stack);
                return true;
            }
            return false;
        });
        if (changed > 0) {
            HotLog.tell(player, "\u00A77Silk Touch was removed from " + changed
                    + " item(s). The items were kept.");
            HotLog.debug("[Silk Touch Remove] stripped " + changed + " stack(s)");
        }
    }

    public static void onItemSpawn(ItemEntity entity, ItemStack stack) {
        if (!HotPathConfig.get().silkTouch.enabled) {
            return;
        }
        if (entity == null || stack == null || stack.m_41619_() || !mentionsSilk(stack)) {
            return;
        }
        String before = ItemIds.id(stack);
        strip(stack);
        if ("minecraft:enchanted_book".equals(before) && enchantmentsEmpty(stack)) {
            ItemStack book = ItemIds.stack("minecraft:book", stack.m_41613_());
            if (!book.m_41619_()) {
                entity.m_32045_(book);
            }
        }
    }

    static boolean mentionsSilk(ItemStack stack) {
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        String raw = tag.toString();
        return raw.contains("silk_touch") || raw.contains("minecraft:silk_touch");
    }

    private static boolean strip(ItemStack stack) {
        boolean changed = false;
        try {
            Map<Enchantment, Integer> map = EnchantmentHelper.m_44831_(stack);
            if (map != null && removeSilk(map)) {
                EnchantmentHelper.m_44865_(map, stack);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        CompoundTag tag = stack.m_41783_();
        if (tag != null) {
            changed = filterList(tag, "Enchantments") || changed;
            changed = filterList(tag, "StoredEnchantments") || changed;
        }
        return changed;
    }

    private static boolean removeSilk(Map<Enchantment, Integer> map) {
        boolean changed = false;
        Enchantment silkEnchant = silk();
        if (silkEnchant != null && map.containsKey(silkEnchant)) {
            map.remove(silkEnchant);
            changed = true;
        }
        for (Enchantment enchant : new ArrayList<>(map.keySet())) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
            if (id != null && "minecraft:silk_touch".equals(id.toString())) {
                map.remove(enchant);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean filterList(CompoundTag tag, String key) {
        if (tag == null || !tag.m_128441_(key)) {
            return false;
        }
        ListTag list = tag.m_128437_(key, 10);
        if (list == null) {
            return false;
        }
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag compound = list.m_128728_(i);
            String id = compound.m_128461_("id");
            if (id.contains("silk_touch")) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed && list.size() <= 0) {
            tag.m_128473_(key);
        }
        return removed;
    }

    private static boolean enchantmentsEmpty(ItemStack stack) {
        try {
            Map<Enchantment, Integer> map = EnchantmentHelper.m_44831_(stack);
            return map == null || map.isEmpty();
        } catch (Throwable t) {
            CompoundTag tag = stack.m_41783_();
            if (tag == null) {
                return true;
            }
            return !tag.m_128441_("Enchantments") && !tag.m_128441_("StoredEnchantments");
        }
    }

    private static Enchantment silk() {
        if (looked) {
            return silk;
        }
        looked = true;
        silk = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft", "silk_touch"));
        return silk;
    }
}
