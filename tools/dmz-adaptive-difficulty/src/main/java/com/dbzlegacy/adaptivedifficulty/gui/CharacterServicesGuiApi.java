package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesAccess;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.FightingClassLabels;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
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
        out.put("head_bone_shop_enabled", cfg.headBoneShop.enabled ? "true" : "false");
        out.put("active_head_bone", titleBone(CosmeticHeadBoneService.activeBone(player)));
        out.put("current_race", title(DmzProgression.race(player)));
        out.put("current_class", FightingClassLabels.display(resolveFightingClassRaw(player)));
        putWalletPlaceholders(player, out);
        boolean bypassCost = CharacterServicesAccess.bypassCost(player);
        out.put("bypass_cost", bypassCost ? "true" : "false");
        out.put("reskin_cost", bypassCost ? "free" : CharacterServicesSystem.formatCost(CharacterServicesSystem.reskinCost(player)));
        out.put("class_cost", bypassCost ? "free" : CharacterServicesSystem.formatCost(CharacterServicesSystem.classCost(player)));
        out.put("race_cost_100", bypassCost ? "free" : CharacterServicesSystem.formatCost(CharacterServicesSystem.raceCost(player, 100)));
        out.put("race_cooldown", CharacterServicesSystem.cooldownLine(player, "race"));
        out.put("class_cooldown", CharacterServicesSystem.cooldownLine(player, "class"));
        out.put("reskin_cooldown", CharacterServicesSystem.cooldownLine(player, "reskin"));
        out.put("can_services", cfg.enabled && CharacterServicesAccess.canUseServices(player) ? "true" : "false");
        out.put("can_race_change",
                cfg.enabled && cfg.raceChange.enabled && CharacterServicesAccess.canRaceChange(player)
                        ? "true"
                        : "false");
        out.put("can_class_change",
                cfg.enabled && cfg.classChange.enabled && CharacterServicesAccess.canClassChange(player)
                        ? "true"
                        : "false");
        out.put("can_reskin",
                cfg.enabled && cfg.reskin.enabled && CharacterServicesAccess.canReskin(player) ? "true" : "false");
        out.put("can_head_bones",
                cfg.enabled && cfg.headBoneShop.enabled && CharacterServicesAccess.canHeadBoneShop(player)
                        ? "true"
                        : "false");
        LmOverhaulScaledCombat.putPlaceholders(out, player);
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
            if (!CharacterServicesConfig.get().raceChange.enabled) {
                lines.add("§cRace change is turned off on this server.");
                return lines;
            }
            if (!CharacterServicesAccess.canRaceChange(player)) {
                lines.add("§cYou do not have permission for race change.");
                return lines;
            }
            lines.add("§7Choose the race you want to become.");
            lines.add("§7Use §f0% §7for a free full wipe (new race, nothing carried over).");
            lines.add("§8Prestige races need their Fabled unlock skill.");
            lines.add("§7You keep ki skills, techniques, and shared form progress.");
            lines.add("§8Race-only form ladders reset when the new race does not use them.");
            lines.add(CharacterServicesSystem.cooldownLine(player, "race"));
            return lines;
        }
        if ("class".equals(p)) {
            if (!CharacterServicesConfig.get().classChange.enabled) {
                lines.add("§cClass change is turned off on this server.");
                return lines;
            }
            if (!CharacterServicesAccess.canClassChange(player)) {
                lines.add("§cYou do not have permission for class change.");
                return lines;
            }
            lines.add("§7Pick a new fighting class for your §fcurrent race§7.");
            lines.add("§7Only classes from your race's DMZ stats are listed.");
            lines.add("§7Your base combat stats stay — class skills and perks reset.");
            lines.add(costLine(player, CharacterServicesSystem.classCost(player)));
            lines.add(CharacterServicesSystem.cooldownLine(player, "class"));
            return lines;
        }
        if (p.startsWith("bones:") || "bones".equals(p)) {
            if (!CharacterServicesConfig.get().headBoneShop.enabled) {
                lines.add("§cHead Parts Shop is turned off on this server.");
                return lines;
            }
            if (!CharacterServicesAccess.canHeadBoneShop(player)) {
                lines.add("§cYou do not have permission for the Head Parts Shop.");
                return lines;
            }
            int bonePage = parseBonePage(p);
            lines.add("§7Unlock parts from any race · yours are free, others cost coins.");
            lines.add("§7Click a row to unlock or equip · search filters the list · drag the scrollbar.");
            return lines;
        }
        if ("reskin".equals(p)) {
            if (!CharacterServicesConfig.get().reskin.enabled) {
                lines.add("§cReskin is turned off on this server.");
                return lines;
            }
            if (!CharacterServicesAccess.canReskin(player)) {
                lines.add("§cYou do not have permission for reskin.");
                return lines;
            }
            lines.add("§7Change hair, colors, and other cosmetics.");
            lines.add("§7Use §fHead Parts Shop §7here for cross-race ears, horns, and more.");
            lines.add("§7Fighting class cannot be changed during a reskin.");
            lines.add("§7Level, stats, and race are unchanged.");
            lines.add(costLine(player, CharacterServicesSystem.reskinCost(player)));
            lines.add("§8Pay-up OK · change returned");
            lines.add(CharacterServicesSystem.cooldownLine(player, "reskin"));
            return lines;
        }
        if (p.startsWith("race_pct:")) {
            String[] bits = p.split(":", 3);
            int pct = parsePct(bits.length > 2 ? bits[2] : "0");
            lines.addAll(CharacterServicesSystem.statPreviewLines(player, pct));
            lines.add(costLine(player, CharacterServicesSystem.raceCost(player, pct)));
            if (pct > 0) {
                lines.add("§8Scales with your DMZ level");
            } else {
                lines.add("§8Free — full wipe, then DMZ class & look setup");
            }
            return lines;
        }
        if (p.startsWith("race_confirm:")) {
            String[] bits = p.split(":", 3);
            int pct = parsePct(bits.length > 2 ? bits[2] : "0");
            String race = bits.length > 1 ? bits[1] : "?";
            lines.add("§7You are becoming §f" + title(race) + "§7.");
            lines.add("§7Head-part unlocks carry over; same class kept when the new race has it.");
            lines.add("§7If not, the editor opens so you can pick a class at no extra cost.");
            lines.add("§7Keeping §f" + pct + "% §7of eligible stats:");
            lines.addAll(CharacterServicesSystem.statPreviewLines(player, pct));
            lines.add(costLine(player, CharacterServicesSystem.raceCost(player, pct)));
            lines.add("§8Pay-up OK · change returned");
            lines.add("§cStaff cannot auto-revert this for you.");
            return lines;
        }
        if (p.startsWith("class_confirm:")) {
            String cls = p.substring("class_confirm:".length());
            lines.add("§7New class: §f" + FightingClassLabels.display(cls));
            lines.add(costLine(player, CharacterServicesSystem.classCost(player)));
            lines.add("§8Pay-up OK · change returned");
            lines.add("§8Class progression and class skills will reset.");
            return lines;
        }
        lines.add("§7Rebuild your character without starting from zero.");
        lines.add("§7Race · class · reskin · head parts (under Reskin).");
        return lines;
    }

    public static List<String> raceCards(ServerPlayer player) {
        return CharacterServicesSystem.raceCards(player);
    }

    public static List<String> classCards(ServerPlayer player) {
        return CharacterServicesSystem.classCards(player);
    }

    public static List<String> headBoneCards(ServerPlayer player, int page) {
        return CosmeticHeadBoneService.cards(player, page);
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
        if ("bone_unlock".equals(act)) {
            return CosmeticHeadBoneService.executeUnlock(player, a);
        }
        if ("bone_equip".equals(act)) {
            return CosmeticHeadBoneService.executeEquip(player, a);
        }
        if ("bone_race_default".equals(act)) {
            return CosmeticHeadBoneService.executeRaceDefaultHeadBone(player);
        }
        if ("bone_unequip".equals(act)) {
            return CosmeticHeadBoneService.executeUnequipHeadBone(player);
        }
        return "§cUnknown character action: " + act;
    }

    private static void putWalletPlaceholders(ServerPlayer player, Map<String, String> out) {
        AncientCoinEconomy.migrateWalletToItems(player);
        out.put("ancient_coins", String.valueOf(AncientCoinEconomy.balance(player)));
        out.put("coins_copper", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.COPPER)));
        out.put("coins_iron", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.IRON)));
        out.put("coins_gold", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.GOLD)));
        out.put("coins_emerald", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.EMERALD)));
        out.put("coins_diamond", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.DIAMOND)));
        out.put("coins_netherite", String.valueOf(AncientCoinEconomy.countOf(player, AncientCoinEconomy.CoinKind.NETHERITE)));
    }

    private static String costLine(ServerPlayer player, long copperCost) {
        if (copperCost <= 0L) {
            return "§7Cost §afree";
        }
        if (CharacterServicesAccess.bypassCost(player)) {
            if (DifficultyConfig.get().staffFreeAncientCoinCosts && StaffAccess.isStaff(player)) {
                return "§7Cost §afree §8(staff free coins ON)";
            }
            return "§7Cost §afree §8(bypass permission)";
        }
        return "§7Cost §f" + CharacterServicesSystem.formatCost(copperCost) + " §7Ancient Coins";
    }

    private static int parseBonePage(String page) {
        if (page == null || page.isBlank() || "bones".equals(page)) {
            return 0;
        }
        if (page.startsWith("bones:")) {
            try {
                return Integer.parseInt(page.substring("bones:".length()).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String titleBone(String id) {
        if (id == null || id.isBlank()) {
            return "none";
        }
        var entry = com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneCatalog.get(id);
        return entry == null ? id : entry.displayName();
    }

    private static int parsePct(String raw) {
        try {
            return Integer.parseInt(raw == null ? "0" : raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String title(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String labeled = FightingClassLabels.display(id);
        return "Not set".equals(labeled) ? "?" : labeled;
    }

    private static String resolveFightingClassRaw(ServerPlayer player) {
        String cls = DmzProgression.fightingClass(player);
        if (cls != null && !cls.isBlank()) {
            return cls;
        }
        try {
            var ch = DmzProgression.character(player);
            if (ch != null) {
                String raw = ch.getCharacterClass();
                if (raw != null && !raw.isBlank()) {
                    return raw;
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }
}
