package com.dbzlegacy.adaptivedifficulty.data;

import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * V3 player difficulty data.
 * <p>
 * Permanent: highest DMZ level, unlocked tiers, titles.<br>
 * Temporary: active tier / active difficulty level (reset on death).<br>
 * Ancient Coins are real Lightman's {@code coin_ancient} inventory items;
 * {@code ancientCopper} is only kept for one-time migration from older builds.
 */
public final class PlayerDifficultyData {
    public static final String NBT_ROOT = "dmz_adaptive_difficulty";

    // ── Permanent ──────────────────────────────────────────────────────────
    private long highestDmzLevel;
    private final Set<Integer> unlockedTiers = new LinkedHashSet<>();
    private long ancientCopper;
    private final Set<String> titles = new LinkedHashSet<>();
    private String activeTitle = "";
    /** Player opt-in: when false, no scaling / kill coins / death reset for them. */
    private boolean personalEnabled = true;
    /** When true, chat notifies on Ancient Coin kill drops. Default off (less spam). */
    private boolean coinDropChat = false;

    // ── Temporary (death / character-reset clears) ─────────────────────────
    private int activeTier;
    private long activeDifficultyLevel;
    private TeamMode teamMode = TeamMode.PERSONAL_ONLY;

    // Legacy migration fields (not used by V3 logic after load)
    private long legacyPurchased;
    private long legacyActive;

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

    public boolean isPersonalEnabled() {
        return personalEnabled;
    }

    public void setPersonalEnabled(boolean enabled) {
        this.personalEnabled = enabled;
    }

    public boolean togglePersonalEnabled() {
        this.personalEnabled = !this.personalEnabled;
        return this.personalEnabled;
    }

    public boolean isCoinDropChat() {
        return coinDropChat;
    }

    public void setCoinDropChat(boolean enabled) {
        this.coinDropChat = enabled;
    }

    public boolean toggleCoinDropChat() {
        this.coinDropChat = !this.coinDropChat;
        return this.coinDropChat;
    }

    /**
     * Death / character-reset: clear temporary activation only.
     * Keeps prestige, unlock tiers, toggles, and Ancient Coin wallet.
     */
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

    /** Teams are WIP — always personal-only until that feature ships. */
    public TeamMode getTeamMode() {
        return TeamMode.PERSONAL_ONLY;
    }

    public void setTeamMode(TeamMode teamMode) {
        this.teamMode = TeamMode.PERSONAL_ONLY;
    }

    public List<String> getTitles() {
        return new ArrayList<>(titles);
    }

    public boolean hasTitle(String titleId) {
        if (titleId == null || titleId.isBlank()) {
            return false;
        }
        return titles.contains(normalizeId(titleId));
    }

    public boolean unlockTitle(String title) {
        DifficultyTitle known = DifficultyTitle.byId(title);
        String id = known != null ? known.id : normalizeId(title);
        if (id == null || id.isBlank()) {
            return false;
        }
        return titles.add(id);
    }

    public String getActiveTitle() {
        return activeTitle == null ? "" : activeTitle;
    }

    public void setActiveTitle(String titleId) {
        if (titleId == null || titleId.isBlank()) {
            this.activeTitle = "";
            return;
        }
        DifficultyTitle known = DifficultyTitle.byId(titleId);
        this.activeTitle = known != null ? known.id : normalizeId(titleId);
    }

    public boolean normalizeTitles() {
        boolean dirty = false;
        List<String> snapshot = new ArrayList<>(titles);
        titles.clear();
        for (String raw : snapshot) {
            DifficultyTitle known = DifficultyTitle.byId(raw);
            String id = known != null ? known.id : normalizeId(raw);
            if (id == null || id.isBlank()) {
                dirty = true;
                continue;
            }
            if (known == null && !id.equals(raw)) {
                dirty = true;
            } else if (known != null && !known.id.equals(raw)) {
                dirty = true;
            }
            titles.add(id);
        }
        if (snapshot.size() != titles.size()) {
            dirty = true;
        }
        if (activeTitle != null && !activeTitle.isBlank()) {
            DifficultyTitle known = DifficultyTitle.byId(activeTitle);
            String id = known != null ? known.id : normalizeId(activeTitle);
            if (id == null || id.isBlank() || !titles.contains(id)) {
                activeTitle = "";
                dirty = true;
            } else if (!id.equals(activeTitle)) {
                activeTitle = id;
                dirty = true;
            }
        } else if (activeTitle == null) {
            activeTitle = "";
            dirty = true;
        }
        return dirty;
    }

    public CompoundTag save() {
        normalizeTitles();
        CompoundTag tag = new CompoundTag();
        tag.m_128356_("highestDmzLevel", highestDmzLevel);
        tag.m_128405_("activeTier", activeTier); // putInt
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
        ListTag list = new ListTag();
        for (String t : titles) {
            list.add(StringTag.m_129297_(t));
        }
        tag.m_128365_("titles", list);
        tag.m_128359_("activeTitle", getActiveTitle());
        tag.m_128379_("personalEnabled", personalEnabled);
        tag.m_128379_("coinDropChat", coinDropChat);
        // Keep legacy keys written as 0 so old tools don't explode on read.
        tag.m_128356_("purchased", 0L);
        tag.m_128356_("active", activeDifficultyLevel);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        highestDmzLevel = Math.max(0L, tag.m_128454_("highestDmzLevel"));
        activeTier = Math.max(0, tag.m_128451_("activeTier")); // getInt
        activeDifficultyLevel = Math.max(0L, tag.m_128454_("activeLevel"));
        ancientCopper = Math.max(0L, tag.m_128454_("ancientCopper"));
        teamMode = TeamMode.fromString(tag.m_128461_("teamMode"));
        // Missing keys → defaults (on for personal, off for coin chat).
        personalEnabled = !tag.m_128441_("personalEnabled") || tag.m_128471_("personalEnabled");
        coinDropChat = tag.m_128441_("coinDropChat") && tag.m_128471_("coinDropChat");
        unlockedTiers.clear();
        if (tag.m_128425_("unlockedTiers", 10)) { // TAG_COMPOUND=10
            ListTag tiers = tag.m_128437_("unlockedTiers", 10);
            for (int i = 0; i < tiers.size(); i++) {
                CompoundTag t = tiers.m_128728_(i); // getCompound
                int id = t.m_128451_("id");
                if (UnlockTier.byId(id) != null) {
                    unlockedTiers.add(id);
                }
            }
        }
        titles.clear();
        if (tag.m_128425_("titles", 8)) {
            ListTag list = tag.m_128437_("titles", 8);
            for (int i = 0; i < list.size(); i++) {
                String s = list.m_128778_(i);
                if (s != null && !s.isBlank()) {
                    titles.add(s);
                }
            }
        }
        activeTitle = tag.m_128461_("activeTitle");
        if (activeTitle == null) {
            activeTitle = "";
        }

        // Migrate pre-V3 purchased/active → best-effort temporary activation.
        legacyPurchased = Math.max(0L, tag.m_128454_("purchased"));
        legacyActive = Math.max(0L, tag.m_128454_("active"));
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
                // Old "purchased" was difficulty points, not copper coins — soft token only.
                ancientCopper = Math.min(1_000L, legacyPurchased);
            }
        }
        if (activeTier > 0 && UnlockTier.byId(activeTier) == null) {
            activeTier = 0;
            activeDifficultyLevel = 0L;
        }
        normalizeTitles();
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

    private static String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
