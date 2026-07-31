package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Unlock / equip cosmetic difficulty titles. */
public final class TitleSystem {
    private TitleSystem() {}

    /**
     * Grant every tier title the player has earned for their current active difficulty.
     *
     * @return newly unlocked display names (may be empty)
     */
    public static List<String> syncTierTitles(ServerPlayer player, boolean announce) {
        List<String> unlocked = new ArrayList<>();
        if (player == null) {
            return unlocked;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean dirty = data.normalizeTitles();
        for (DifficultyTitle title : DifficultyTitle.values()) {
            if (title.kind != DifficultyTitle.Kind.TIER || title.unlockTier == null) {
                continue;
            }
            if (snap.active < title.unlockTier.threshold()) {
                continue;
            }
            if (data.unlockTitle(title.id)) {
                unlocked.add(title.display);
                dirty = true;
            }
        }
        if (dirty) {
            DifficultyCache.save(player);
        }
        if (announce) {
            for (String name : unlocked) {
                player.m_213846_(Component.m_237113_("§6✦ Title unlocked: §e" + name));
            }
        }
        return unlocked;
    }

    /** Combat-feat unlocks from kills (boss / elite / high-tier grind). */
    public static void maybeUnlockCombatTitle(
            ServerPlayer killer, DifficultySnapshot snap, boolean elite, boolean boss, DifficultyTier tier
    ) {
        if (killer == null || snap == null) {
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(killer);
        data.normalizeTitles();
        DifficultyTitle grant = null;
        if (boss && tier != null && tier.ordinalPower() >= DifficultyTier.MASTER.ordinalPower()) {
            grant = DifficultyTitle.BOSS_SLAYER;
        } else if (elite && snap.active >= DifficultyTier.LEGENDARY.threshold()) {
            grant = DifficultyTitle.LEGENDARY_HUNTER;
        } else if (snap.active >= DifficultyTier.GOD.threshold()) {
            grant = DifficultyTitle.GOD_CHALLENGER;
        }
        if (grant != null && data.unlockTitle(grant.id)) {
            DifficultyCache.save(killer);
            killer.m_213846_(Component.m_237113_("§6✦ Title unlocked: §e" + grant.display));
        }
        // Also catch up any tier titles earned before Titles existed.
        syncTierTitles(killer, true);
    }

    public static boolean has(ServerPlayer player, DifficultyTitle title) {
        if (player == null || title == null) {
            return false;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.normalizeTitles();
        return data.hasTitle(title.id);
    }

    public static String activeDisplay(ServerPlayer player) {
        if (player == null) {
            return "none";
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.normalizeTitles();
        DifficultyTitle active = DifficultyTitle.byId(data.getActiveTitle());
        return active == null ? "none" : active.display;
    }

    public static String activeId(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.normalizeTitles();
        String id = data.getActiveTitle();
        return id == null ? "" : id;
    }

    public static List<String> unlockedDisplays(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.normalizeTitles();
        for (String id : data.getTitles()) {
            DifficultyTitle title = DifficultyTitle.byId(id);
            out.add(title == null ? id : title.display);
        }
        return out;
    }

    public static boolean equip(ServerPlayer player, String rawId) {
        if (player == null) {
            return false;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.normalizeTitles();
        DifficultyTitle title = DifficultyTitle.byId(rawId);
        if (title == null) {
            return false;
        }
        if (!data.hasTitle(title.id)) {
            return false;
        }
        data.setActiveTitle(title.id);
        DifficultyCache.save(player);
        return true;
    }

    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setActiveTitle("");
        DifficultyCache.save(player);
    }
}
