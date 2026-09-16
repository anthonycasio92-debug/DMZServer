package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesAccess;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Bukkit GUI bridge for Character Services ({@code /character} / hub). */
public final class CharacterServicesGuiApi {
    private CharacterServicesGuiApi() {}

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        if (player == null) {
            return out;
        }
        CharacterServicesConfig cfg = CharacterServicesConfig.get();
        out.put("bridge_ok", "true");
        out.put("enabled", cfg.enabled ? "true" : "false");
        out.put("race_enabled", cfg.raceChange.enabled ? "true" : "false");
        out.put("class_enabled", cfg.classChange.enabled ? "true" : "false");
        out.put("reskin_enabled", cfg.reskin.enabled ? "true" : "false");
        out.put("current_race", title(DmzProgression.race(player)));
        out.put("current_class", title(DmzProgression.fightingClass(player)));
        out.put("ancient_coins", String.valueOf(AncientCoinEconomy.balance(player)));
        out.put("race_cooldown", CharacterServicesSystem.cooldownLine(player, "race"));
        out.put("class_cooldown", CharacterServicesSystem.cooldownLine(player, "class"));
        out.put("reskin_cooldown", CharacterServicesSystem.cooldownLine(player, "reskin"));
        out.put("can_services", CharacterServicesAccess.canUseServices(player) ? "true" : "false");
        return out;
    }

    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        if (!CharacterServicesConfig.get().enabled) {
            lines.add("§cCharacter Services are disabled.");
            return lines;
        }
        if ("race".equals(p)) {
            lines.add("§7Pick a new race, then preservation %.");
            lines.add("§8Race forms/skills recalculate for the new race.");
            lines.add(CharacterServicesSystem.cooldownLine(player, "race"));
            return lines;
        }
        if ("class".equals(p)) {
            lines.add("§7Change fighting class — base stats stay.");
            lines.add("§8Class skills & progression recalculate.");
            lines.add(CharacterServicesSystem.cooldownLine(player, "class"));
            return lines;
        }
        if ("reskin".equals(p)) {
            lines.add("§7Appearance only — §fno stat changes§7.");
            lines.add("§8Opens DMZ customize screen.");
            lines.add(CharacterServicesSystem.cooldownLine(player, "reskin"));
            return lines;
        }
        if (p.startsWith("race_pct:")) {
            String[] bits = p.split(":", 3);
            int pct = parsePct(bits.length > 2 ? bits[2] : "100");
            lines.addAll(CharacterServicesSystem.statPreviewLines(player, pct));
            long cost = CharacterServicesSystem.raceCost(player, pct);
            lines.add("§7Cost §f" + DmzRewards.formatWhole(cost) + " §7AC §8(level-scaled)");
            return lines;
        }
        if (p.startsWith("race_confirm:")) {
            String[] bits = p.split(":", 3);
            int pct = parsePct(bits.length > 2 ? bits[2] : "100");
            String race = bits.length > 1 ? bits[1] : "?";
            lines.add("§7New race §f" + title(race));
            lines.add("§7Preservation §f" + pct + "%");
            lines.addAll(CharacterServicesSystem.statPreviewLines(player, pct));
            long cost = CharacterServicesSystem.raceCost(player, pct);
            lines.add("§7Cost §f" + DmzRewards.formatWhole(cost) + " §7AC");
            lines.add("§cCannot be undone automatically.");
            return lines;
        }
        if (p.startsWith("class_confirm:")) {
            String cls = p.substring("class_confirm:".length());
            lines.add("§7New class §f" + title(cls));
            lines.add("§7Cost §f" + DmzRewards.formatWhole(CharacterServicesSystem.classCost(player)) + " §7AC");
            return lines;
        }
        lines.add("§7Evolve your character without a full wipe.");
        lines.add("§8Race · Class · Reskin");
        return lines;
    }

    public static List<String> raceCards(ServerPlayer player) {
        return CharacterServicesSystem.raceCards(player);
    }

    public static List<String> classCards(ServerPlayer player) {
        return CharacterServicesSystem.classCards(player);
    }

    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("race_confirm".equals(act)) {
            String[] bits = a.split(":", 2);
            if (bits.length < 2) {
                return "§cInvalid race confirm.";
            }
            return CharacterServicesSystem.executeRaceChange(player, bits[0], parsePct(bits[1]));
        }
        if ("class_confirm".equals(act)) {
            if (a.isBlank()) {
                return "§cPick a class.";
            }
            return CharacterServicesSystem.executeClassChange(player, a);
        }
        if ("reskin_confirm".equals(act)) {
            return CharacterServicesSystem.executeReskin(player);
        }
        return "§cUnknown character action: " + act;
    }

    private static int parsePct(String raw) {
        try {
            return Integer.parseInt(raw == null ? "100" : raw.trim());
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    private static String title(String id) {
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
}
