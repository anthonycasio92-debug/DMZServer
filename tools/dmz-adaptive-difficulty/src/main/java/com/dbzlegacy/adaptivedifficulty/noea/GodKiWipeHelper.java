package com.dbzlegacy.adaptivedifficulty.noea;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.skills.Skills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Clears God Ki progress on the same resets that wipe Majin absorption.
 * Lives outside the mixin package so the server classloader can find it.
 *
 * <p>Noea 1.2.0-DMZ-2.1.3 does not store this as a {@code godkienhancement}
 * compound. That string is the stack-form group and the skill id.
 * {@code NoeaGodKiLedgerIdentity} is a string on player persistent data.
 * {@code GodKiVariantLedger} is a static catalog, not per-player state.
 * {@code demonGodTier} is computed from the worn demon form, so it is not a
 * stored key. {@code godforms} is a separate form group and is left alone.
 * Death does not wipe Majin absorption, and it does not wipe God Ki either.
 */
public final class GodKiWipeHelper {
    static final String LEDGER_KEY = "NoeaGodKiLedgerIdentity";
    /** Present in the task notes. Noea does not write this key. Removed if a later build does. */
    static final String ENHANCEMENT_KEY = "godkienhancement";
    static final String GROUP = "godkienhancement";
    static final String FORM = "enhancewithgodki";
    static final String SKILL = "godkienhancement";
    private static final double MASTERY_CAP = 100.0D;

    private GodKiWipeHelper() {}

    public static void wipe(Player player, String via) {
        if (player == null) {
            return;
        }
        String name = "null";
        try {
            name = player.m_7755_().getString();
        } catch (Throwable ignored) {
            name = "?";
        }
        String where = via == null || via.isEmpty() ? "" : " via " + via;
        System.out.println("[LM] wipeGodKi firing for " + name + where);
        try {
            String ledger = removePersistent(player, LEDGER_KEY);
            String enhancement = removePersistent(player, ENHANCEMENT_KEY);
            int skill = clearSkill(player);
            boolean form = clearStackForm(player);
            double mastery = clearMastery(player);
            if ((skill >= 0 || form || mastery > 0.0D) && player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
            }
            System.out.println("[LM] wipeGodKi complete ledger=" + ledger
                    + " enhancement=" + enhancement
                    + " skill=" + skill
                    + " form=" + form
                    + " mastery=" + mastery);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
            }
        }
    }

    /** Wipe only when character creation is about to replace the current race. */
    public static void wipeIfRaceChanges(StatsData stats, String nextRace) {
        if (stats == null || nextRace == null || nextRace.isBlank()) {
            return;
        }
        String current = "";
        try {
            Character character = stats.getCharacter();
            if (character != null && character.getRace() != null) {
                current = character.getRace();
            }
        } catch (Throwable ignored) {
            return;
        }
        if (current.isBlank() || current.equalsIgnoreCase(nextRace.trim())) {
            return;
        }
        try {
            wipe(stats.getPlayer(), "initializeWithRaceAndClass");
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
            }
        }
    }

    /** @return {@code absent}, {@code compound}, {@code string}, or {@code other} */
    private static String removePersistent(Player player, String key) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(key)) {
            return "absent";
        }
        String kind = "other";
        if (tag.m_128425_(key, 10)) {
            kind = "compound";
        } else if (tag.m_128425_(key, 8)) {
            kind = "string";
        }
        tag.m_128473_(key);
        return kind;
    }

    /** @return the skill level before removal, or {@code -1} when the skill was absent */
    private static int clearSkill(Player player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return -1;
        }
        Skills skills = data.getSkills();
        if (skills == null) {
            return -1;
        }
        int level = 0;
        boolean present = false;
        try {
            present = skills.hasSkill(SKILL);
            level = Math.max(0, skills.getSkillLevel(SKILL));
        } catch (Throwable ignored) {
        }
        if (!present && level <= 0) {
            return -1;
        }
        skills.removeSkill(SKILL);
        return level;
    }

    private static boolean clearStackForm(Player player) {
        Character character = DmzProgression.character(player);
        if (character == null) {
            return false;
        }
        boolean cleared = false;
        if (isGroup(character.getActiveStackFormGroup())) {
            character.clearActiveStackForm(player, false);
            cleared = true;
        }
        if (isGroup(character.getSelectedStackFormGroup())) {
            character.setSelectedStackFormGroup("");
            character.setSelectedStackForm("");
            cleared = true;
        }
        return cleared;
    }

    private static double clearMastery(Player player) {
        Character character = DmzProgression.character(player);
        if (character == null) {
            return 0.0D;
        }
        double cleared = 0.0D;
        cleared = Math.max(cleared, zeroMastery(character.getStackFormMasteries()));
        cleared = Math.max(cleared, zeroMastery(character.getFormMasteries()));
        return cleared;
    }

    private static double zeroMastery(FormMasteries masteries) {
        if (masteries == null) {
            return 0.0D;
        }
        double current = masteries.getMastery(GROUP, FORM);
        if (current <= 0.0D) {
            return 0.0D;
        }
        masteries.setMastery(GROUP, FORM, 0.0D, MASTERY_CAP);
        return current;
    }

    private static boolean isGroup(String group) {
        return group != null && GROUP.equalsIgnoreCase(group.trim());
    }
}
