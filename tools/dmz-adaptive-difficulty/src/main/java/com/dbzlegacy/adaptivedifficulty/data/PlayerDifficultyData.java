package com.dbzlegacy.adaptivedifficulty.data;

import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * V3 player difficulty data.
 * <p>
 * Permanent: highest DMZ level, unlocked tiers.<br>
 * Temporary: active tier / active difficulty (cleared on death / character reset).<br>
 * Ancient Coins are Lightman's {@code coin_ancient} inventory items;
 * {@code ancientCopper} is only kept for one-time migration from older builds.
 */
public final class PlayerDifficultyData {
    public static final String NBT_ROOT = "dmz_adaptive_difficulty";

    private long highestDmzLevel;
    private final Set<Integer> unlockedTiers = new LinkedHashSet<>();
    private long ancientCopper;

    private int activeTier;
    private long activeDifficultyLevel;
    private TeamMode teamMode = TeamMode.PERSONAL_ONLY;

    public long getHighestDmzLevel() {
        return highestDmzLevel;
    }

    public void noteDmzLevel(int level) {
        if (level > highestDmzLevel) {
            highestDmzLevel = level;
        }
    }

    public Set<Integer> getUnlockedTiers() {
        return new LinkedHashSet<>(unlockedTiers);
    }

    public boolean hasUnlockedTier(int tierId) {
        return tierId > 0 && unlockedTiers.contains(tierId);
    }

    /** @return true if newly unlocked */
    public boolean unlockTier(int tierId) {
        if (UnlockTier.byId(tierId) == null) {
            return false;
        }
        return unlockedTiers.add(tierId);
    }

    public int getActiveTier() {
        return Math.max(0, activeTier);
    }

    public void setActiveTier(int tierId) {
        if (tierId <= 0) {
            this.activeTier = 0;
            return;
        }
        this.activeTier = UnlockTier.byId(tierId) == null ? 0 : tierId;
    }

    public long getActiveDifficultyLevel() {
        return Math.max(0L, activeDifficultyLevel);
    }

    public void setActiveDifficultyLevel(long level) {
        this.activeDifficultyLevel = Math.max(0L, level);
    }

    public long getActiveDifficulty() {
        return getActiveDifficultyLevel();
    }

    public void setActiveDifficulty(long level) {
        setActiveDifficultyLevel(level);
    }

    /** Death / character-reset: clear temporary activation only. */
    public void resetTemporary() {
        this.activeTier = 0;
        this.activeDifficultyLevel = 0L;
        this.teamMode = TeamMode.PERSONAL_ONLY;
    }

    public long getAncientCopper() {
        return Math.max(0L, ancientCopper);
    }

    public void setAncientCopper(long amount) {
        this.ancientCopper = Math.max(0L, amount);
    }

    public boolean spendAncientCopper(long cost) {
        if (cost <= 0L) {
            return true;
        }
        if (ancientCopper < cost) {
            return false;
        }
        ancientCopper -= cost;
        return true;
    }

    public void addAncientCopper(long amount) {
        if (amount > 0L) {
            long base = Math.max(0L, ancientCopper);
            long next = base + amount;
            ancientCopper = next < base ? Long.MAX_VALUE / 4L : next;
        }
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
        tag.m_128356_("highestDmzLevel", highestDmzLevel);
        tag.m_128405_("activeTier", activeTier);
        tag.m_128356_("activeLevel", activeDifficultyLevel);
        tag.m_128356_("ancientCopper", ancientCopper);
        tag.m_128359_("teamMode", getTeamMode().name());
        ListTag tiers = new ListTag();
        for (Integer id : unlockedTiers) {
            CompoundTag t = new CompoundTag();
            t.m_128405_("id", id);
            tiers.add(t);
        }
        tag.m_128365_("unlockedTiers", tiers);
        // Legacy keys for old tooling — not used by V3.
        tag.m_128356_("purchased", 0L);
        tag.m_128356_("active", activeDifficultyLevel);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        highestDmzLevel = Math.max(0L, tag.m_128454_("highestDmzLevel"));
        activeTier = Math.max(0, tag.m_128451_("activeTier"));
        activeDifficultyLevel = Math.max(0L, tag.m_128454_("activeLevel"));
        ancientCopper = Math.max(0L, tag.m_128454_("ancientCopper"));
        teamMode = TeamMode.fromString(tag.m_128461_("teamMode"));
        unlockedTiers.clear();
        if (tag.m_128425_("unlockedTiers", 10)) {
            ListTag tiers = tag.m_128437_("unlockedTiers", 10);
            for (int i = 0; i < tiers.size(); i++) {
                CompoundTag t = tiers.m_128728_(i);
                int id = t.m_128451_("id");
                if (UnlockTier.byId(id) != null) {
                    unlockedTiers.add(id);
                }
            }
        }
        // Titles NBT is ignored (feature removed).

        long legacyPurchased = Math.max(0L, tag.m_128454_("purchased"));
        long legacyActive = Math.max(0L, tag.m_128454_("active"));
        if (activeTier <= 0 && (legacyActive > 0L || legacyPurchased > 0L)) {
            long seed = Math.max(legacyActive, legacyPurchased);
            UnlockTier best = UnlockTier.T1;
            for (UnlockTier t : UnlockTier.values()) {
                if (seed >= t.defaultRequiredLevel) {
                    best = t;
                }
            }
            unlockTier(best.id);
            activeTier = best.id;
            activeDifficultyLevel = Math.min(best.maxDifficulty(), Math.max(legacyActive, 1L));
            if (ancientCopper <= 0L && legacyPurchased > 0L) {
                ancientCopper = legacyPurchased;
            }
        }
        if (activeTier > 0 && UnlockTier.byId(activeTier) == null) {
            activeTier = 0;
            activeDifficultyLevel = 0L;
        }
    }

    public static PlayerDifficultyData fromPlayerNbt(CompoundTag persistent) {
        PlayerDifficultyData data = new PlayerDifficultyData();
        if (persistent != null && persistent.m_128441_(NBT_ROOT)) {
            data.load(persistent.m_128469_(NBT_ROOT));
        }
        return data;
    }

    public void writeToPlayerNbt(CompoundTag persistent) {
        if (persistent == null) {
            return;
        }
        persistent.m_128365_(NBT_ROOT, save());
    }
}
