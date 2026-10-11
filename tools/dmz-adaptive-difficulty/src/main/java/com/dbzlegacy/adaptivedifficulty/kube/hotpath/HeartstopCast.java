package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.function.Consumer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/** Cancels irons_spellbooks:heartstop before it is cast. No compile dependency on that mod. */
public final class HeartstopCast {
    private static boolean hooked;

    private HeartstopCast() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        try {
            Class type = Class.forName("io.redspace.ironsspellbooks.api.events.SpellPreCastEvent");
            if (!Event.class.isAssignableFrom(type)) {
                return;
            }
            Consumer<Event> listener = HeartstopCast::cancel;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
            HotLog.debug("[Heartstop Disable] SpellPreCastEvent registered");
        } catch (ClassNotFoundException missing) {
            HotLog.debug("[Heartstop Disable] SpellPreCastEvent class missing");
        } catch (Throwable t) {
            HotLog.debug("[Heartstop Disable] SpellPreCastEvent register failed: " + t);
        }
    }

    private static void cancel(Event event) {
        try {
            if (!HotPathConfig.get().heartstop.enabled) {
                return;
            }
            Object id = event.getClass().getMethod("getSpellId").invoke(event);
            String spell = HotPathConfig.get().heartstop.spellId;
            if (id == null || spell == null || !spell.equalsIgnoreCase(String.valueOf(id))) {
                return;
            }
            event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }
}
