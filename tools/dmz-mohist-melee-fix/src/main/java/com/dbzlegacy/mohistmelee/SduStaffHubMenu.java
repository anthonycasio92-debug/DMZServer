package com.dbzlegacy.mohistmelee;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist-visible SDU hub. SDU 3.0.11 {@code OpenHubPacket} encodes 0 bytes and
 * never opens {@code SduHubScreen} on this client stack. A vanilla chest always
 * renders; slot clicks send the fat {@code DmzNet.open*} editor packets.
 */
public final class SduStaffHubMenu extends ChestMenu {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final Component TITLE = Component.m_237113_("SDU Editor");
    private static final String[] SLOT_EDITORS = new String[27];
    private static final String[][] ICONS = {
            {"10", "race", "minecraft:player_head", "§eRace Editor", "Open the SDU race list"},
            {"11", "form", "minecraft:nether_star", "§dForm Editor", "Open the SDU form list"},
            {"12", "saga", "minecraft:writable_book", "§bSaga Editor", "Open the SDU saga list"},
            {"13", "sidequest", "minecraft:book", "§aSidequest Editor", "Open the SDU sidequest list"},
            {"14", "wish", "minecraft:ender_pearl", "§5Wish Editor", "Open the SDU wish list"},
            {"15", "shrine", "minecraft:ender_chest", "§6Shrine Config", "Open shrine settings"},
            {"16", "options", "minecraft:comparator", "§7Options", "Open SDU options"}
    };

    static {
        for (String[] icon : ICONS) {
            SLOT_EDITORS[Integer.parseInt(icon[0])] = icon[1];
        }
    }

    private final ServerPlayer owner;
    private boolean handlingClick;

    private SduStaffHubMenu(int id, Inventory playerInv, SimpleContainer container, ServerPlayer owner) {
        super(MenuType.f_39959_, id, playerInv, container, 3);
        this.owner = owner;
    }

    public static boolean open(ServerPlayer player) {
        if (player == null || player.m_9236_() == null) {
            return false;
        }
        SimpleContainer container = new SimpleContainer(27);
        fill(container);
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, ignored) -> new SduStaffHubMenu(id, inv, container, player),
                TITLE
        );
        return player.m_5893_(provider).isPresent();
    }

    private static void fill(SimpleContainer container) {
        ItemStack banner = namedStack("minecraft:name_tag", "§6SDU Editor");
        container.m_6836_(4, banner);
        for (String[] icon : ICONS) {
            int slot = Integer.parseInt(icon[0]);
            container.m_6836_(slot, namedStack(icon[2], icon[3]));
        }
    }

    private static ItemStack namedStack(String itemId, String title) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) {
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:paper"));
        }
        ItemStack stack = item == null ? ItemStack.f_41583_ : new ItemStack(item);
        if (!stack.m_41619_()) {
            stack.m_41714_(Component.m_237113_(title));
        }
        return stack;
    }

    @Override
    public void m_150399_(int slotId, int button, ClickType clickType, Player clicker) {
        if (handlingClick) {
            return;
        }
        handlingClick = true;
        try {
            if (slotId >= 0 && slotId < 27 && clicker instanceof ServerPlayer player) {
                String which = SLOT_EDITORS[slotId];
                if (which != null && player.m_20148_().equals(owner.m_20148_())) {
                    scheduleEditor(player, which);
                }
                m_150444_();
                return;
            }
            if (slotId >= 0 && slotId < 27) {
                m_150444_();
                return;
            }
            super.m_150399_(slotId, button, clickType, clicker);
        } finally {
            handlingClick = false;
        }
    }

    @Override
    public ItemStack m_7648_(Player player, int index) {
        return ItemStack.f_41583_;
    }

    private static void scheduleEditor(ServerPlayer player, String which) {
        MinecraftServer server = player.m_20194_();
        Runnable open = () -> {
            try {
                player.m_6915_();
            } catch (Throwable ignored) {
            }
            try {
                SduEditCommandAccess.openNamedEditor(player, which);
                player.m_5661_(Component.m_237113_("§eOpening SDU " + which + " editor…"), false);
                LOGGER.info("[{}] SDU chest opened {} for {}", DmzMohistMeleeFix.MOD_ID, which, player.m_6302_());
            } catch (Throwable t) {
                player.m_5661_(Component.m_237113_("§cFailed to open SDU " + which + " editor."), false);
                LOGGER.warn(
                        "[{}] SDU chest editor {} failed for {}: {}",
                        DmzMohistMeleeFix.MOD_ID,
                        which,
                        player.m_6302_(),
                        t.toString()
                );
            }
        };
        if (server != null) {
            server.m_6937_(new TickTask(server.m_129921_() + 1, open));
        } else {
            open.run();
        }
    }
}
