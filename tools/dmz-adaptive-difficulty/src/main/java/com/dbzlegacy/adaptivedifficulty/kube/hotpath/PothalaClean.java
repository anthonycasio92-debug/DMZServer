package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.lang.reflect.Method;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

/** Strips enchantments and Apotheosis affixes from DMZ Pothala earrings. Pair NBT stays. */
public final class PothalaClean {
    public static final int CHECK_INTERVAL = 20;
    private static final Set<String> IDS = Set.of(
            "dragonminez:pothala_left",
            "dragonminez:pothala_right",
            "dragonminez:pothala_pair",
            "dragonminez:green_pothala_left",
            "dragonminez:green_pothala_right",
            "dragonminez:green_pothala_pair");
    private static final String[] STRIP = {
        "Enchantments",
        "StoredEnchantments",
        "affix_data",
        "apoth_boss",
        "apoth_rspawn",
        "apoth_rchest",
        "apothic_rarity"
    };

    private static boolean curiosLooked;
    private static Method getCuriosInventory;
    private static Method getEquippedCurios;
    private static Method getSlots;
    private static Method getStackInSlot;
    private static Method setStackInSlot;

    private PothalaClean() {}

    public static void clean(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int inventory = ItemIds.editPlayer(player, (container, slot, stack) -> {
            if (!cleanStack(stack)) {
                return false;
            }
            container.m_6836_(slot, stack);
            return true;
        });
        int curios = cleanCurios(player);
        if (inventory > 0 || curios > 0) {
            try {
                player.m_150109_().m_6596_();
            } catch (Throwable ignored) {
            }
            try {
                player.f_36095_.m_38946_();
            } catch (Throwable ignored) {
            }
            try {
                if (player.f_36096_ != null) {
                    player.f_36096_.m_38946_();
                }
            } catch (Throwable ignored) {
            }
            HotLog.debug("[DMZ Pothala Protection] cleaned inventory=" + inventory + " curios=" + curios);
        }
    }

    static boolean cleanStack(ItemStack stack) {
        if (!IDS.contains(ItemIds.id(stack))) {
            return false;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        boolean changed = false;
        for (String key : STRIP) {
            if (tag.m_128441_(key)) {
                tag.m_128473_(key);
                changed = true;
            }
        }
        return changed;
    }

    private static int cleanCurios(ServerPlayer player) {
        ensureCurios();
        if (getCuriosInventory == null) {
            return 0;
        }
        try {
            Object raw = getCuriosInventory.invoke(null, player);
            if (!(raw instanceof LazyOptional<?> optional)) {
                return 0;
            }
            int[] cleaned = {0};
            optional.ifPresent(handler -> cleaned[0] = scanCurios(handler));
            return cleaned[0];
        } catch (Throwable t) {
            HotLog.debug("[DMZ Pothala Protection] Curios API scan failed: " + t);
            return 0;
        }
    }

    private static int scanCurios(Object handler) {
        try {
            if (getEquippedCurios == null) {
                getEquippedCurios = handler.getClass().getMethod("getEquippedCurios");
            }
            Object curios = getEquippedCurios.invoke(handler);
            if (curios == null) {
                return 0;
            }
            if (getSlots == null) {
                getSlots = curios.getClass().getMethod("getSlots");
                getStackInSlot = curios.getClass().getMethod("getStackInSlot", int.class);
                setStackInSlot = curios.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            }
            int slots = ((Number) getSlots.invoke(curios)).intValue();
            int cleaned = 0;
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) getStackInSlot.invoke(curios, i);
                if (stack == null || stack.m_41619_()) {
                    continue;
                }
                if (cleanStack(stack)) {
                    setStackInSlot.invoke(curios, i, stack);
                    cleaned++;
                }
            }
            return cleaned;
        } catch (Throwable t) {
            HotLog.debug("[DMZ Pothala Protection] Curios handler scan failed: " + t);
            return 0;
        }
    }

    private static void ensureCurios() {
        if (curiosLooked) {
            return;
        }
        curiosLooked = true;
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            for (Method method : api.getMethods()) {
                if ("getCuriosInventory".equals(method.getName()) && method.getParameterCount() == 1) {
                    getCuriosInventory = method;
                    break;
                }
            }
            if (getCuriosInventory == null) {
                return;
            }
        } catch (Throwable t) {
            getCuriosInventory = null;
            HotLog.debug("[DMZ Pothala Protection] Curios API unavailable: " + t);
        }
    }
}
