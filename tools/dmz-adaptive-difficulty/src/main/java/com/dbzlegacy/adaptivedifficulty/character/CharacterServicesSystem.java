package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassPermissionSync;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.RaceClassSync;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.RaceSkillSync;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.progression.classdef.FightingClassCatalog;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Stats;
import com.dragonminez.common.stats.character.Status;
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
        Character ch = data.getCharacter();
        if (!cosmetic && cfg.restrictions.blockWhileTransformed && ch != null) {
            try {
                if (ch.hasActiveForm() || ch.hasActiveStackForm()) {
                    return "§cYou cannot modify your character while transformed.";
                }
            } catch (Throwable ignored) {
            }
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
            for (String race : ConfigManager.getLoadedRaces()) {
                if (race == null || race.isBlank()) {
                    continue;
                }
                String id = race.trim().toLowerCase(Locale.ROOT);
                if (blocked.contains(id)) {
                    continue;
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
        String current = DmzProgression.fightingClass(player);
        for (String id : FightingClassCatalog.allClassIds()) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String name = FightingClassCatalog.skillNameFor(id);
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
            lines.add("§7Stats unavailable.");
            return lines;
        }
        TransferableStats kept = now.scaled(preservationPercent);
        lines.add("§7Preservation §f" + preservationPercent + "%");
        lines.add("§7STR §f" + format(now.strength) + " §8→ §f" + format(kept.strength));
        lines.add("§7SKP §f" + format(now.strike) + " §8→ §f" + format(kept.strike));
        lines.add("§7RES §f" + format(now.resistance) + " §8→ §f" + format(kept.resistance));
        lines.add("§7VIT §f" + format(now.vitality) + " §8→ §f" + format(kept.vitality));
        lines.add("§7PWR §f" + format(now.kiPower) + " §8→ §f" + format(kept.kiPower));
        lines.add("§7ENE §f" + format(now.energy) + " §8→ §f" + format(kept.energy));
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
        return scaledCost(player, base, CharacterServicesConfig.get().raceChange.levelCostMultiplier);
    }

    public static long classCost(ServerPlayer player) {
        CharacterServicesConfig.ClassChange cc = CharacterServicesConfig.get().classChange;
        return scaledCost(player, cc.baseCostCopper, cc.levelCostMultiplier);
    }

    public static long reskinCost(ServerPlayer player) {
        CharacterServicesConfig.Reskin rs = CharacterServicesConfig.get().reskin;
        return scaledCost(player, rs.baseCostCopper, rs.levelCostMultiplier);
    }

    public static String cooldownLine(ServerPlayer player, String kind) {
        if (CharacterServicesAccess.bypassCooldown(player)) {
            return "§aAvailable now §8(bypass)";
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
            return "§aAvailable now";
        }
        return "§cAvailable in §f" + formatDuration(left);
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
        if (!CharacterServicesAccess.bypassCooldown(player)) {
            long left = cooldownRemaining(player, "race");
            if (left > 0L) {
                return "§cYou must wait §f" + formatDuration(left) + " §cbefore another race change.";
            }
        }
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : raceCost(player, preservationPercent);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return "§cYou do not have enough Ancient Coins.";
        }

        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return "§cCharacter data unavailable.";
        }
        TransferableStats before = captureStats(player);
        if (before == null) {
            return "§cCould not read your stats.";
        }
        TransferableStats target = before.scaled(preservationPercent);

        if (cost > 0L && !AncientCoinEconomy.charge(player, cost)) {
            return "§cPayment failed. No changes were made.";
        }

        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                refund(player, cost);
                return "§cCharacter data unavailable.";
            }
            clearForms(ch, player);
            ch.setRace(raceId);
            applyStats(data.getStats(), target);
            RaceSkillSync.sync(player, raceId);
            RaceClassSync.sync(player);
            ClassPermissionSync.sync(player);
            syncClient(player);
            CharacterServicesStore.get().record(player.m_20148_().toString()).lastRaceChangeAt =
                    System.currentTimeMillis();
            CharacterServicesStore.get().markDirty();
            audit(player, "Race Change", currentRace, raceId, preservationPercent, cost, true);
            return "§aRace changed to §f" + titleCase(raceId)
                    + "§a. Eligible stats preserved at §f" + preservationPercent + "%§a.";
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
        if (classId.isEmpty() || !FightingClassCatalog.allClassIds().contains(classId)) {
            return "§cThat class is unavailable.";
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
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : classCost(player);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return "§cYou do not have enough Ancient Coins.";
        }

        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return "§cCharacter data unavailable.";
        }
        TransferableStats before = cc.preserveBaseStats ? captureStats(player) : null;

        if (cost > 0L && !AncientCoinEconomy.charge(player, cost)) {
            return "§cPayment failed. No changes were made.";
        }

        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                refund(player, cost);
                return "§cCharacter data unavailable.";
            }
            ch.setCharacterClass(classId);
            if (before != null) {
                applyStats(data.getStats(), before);
            }
            ClassPermissionSync.sync(player);
            RaceClassSync.sync(player);
            syncClient(player);
            CharacterServicesStore.get().record(player.m_20148_().toString()).lastClassChangeAt =
                    System.currentTimeMillis();
            CharacterServicesStore.get().markDirty();
            audit(player, "Class Change", current, classId, 100, cost, true);
            return "§aClass changed to §f" + titleCase(classId) + "§a.";
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
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return "§cYou do not have enough Ancient Coins.";
        }
        if (cost > 0L && !AncientCoinEconomy.charge(player, cost)) {
            return "§cPayment failed. No changes were made.";
        }
        try {
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
            return "§aOpening appearance editor. §7Gameplay stats are unchanged.";
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
        try {
            return ConfigManager.isRaceLoaded(raceId);
        } catch (Throwable ignored) {
            return false;
        }
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
