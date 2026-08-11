package com.dbzlegacy.adaptivedifficulty.title;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Unlock / equip cosmetic difficulty titles (stricter requirements). */
public final class TitleSystem {
    private TitleSystem() {}

    /**
     * Grant tier titles the player currently qualifies for.
     * Requires the matching Unlock Tier to be <b>active</b>, plus elevated DMZ/Prestige.
     */
    public static List<String> syncTierTitles(ServerPlayer player, boolean announce) {
        List<String> unlocked = new ArrayList<>();
        if (player == null) {
            return unlocked;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean dirty = data.normalizeTitles();
        int level = DmzProgression.dmzLevelForProgression(player);
        int prestige = DmzProgression.prestige(player);
        for (DifficultyTitle title : DifficultyTitle.values()) {
            if (title.kind != DifficultyTitle.Kind.TIER || title.unlockTier == null) {
                continue;
            }
            if (snap.activeTier < title.unlockTier.id) {
                continue;
            }
            if (level < title.requiredDmzLevel && prestige < title.requiredPrestige) {
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

    /** Combat-feat unlocks — harder gates than before. */
    public static void maybeUnlockCombatTitle(
            ServerPlayer killer, DifficultySnapshot snap, boolean elite, boolean boss
    ) {
        if (killer == null || snap == null) {
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(killer);
        data.normalizeTitles();
        DifficultyTitle grant = null;
        if (boss && snap.activeTier >= 5) {
            grant = DifficultyTitle.BOSS_SLAYER;
        } else if (elite && snap.activeTier >= 6) {
            grant = DifficultyTitle.ELITE_HUNTER;
        } else if (snap.activeTier >= 7) {
            grant = DifficultyTitle.ASCENDANT;
        }
        if (grant != null && data.unlockTitle(grant.id)) {
            DifficultyCache.save(killer);
            killer.m_213846_(Component.m_237113_("§6✦ Title unlocked: §e" + grant.display));
        }
        syncTierTitles(killer, true);
    }

    public static boolean has(ServerPlayer player, DifficultyTitle title) {
        if (player == null || title == null) {
            return false;
        }
        return DifficultyCache.data(player).hasTitle(title.id);
    }

    public static String activeId(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return DifficultyCache.data(player).getActiveTitle();
    }

    public static String activeDisplay(ServerPlayer player) {
        DifficultyTitle title = DifficultyTitle.byId(activeId(player));
        return title == null ? "None" : title.display;
    }

    public static List<String> unlockedDisplays(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        for (String id : DifficultyCache.data(player).getTitles()) {
            DifficultyTitle title = DifficultyTitle.byId(id);
            out.add(title == null ? id : title.display);
        }
        return out;
    }

    public static boolean equip(ServerPlayer player, String titleId) {
        if (player == null) {
            return false;
        }
        DifficultyTitle title = DifficultyTitle.byId(titleId);
        if (title == null || !has(player, title)) {
            return false;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
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
