package com.dbzlegacy.adaptivedifficulty.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Persisted per-player difficulty fields from the concept doc.
 * Calculated difficulty is derived live from DMZ level/prestige and not stored.
 */
public final class PlayerDifficultyData {
    public static final String NBT_ROOT = "dmz_adaptive_difficulty";

    private long purchasedDifficulty;
    private long activeDifficulty;
    private TeamMode teamMode = TeamMode.PERSONAL_ONLY;
    private final Set<String> titles = new LinkedHashSet<>();

    public long getPurchasedDifficulty() {
        return purchasedDifficulty;
    }

    public void setPurchasedDifficulty(long purchasedDifficulty) {
        this.purchasedDifficulty = Math.max(0L, purchasedDifficulty);
    }

    public long getActiveDifficulty() {
        return activeDifficulty;
    }

    public void setActiveDifficulty(long activeDifficulty) {
        this.activeDifficulty = Math.max(0L, activeDifficulty);
    }

    public TeamMode getTeamMode() {
        return teamMode == null ? TeamMode.PERSONAL_ONLY : teamMode;
    }

    public void setTeamMode(TeamMode teamMode) {
        this.teamMode = teamMode == null ? TeamMode.PERSONAL_ONLY : teamMode;
    }

    public void cycleTeamMode() {
        TeamMode current = getTeamMode();
        this.teamMode = switch (current) {
            case PERSONAL_ONLY -> TeamMode.THRESHOLD_BONUS_ONLY;
            case THRESHOLD_BONUS_ONLY -> TeamMode.FULL_TEAM_SCALING;
            case FULL_TEAM_SCALING -> TeamMode.PERSONAL_ONLY;
        };
    }

    public List<String> getTitles() {
        return new ArrayList<>(titles);
    }

    /** @return true if newly unlocked */
    public boolean unlockTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        return titles.add(title.trim());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.m_128356_("purchased", purchasedDifficulty); // putLong
        tag.m_128356_("active", activeDifficulty);
        tag.m_128359_("teamMode", getTeamMode().name()); // putString
        ListTag list = new ListTag();
        for (String t : titles) {
            list.add(StringTag.m_129297_(t));
        }
        tag.m_128365_("titles", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        purchasedDifficulty = Math.max(0L, tag.m_128454_("purchased")); // getLong
        activeDifficulty = Math.max(0L, tag.m_128454_("active"));
        teamMode = TeamMode.fromString(tag.m_128461_("teamMode")); // getString
        titles.clear();
        if (tag.m_128425_("titles", 8)) { // contains list of strings (TAG_STRING=8)
            ListTag list = tag.m_128437_("titles", 8);
            for (int i = 0; i < list.size(); i++) {
                String s = list.m_128778_(i); // getString
                if (s != null && !s.isBlank()) {
                    titles.add(s);
                }
            }
        }
    }

    public static PlayerDifficultyData fromPlayerNbt(CompoundTag persistent) {
        PlayerDifficultyData data = new PlayerDifficultyData();
        if (persistent != null && persistent.m_128441_(NBT_ROOT)) { // contains
            data.load(persistent.m_128469_(NBT_ROOT)); // getCompound
        }
        return data;
    }

    public void writeToPlayerNbt(CompoundTag persistent) {
        if (persistent == null) {
            return;
        }
        persistent.m_128365_(NBT_ROOT, save()); // put
    }
}
