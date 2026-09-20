package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.RaceClassSync;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.RaceSkillSync;
import com.dbzlegacy.adaptivedifficulty.progression.race.RaceLock;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.progression.classdef.FightingClassCatalog;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Stats;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Paid race / class / reskin services integrated with DragonMineZ + Ancient Coins. */
public final class CharacterServicesSystem {
    private CharacterServicesSystem() {}

    public static record TransferableStats(
            int strength,
            int strike,
            int resistance,
            int vitality,
            int kiPower,
            int energy
    ) {
        TransferableStats scaled(int preservationPercent) {
            int pct = Math.max(0, Math.min(100, preservationPercent));
            return new TransferableStats(
                    scale(strength, pct),
                    scale(strike, pct),
                    scale(resistance, pct),
                    scale(vitality, pct),
                    scale(kiPower, pct),
                    scale(energy, pct)
            );
        }

        private static int scale(int v, int pct) {
            if (v <= 0 || pct <= 0) {
                return 0;
            }
            return Math.max(0, (int) Math.round(v * (pct / 100.0)));
        }
    }

    public static String blockReason(ServerPlayer player) {
        return blockReason(player, false);
    }

    /** @param cosmetic when true (reskin), skip transform lock — appearance-only */
    public static String blockReason(ServerPlayer player, boolean cosmetic) {
        CharacterServicesConfig cfg = CharacterServicesConfig.get();
        if (!cfg.enabled) {
            return "§cCharacter services are disabled.";
        }
        if (!CharacterServicesAccess.canUseServices(player)) {
            return "§cYou do not have access to Character Services.";
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return "§cCharacter data is not ready yet. Try again in a moment.";
        }
        Status status = data.getStatus();
        if (cfg.restrictions.blockWhileDead && status != null && !status.isAlive()) {
            return "§cYou cannot modify your character while dead.";
        }
        if (cfg.restrictions.blockWhileSparring && inActiveSpar(player)) {
            return "§cYou cannot modify your character during an active spar.";
        }
        return "";
    }

    private static boolean inActiveSpar(ServerPlayer player) {
        try {
            Class<?> spar = Class.forName("com.dbzlegacy.adaptivedifficulty.sparring.SparCombat");
            var m = spar.getMethod("isInActiveFight", ServerPlayer.class);
            Object r = m.invoke(null, player);
            return r instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static List<String> raceCards(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        String current = DmzProgression.race(player);
        Set<String> blocked = CharacterServicesConfig.get().blockedRaceSet();
        try {
            for (String race : DmzContentDiscovery.discoverRaceIds()) {
                if (race == null || race.isBlank()) {
                    continue;
                }
                String id = race.trim().toLowerCase(Locale.ROOT);
                if (blocked.contains(id)) {
                    continue;
                }
                if (!CharacterServicesAccess.bypassRaceLock(player)) {
                    if (RaceLock.selectBlockReason(player, id) != null) {
                        continue;
                    }
                }
                String name = titleCase(id);
                boolean selected = id.equalsIgnoreCase(current);
                out.add(id + "\t" + name + "\t" + (selected ? "1" : "0"));
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] race list failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        return out;
    }

    public static List<String> classCards(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        String race = DmzProgression.race(player);
        String current = DmzProgression.fightingClass(player);
        List<String> raceClasses = FightingClassCatalog.classIdsForRace(race);
        if (raceClasses.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] no DMZ classes for race {} in character services GUI",
                    AdaptiveDifficultyMod.MOD_ID,
                    race);
            return out;
        }
        for (String id : raceClasses) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String name = FightingClassLabels.display(id);
            if (name == null || name.isBlank() || "Not set".equals(name)) {
                name = FightingClassCatalog.skillNameFor(id);
            }
            if (name == null || name.isBlank()) {
                name = titleCase(id);
            }
            out.add(id + "\t" + name + "\t" + (id.equalsIgnoreCase(current) ? "1" : "0"));
        }
        return out;
    }

    public static List<String> statPreviewLines(ServerPlayer player, int preservationPercent) {
        List<String> lines = new ArrayList<>();
        TransferableStats now = captureStats(player);
        if (now == null) {
            lines.add("§7We could not read your stats yet — try again in a moment.");
            return lines;
        }
        TransferableStats kept = now.scaled(preservationPercent);
        lines.add("§8After change (at " + preservationPercent + "%):");
        lines.add(statLine("Strength", now.strength, kept.strength));
        lines.add(statLine("Strike", now.strike, kept.strike));
        lines.add(statLine("Defense", now.resistance, kept.resistance));
        lines.add(statLine("Vitality", now.vitality, kept.vitality));
        lines.add(statLine("Ki Power", now.kiPower, kept.kiPower));
        lines.add(statLine("Energy", now.energy, kept.energy));
        return lines;
    }

    public static long scaledCost(ServerPlayer player, long baseCopper, boolean useLevelMult) {
        if (baseCopper <= 0L) {
            return 0L;
        }
        if (!useLevelMult) {
            return baseCopper;
        }
        double mult = DifficultyConfig.get().tierCostLevelMultiplier(DmzProgression.tierScalingDmzLevel(player));
        return Math.max(1L, Math.round(baseCopper * mult));
    }

    public static long raceCost(ServerPlayer player, int preservationPercent) {
        long base = CharacterServicesConfig.get().raceCostCopper(preservationPercent);
        return payableCost(player, base, CharacterServicesConfig.get().raceChange.levelCostMultiplier);
    }

    public static long classCost(ServerPlayer player) {
        CharacterServicesConfig.ClassChange cc = CharacterServicesConfig.get().classChange;
        return payableCost(player, cc.baseCostCopper, cc.levelCostMultiplier);
    }

    public static long reskinCost(ServerPlayer player) {
        CharacterServicesConfig.Reskin rs = CharacterServicesConfig.get().reskin;
        return payableCost(player, rs.baseCostCopper, rs.levelCostMultiplier);
    }

    /** Level-scaled copper, snapped to payable Ancient Coin denominations (same as AD tier buys). */
    public static long payableCost(ServerPlayer player, long baseCopper, boolean useLevelMult) {
        return AncientCoinEconomy.normalizeCost(scaledCost(player, baseCopper, useLevelMult));
    }

    public static String formatCost(long copperCost) {
        return AncientCoinEconomy.formatExactCost(copperCost);
    }

    private static boolean chargeAc(ServerPlayer player, long cost) {
        if (cost <= 0L) {
            return true;
        }
        AncientCoinEconomy.migrateWalletToItems(player);
        return AncientCoinEconomy.charge(player, cost);
    }

    private static String insufficientFunds(ServerPlayer player, long cost) {
        return "§c" + AncientCoinEconomy.missingText(player, cost);
    }

    public static String cooldownLine(ServerPlayer player, String kind) {
        if (CharacterServicesAccess.bypassCooldown(player)) {
            return "§aReady whenever you are §8(staff bypass)";
        }
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        long now = System.currentTimeMillis();
        long last;
        long cd;
        switch (kind == null ? "" : kind.toLowerCase(Locale.ROOT)) {
            case "race" -> {
                last = rec.lastRaceChangeAt;
                cd = CharacterServicesConfig.get().raceChange.cooldownMs;
            }
            case "class" -> {
                last = rec.lastClassChangeAt;
                cd = CharacterServicesConfig.get().classChange.cooldownMs;
            }
            case "reskin" -> {
                last = rec.lastReskinAt;
                cd = CharacterServicesConfig.get().reskin.cooldownMs;
            }
            default -> {
                return "§7—";
            }
        }
        long left = last + cd - now;
        if (left <= 0L) {
            return "§aReady to use";
        }
        return "§7On cooldown — ready in §f" + formatDuration(left);
    }

    private static String statLine(String label, int before, int after) {
        return "§7" + label + " §f" + format(before) + " §8→ §f" + format(after);
    }

    public static String executeRaceChange(ServerPlayer player, String targetRace, int preservationPercent) {
        String block = blockReason(player);
        if (!block.isEmpty()) {
            return block;
        }
        if (!CharacterServicesAccess.canRaceChange(player)) {
            return "§cYou cannot change race.";
        }
        CharacterServicesConfig cfg = CharacterServicesConfig.get();
        if (!cfg.raceChange.enabled) {
            return "§cRace change is disabled.";
        }
        if (!isValidPreservation(preservationPercent)) {
            return "§cInvalid preservation percentage.";
        }
        String raceId = normalizeId(targetRace);
        if (raceId.isEmpty() || !isRaceAvailable(raceId)) {
            return "§cThat race is currently unavailable.";
        }
        String currentRace = DmzProgression.race(player);
        if (raceId.equalsIgnoreCase(currentRace)) {
            return "§cYou are already that race.";
        }
        if (!CharacterServicesAccess.bypassRaceLock(player)) {
            String lock = RaceLock.selectBlockReason(player, raceId);
            if (lock != null && !lock.isBlank()) {
                return lock;
            }
        }
        if (!CharacterServicesAccess.bypassCooldown(player)) {
            long left = cooldownRemaining(player, "race");
            if (left > 0L) {
                return "§cYou must wait §f" + formatDuration(left) + " §cbefore another race change.";
            }
        }
        releaseTransformation(player);
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : raceCost(player, preservationPercent);
        AncientCoinEconomy.migrateWalletToItems(player);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return insufficientFunds(player, cost);
        }

        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return "§cCharacter data unavailable.";
        }
        boolean fullWipe = preservationPercent <= 0;
        TransferableStats before = null;
        TransferableStats target = null;
        if (!fullWipe) {
            before = captureStats(player);
            if (before == null) {
                return "§cCould not read your stats.";
            }
            target = before.scaled(preservationPercent);
        }

        if (cost > 0L && !chargeAc(player, cost)) {
            return insufficientFunds(player, cost);
        }

        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                refund(player, cost);
                return "§cCharacter data unavailable.";
            }
            clearForms(ch, player);
            String priorHeadBone = CosmeticHeadBoneService.activeBone(player);
            String priorClassBeforeRaceChange = "";
            boolean openedFreeClassPicker = false;
            if (fullWipe) {
                applyFullProgressWipe(player, data);
                RaceChangeCreationFlow.begin(player, raceId, priorHeadBone);
                RaceChangeCreationFlow.prepareCharacterData(player, data, raceId);
                RaceChangeCreationFlow.openEditor(player);
            } else {
                String priorClass = DmzProgression.fightingClass(player);
                if (priorClass == null || priorClass.isBlank()) {
                    try {
                        priorClass = ch.getCharacterClass();
                    } catch (Throwable ignored) {
                    }
                }
                priorClassBeforeRaceChange = priorClass == null ? "" : priorClass;
                boolean keepSkills = cfg.raceChange.keepSkillsOnRaceChange;
                List<RaceChangeSkillPreserve.Entry> skillSnapshot =
                        keepSkills ? RaceChangeSkillPreserve.capture(data.getSkills()) : List.of();
                if (RaceChangeClassMapper.requiresClassPicker(priorClassBeforeRaceChange, raceId)) {
                    float[] pickerResourceSnapshot = data.snapshotMultiplierResources();
                    RaceChangeClassMapper.commitFightingClassForRace(
                            data, raceId, "", priorClassBeforeRaceChange);
                    com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion.stripIfRaceIneligible(
                            player, raceId);
                    try {
                        data.updateTransformationSkillLimits(raceId);
                    } catch (Throwable ignored) {
                    }
                    DmzClassCommandApply.pushStatsSync(player);
                    CosmeticHeadBoneService.reapplyHeadBoneAfterRaceChange(player, priorHeadBone);
                    if (target != null) {
                        applyStats(data.getStats(), target);
                    }
                    // Full class sync runs when the player finishes recustomize (packet mixin hook).
                    if (keepSkills) {
                        RaceChangeSkillPreserve.restore(data, currentRace, raceId, skillSnapshot);
                        RaceClassSync.syncRaceSkillOnly(player);
                    } else {
                        RaceSkillSync.sync(player, raceId);
                        RaceClassSync.sync(player);
                    }
                    RaceChangeClassPickFlow.begin(
                            player, raceId, priorClassBeforeRaceChange, pickerResourceSnapshot);
                    RaceChangeClassPickFlow.openRecustomizeEditor(player);
                    openedFreeClassPicker = true;
                } else {
                    float[] resourceSnapshot = data.snapshotMultiplierResources();
                    String mappedClass =
                            RaceChangeClassMapper.applyRaceAndFightingClass(
                                    player, data, raceId, priorClass);
                    if (mappedClass != null
                            && !mappedClass.isBlank()
                            && !priorClassBeforeRaceChange.isBlank()
                            && !mappedClass.equalsIgnoreCase(priorClassBeforeRaceChange)) {
                        AdaptiveDifficultyMod.LOGGER.info(
                                "[{}] race change remapped fighting class {} → {} for race {}",
                                AdaptiveDifficultyMod.MOD_ID,
                                priorClassBeforeRaceChange,
                                mappedClass,
                                raceId);
                    }
                    CosmeticHeadBoneService.reapplyHeadBoneAfterRaceChange(player, priorHeadBone);
                    if (target != null) {
                        applyStats(data.getStats(), target);
                    }
                    DmzCharacterClassChangeHooks.onServicesRaceChangeApplied(
                            player, data, raceId, mappedClass, resourceSnapshot, target != null);
                    if (keepSkills) {
                        RaceChangeSkillPreserve.restore(data, currentRace, raceId, skillSnapshot);
                        RaceClassSync.syncRaceSkillOnly(player);
                    } else {
                        RaceSkillSync.sync(player, raceId);
                        RaceClassSync.sync(player);
                    }
                }
            }
            CharacterServicesStore.get().record(player.m_20148_().toString()).lastRaceChangeAt =
                    System.currentTimeMillis();
            CharacterServicesStore.get().markDirty();
            audit(player, "Race Change", currentRace, raceId, preservationPercent, cost, true);
            String paid = cost > 0L ? " §7Paid §f" + formatCost(cost) + "§7." : "";
            if (fullWipe) {
                return "§aOpening character setup for §f" + titleCase(raceId)
                        + "§a. §7Pick your class and appearance — free full wipe."
                        + " §7Purchased head parts stay unlocked."
                        + paid;
            }
            if (openedFreeClassPicker) {
                return "§aRace changed to §f" + titleCase(raceId)
                        + "§a. Eligible stats preserved at §f" + preservationPercent + "%§a."
                        + " §7Your old class is not available on this race — §fchoose a new class§7"
                        + " in the editor (§fno extra cost§7)."
                        + (cfg.raceChange.keepSkillsOnRaceChange
                                ? " §7Ki skills and shared form progress kept."
                                : "")
                        + " §7Head part unlocks are kept."
                        + paid;
            }
            boolean keepSkills = cfg.raceChange.keepSkillsOnRaceChange;
            String kept = keepSkills
                    ? " §7Ki skills, techniques, and shared form progress kept."
                    : "";
            String classNote = "";
            if (!fullWipe) {
                String cls = DmzProgression.fightingClass(player);
                if (cls != null && !cls.isBlank()) {
                    String label = FightingClassLabels.display(cls);
                    if (label == null || label.isBlank() || "Not set".equals(label)) {
                        label = titleCase(cls);
                    }
                    classNote = " §7Fighting class §f" + label + " §7(" + raceId + " stats).";
                }
            }
            return "§aRace changed to §f" + titleCase(raceId)
                    + "§a. Eligible stats preserved at §f" + preservationPercent + "%§a."
                    + classNote
                    + kept
                    + " §7Head part unlocks are kept."
                    + paid;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] race change failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    player.m_6302_(),
                    t.toString());
            refund(player, cost);
            audit(player, "Race Change", currentRace, raceId, preservationPercent, cost, false);
            return "§cThe character change could not be completed. Payment was refunded when possible.";
        }
    }

    public static String executeClassChange(ServerPlayer player, String targetClass) {
        String block = blockReason(player);
        if (!block.isEmpty()) {
            return block;
        }
        if (!CharacterServicesAccess.canClassChange(player)) {
            return "§cYou cannot change class.";
        }
        CharacterServicesConfig.ClassChange cc = CharacterServicesConfig.get().classChange;
        if (!cc.enabled) {
            return "§cClass change is disabled.";
        }
        String classId = normalizeId(targetClass);
        String race = DmzProgression.race(player);
        if (classId.isEmpty()) {
            return "§cThat class is not available for your race.";
        }
        String current = DmzProgression.fightingClass(player);
        if (classId.equalsIgnoreCase(current)) {
            return "§cYou are already that class.";
        }
        if (!CharacterServicesAccess.bypassCooldown(player)) {
            long left = cooldownRemaining(player, "class");
            if (left > 0L) {
                return "§cYou must wait §f" + formatDuration(left) + " §cbefore another class change.";
            }
        }
        releaseTransformation(player);
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : classCost(player);
        AncientCoinEconomy.migrateWalletToItems(player);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return insufficientFunds(player, cost);
        }

        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return "§cCharacter data unavailable.";
        }
        if (!DmzClassCommandApply.isValidClass(data, classId)) {
            return "§cThat class is not available for your race.";
        }
        TransferableStats before = cc.preserveBaseStats ? captureStats(player) : null;
        float[] resourceSnapshot = data.snapshotMultiplierResources();

        if (cost > 0L && !chargeAc(player, cost)) {
            return insufficientFunds(player, cost);
        }

        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                refund(player, cost);
                return "§cCharacter data unavailable.";
            }
            String committed =
                    RaceChangeClassMapper.commitFightingClassForRace(data, race, classId, current);
            if (committed != null && !committed.isBlank()) {
                classId = committed;
            } else {
                ch.setCharacterClass(classId);
            }
            boolean preservePrimaries = before != null;
            if (preservePrimaries) {
                applyStats(data.getStats(), before);
            }
            DmzCharacterClassChangeHooks.onPaidClassChange(
                    player, data, classId, resourceSnapshot, preservePrimaries);
            RaceClassSync.sync(player);
            CharacterServicesStore.get().record(player.m_20148_().toString()).lastClassChangeAt =
                    System.currentTimeMillis();
            CharacterServicesStore.get().markDirty();
            audit(player, "Class Change", current, classId, 100, cost, true);
            String paid = cost > 0L ? " §7Paid §f" + formatCost(cost) + "§7." : "";
            String label = FightingClassLabels.display(classId);
            if (label == null || label.isBlank() || "Not set".equals(label)) {
                label = titleCase(classId);
            }
            return "§aClass changed to §f" + label + "§a." + paid;
        } catch (Throwable t) {
            refund(player, cost);
            audit(player, "Class Change", current, classId, 100, cost, false);
            return "§cThe character change could not be completed. Payment was refunded when possible.";
        }
    }

    /**
     * Charges Ancient Coins (level-scaled), then opens DMZ recustomize UI only — no full mod menu.
     */
    public static String executeReskin(ServerPlayer player) {
        String block = blockReason(player, true);
        if (!block.isEmpty()) {
            return block;
        }
        if (!CharacterServicesAccess.canReskin(player)) {
            return "§cYou cannot reskin.";
        }
        CharacterServicesConfig.Reskin rs = CharacterServicesConfig.get().reskin;
        if (!rs.enabled) {
            return "§cReskin is disabled.";
        }
        if (!CharacterServicesAccess.bypassCooldown(player)) {
            long left = cooldownRemaining(player, "reskin");
            if (left > 0L) {
                return "§cYou must wait §f" + formatDuration(left) + " §cbefore another reskin.";
            }
        }
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : reskinCost(player);
        AncientCoinEconomy.migrateWalletToItems(player);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return insufficientFunds(player, cost);
        }
        if (cost > 0L && !chargeAc(player, cost)) {
            return insufficientFunds(player, cost);
        }
        try {
            ReskinSessionGuard.begin(player);
            if (RaceHeadBoneSync.syncCharacter(player)) {
                RaceHeadBoneSync.syncClient(player);
            }
            var server = player.m_20194_();
            Runnable openEditor = () -> NetworkHandler.sendToPlayer(new OpenRecustomizeS2C(), player);
            if (server != null) {
                server.execute(openEditor);
            } else {
                openEditor.run();
            }
            CharacterServicesStore.get().record(player.m_20148_().toString()).lastReskinAt =
                    System.currentTimeMillis();
            CharacterServicesStore.get().markDirty();
            audit(player, "Reskin", "", "", 0, cost, true);
            String paid = cost > 0L ? "§7Paid §f" + formatCost(cost) + "§7. " : "";
            return "§a" + paid + "Opening the appearance editor. §7Your stats and progression are unchanged.";
        } catch (Throwable t) {
            refund(player, cost);
            audit(player, "Reskin", "", "", 0, cost, false);
            return "§cCould not open the appearance editor. Payment was refunded when possible.";
        }
    }

    private static void refund(ServerPlayer player, long cost) {
        if (cost > 0L) {
            AncientCoinEconomy.grantCopperExact(player, cost);
        }
    }

    private static void syncClient(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Same core wipe as {@code dmzstats reset <player> 0 false} — keeps the race already set on
     * {@link Character}, clears skills, techniques, resources, quest progress, etc.
     */
    private static void applyFullProgressWipe(ServerPlayer player, StatsData data) {
        if (player == null || data == null) {
            return;
        }
        try {
            data.resetPlayerProgress(player, 0, false, false);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] full race-change wipe failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    player.m_6302_(),
                    t.toString());
            Skills skills = data.getSkills();
            if (skills != null) {
                try {
                    skills.removeAllSkills();
                } catch (Throwable ignored) {
                }
            }
            Stats stats = data.getStats();
            if (stats != null) {
                applyStats(stats, new TransferableStats(0, 0, 0, 0, 0, 0));
            }
        }
    }

    /** Drop active form / stack form so race & class edits apply safely while transformed. */
    private static void releaseTransformation(ServerPlayer player) {
        if (player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        Character ch = data.getCharacter();
        if (ch == null) {
            return;
        }
        boolean hadForm = false;
        try {
            hadForm = ch.hasActiveForm() || ch.hasActiveStackForm();
        } catch (Throwable ignored) {
        }
        if (!hadForm) {
            return;
        }
        clearForms(ch, player);
        syncClient(player);
    }

    private static void clearForms(Character ch, ServerPlayer player) {
        try {
            ch.clearActiveForm(player, true);
        } catch (Throwable ignored) {
            try {
                ch.clearActiveForm();
            } catch (Throwable ignored2) {
            }
        }
        try {
            ch.clearActiveStackForm(player, true);
        } catch (Throwable ignored) {
            try {
                ch.clearActiveStackForm();
            } catch (Throwable ignored2) {
            }
        }
    }

    private static TransferableStats captureStats(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return null;
        }
        Stats s = data.getStats();
        if (s == null) {
            return null;
        }
        return new TransferableStats(
                s.getStrength(),
                s.getStrikePower(),
                s.getResistance(),
                s.getVitality(),
                s.getKiPower(),
                s.getEnergy()
        );
    }

    private static void applyStats(Stats stats, TransferableStats t) {
        if (stats == null || t == null) {
            return;
        }
        stats.setStrength(t.strength);
        stats.setStrikePower(t.strike);
        stats.setResistance(t.resistance);
        stats.setVitality(t.vitality);
        stats.setKiPower(t.kiPower);
        stats.setEnergy(t.energy);
    }

    private static boolean isValidPreservation(int pct) {
        for (int step : CharacterServicesConfig.preservationSteps()) {
            if (step == pct) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRaceAvailable(String raceId) {
        return DmzContentDiscovery.isKnownRace(raceId);
    }

    private static long cooldownRemaining(ServerPlayer player, String kind) {
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        long now = System.currentTimeMillis();
        long last;
        long cd;
        switch (kind) {
            case "race" -> {
                last = rec.lastRaceChangeAt;
                cd = CharacterServicesConfig.get().raceChange.cooldownMs;
            }
            case "class" -> {
                last = rec.lastClassChangeAt;
                cd = CharacterServicesConfig.get().classChange.cooldownMs;
            }
            case "reskin" -> {
                last = rec.lastReskinAt;
                cd = CharacterServicesConfig.get().reskin.cooldownMs;
            }
            default -> {
                return 0L;
            }
        }
        return Math.max(0L, last + cd - now);
    }

    private static void audit(
            ServerPlayer player,
            String action,
            String from,
            String to,
            int preservation,
            long cost,
            boolean success
    ) {
        AdaptiveDifficultyMod.LOGGER.info(
                "[CharacterServices] player={} action={} from={} to={} preservation={} cost={} success={}",
                player == null ? "?" : player.m_7755_().getString(),
                action,
                from,
                to,
                preservation,
                cost,
                success);
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("race:")) {
            s = s.substring(5).trim();
        }
        if (s.startsWith("class:")) {
            s = s.substring(6).trim();
        }
        return s;
    }

    private static String titleCase(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(java.lang.Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private static String format(int v) {
        return DmzRewards.formatWhole(v);
    }

    private static String formatDuration(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        long days = sec / 86400L;
        sec %= 86400L;
        long hours = sec / 3600L;
        sec %= 3600L;
        long minutes = sec / 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
