package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

/** Removes an active Iron's Spells Heartstop effect. Recipes stay in KubeJS. */
public final class HeartstopStrip {
    public static final int INTERVAL = 40;

    private HeartstopStrip() {}

    public static void strip(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Collection<MobEffectInstance> effects;
        try {
            effects = player.m_21220_();
        } catch (Throwable t) {
            return;
        }
        if (effects == null || effects.isEmpty()) {
            return;
        }
        List<MobEffect> remove = null;
        for (MobEffectInstance inst : effects) {
            if (inst == null) {
                continue;
            }
            MobEffect effect = inst.m_19544_();
            if (effect == null || !isHeartstop(effect)) {
                continue;
            }
            if (remove == null) {
                remove = new ArrayList<>();
            }
            remove.add(effect);
        }
        if (remove == null) {
            return;
        }
        for (MobEffect effect : remove) {
            try {
                player.m_21195_(effect);
            } catch (Throwable ignored) {
            }
        }
        HotLog.debug("[Heartstop] stripped " + remove.size() + " effect(s)");
    }

    private static boolean isHeartstop(MobEffect effect) {
        String desc = "";
        try {
            desc = effect.m_19481_();
        } catch (Throwable ignored) {
        }
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        String key = (desc + " " + (id == null ? "" : id.toString())).toLowerCase();
        return key.contains("heartstop");
    }
}
