package com.dbzlegacy.adaptivedifficulty.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Persisted per-player difficulty fields from the concept doc.
 * Calculated difficulty is derived live from DMZ level/prestige and not stored.
 */
public final class PlayerDifficultyData {
    public static final String NBT_ROOT = "dmz_adaptive_difficulty";

    private long purchasedDifficulty;
    private long activeDifficulty;
    private TeamMode teamMode = TeamMode.PERSONAL_ONLY;

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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.m_128356_("purchased", purchasedDifficulty); // putLong
        tag.m_128356_("active", activeDifficulty);
        tag.m_128359_("teamMode", getTeamMode().name()); // putString
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        purchasedDifficulty = Math.max(0L, tag.m_128454_("purchased")); // getLong
        activeDifficulty = Math.max(0L, tag.m_128454_("active"));
        teamMode = TeamMode.fromString(tag.m_128461_("teamMode")); // getString
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
