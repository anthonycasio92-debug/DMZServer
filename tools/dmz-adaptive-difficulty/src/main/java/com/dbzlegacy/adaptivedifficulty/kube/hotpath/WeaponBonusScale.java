package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Native port of dmz_weapon_bonus_scale.js. Multiplicative DMZ bonuses only,
 * at most two of mainhand / offhand / hotbar slot 9, and slot 9 is suppressed
 * while that hotbar slot is selected.
 */
public final class WeaponBonusScale {
    public static final int UPDATE_INTERVAL = 20;
    private static final int SLOT_9 = 8;
    private static final double UNIQUE_BASE = 1.80;
    private static final double UNIQUE_THIRD = 1.35;
    private static final String VERSION = "DMZ_WEAPON_MULTIPLIERS_V11_SLOT9_SELECTED_FIX";
    private static final String BONUS_MAIN = "DMZ Weapon Mainhand";
    private static final String BONUS_OFF = "DMZ Weapon Offhand";
    private static final String BONUS_SLOT9 = "DMZ Weapon Slot 9";
    private static final String[] CLEAR_STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};
    private static final String[] OLD_SLOTS = {"MAIN", "OFF", "SLOT9"};

    private static final Map<UUID, String> SIGNATURES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> TOOL_DEFINITION = new ConcurrentHashMap<>();
    private static final Set<Class<?>> NO_TOOL_DEFINITION = ConcurrentHashMap.newKeySet();

    private WeaponBonusScale() {}

    public static void clearPlayer(UUID uuid) {
        if (uuid != null) {
            SIGNATURES.remove(uuid);
        }
    }

    public static void update(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            updateInner(player);
        } catch (Throwable t) {
            HotLog.debug("[DMZ Weapon V11 ERROR] " + t);
        }
    }

    private static void updateInner(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        BonusStats bonus = data.getBonusStats();
        if (bonus == null) {
            return;
        }
        if (!HotPathConfig.get().weaponBonus.enabled) {
            UUID offId = player.m_20148_();
            if (!"OFF".equals(SIGNATURES.get(offId))) {
                clearWeaponBonuses(bonus);
                SIGNATURES.put(offId, "OFF");
                try {
                    NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
                } catch (Throwable t) {
                    HotLog.debug("[DMZ Weapon V11] Sync error: " + t);
                }
            }
            return;
        }

        boolean slot9Selected = player.m_150109_().f_35977_ == SLOT_9;
        ItemStack mainStack = player.m_21205_();
        ItemStack offStack = player.m_21206_();
        ItemStack slot9Stack = player.m_150109_().m_8020_(SLOT_9);

        Info main = identify(mainStack);
        Info off = identify(offStack);
        Info slot9 = identify(slot9Stack);

        Result mainResult = resultOf(mainStack, "MAIN", main);
        Result offResult = resultOf(offStack, "OFF", off);
        int hands = (mainResult == null ? 0 : 1) + (offResult == null ? 0 : 1);
        Result slot9Result = null;
        if (!slot9Selected && hands < 2 && slot9 != null && slot9.weapon && slot9.profile != null) {
            slot9Result = calculate(slot9Stack, "SLOT9", slot9);
        }

        String signature = VERSION
                + "|SLOT9SELECTED=" + slot9Selected
                + "|MAINRAW=" + raw(mainStack, main)
                + "|OFFRAW=" + raw(offStack, off)
                + "|SLOT9RAW=" + raw(slot9Stack, slot9)
                + "|MAIN=" + signed(mainResult)
                + "|OFF=" + signed(offResult)
                + "|SLOT9=" + signed(slot9Result);
        UUID id = player.m_20148_();
        if (signature.equals(SIGNATURES.get(id))) {
            return;
        }

        clearWeaponBonuses(bonus);
        apply(bonus, mainResult);
        apply(bonus, offResult);
        apply(bonus, slot9Result);
        SIGNATURES.put(id, signature);
        try {
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        } catch (Throwable t) {
            HotLog.debug("[DMZ Weapon V11] Sync error: " + t);
        }
    }

    private static Result resultOf(ItemStack stack, String slot, Info info) {
        if (info == null || info.profile == null) {
            return null;
        }
        return calculate(stack, slot, info);
    }

    private static Result calculate(ItemStack stack, String slot, Info info) {
        double damage = 1.0;
        double damageScale = 1.0;
        if (!info.shield) {
            damage = attackDamage(stack) * WeaponDamageControl.multiplier(info.id, info.family);
            if (!Double.isFinite(damage) || damage < 0.0) {
                damage = 0.0;
            }
            HotPathConfig.WeaponBonus bonus = HotPathConfig.get().weaponBonus;
            damageScale = bonus.damageFloor + (damage / bonus.damageDivisor);
        }
        double profileScale = damageScale;
        HotPathConfig.WeaponBonus bonus = HotPathConfig.get().weaponBonus;
        if (info.unique) {
            profileScale *= bonus.uniqueBase;
        }
        double[] stats = new double[6];
        for (int i = 0; i < 6; i++) {
            if (!Double.isNaN(info.profile[i])) {
                stats[i] = info.profile[i] * profileScale;
            }
        }
        if (info.unique) {
            int third = statIndex(chooseThird(info.id, info.profile));
            if (third >= 0) {
                stats[third] += bonus.thirdStat * damageScale * bonus.uniqueThird;
            }
        }
        return new Result(slot, info.id, info.family, info.definition, damage, stats);
    }

    private static String chooseThird(String weaponId, double[] profile) {
        String preferred = WeaponBonusTables.uniqueThird(weaponId);
        if (preferred == null && weaponId.startsWith("simplymore:mimicry_")) {
            preferred = "VIT";
        }
        if (preferred != null) {
            int index = statIndex(preferred);
            if (index >= 0 && Double.isNaN(profile[index])) {
                return preferred;
            }
        }
        for (String stat : WeaponBonusTables.THIRD_ORDER) {
            int index = statIndex(stat);
            if (index >= 0 && Double.isNaN(profile[index])) {
                return stat;
            }
        }
        return "VIT";
    }

    private static Info identify(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        String id = ItemIds.id(stack);
        if (id.isEmpty()) {
            return null;
        }
        if (stack.m_41720_() instanceof ShieldItem
                || (id.startsWith("tconstruct:") && path(id).toLowerCase(Locale.ROOT).contains("shield"))) {
            return new Info(id, "Shield", "shield", "", shieldProfile(stack), false, false, true);
        }
        WeaponBonusTables.WeaponDef dmz = WeaponBonusTables.dmz(id);
        if (dmz != null) {
            return new Info(id, "DragonMineZ", dmz.family, "", dmz.profile, false, true, false);
        }
        WeaponBonusTables.WeaponDef irons = WeaponBonusTables.irons(id);
        if (irons != null) {
            return new Info(id, "Iron's Spellbooks", irons.family, "", irons.profile, false, true, false);
        }
        if (id.startsWith("simplyswords:") || id.startsWith("simplymore:")) {
            String family = simplyFamily(id);
            double[] profile = WeaponBonusTables.family(family);
            if (family == null || profile == null) {
                return null;
            }
            boolean unique = WeaponBonusTables.uniqueThird(id) != null || id.startsWith("simplymore:mimicry_");
            String source = id.startsWith("simplymore:") ? "Simply More" : "Simply Swords";
            return new Info(id, source, family, "", profile, unique, true, false);
        }
        String definition = tinkersDefinition(stack);
        String tinkersFamily = definition.isEmpty() ? null : normalizeTinkers(definition);
        if (tinkersFamily == null && id.startsWith("tconstruct:")) {
            tinkersFamily = normalizeTinkers(id);
        }
        double[] tinkersProfile = WeaponBonusTables.tinkers(tinkersFamily);
        if (tinkersFamily != null && tinkersProfile != null) {
            return new Info(id, "Tinkers", tinkersFamily, definition, tinkersProfile, false, true, false);
        }
        if (!definition.isEmpty()) {
            return new Info(id, "Tinkers", "UNMATCHED", definition, null, false, true, false);
        }
        return null;
    }

    private static String simplyFamily(String id) {
        String exact = WeaponBonusTables.simplyExact(id);
        if (exact != null) {
            return exact;
        }
        String itemPath = path(id);
        for (String[] suffix : WeaponBonusTables.SIMPLY_SUFFIXES) {
            if (itemPath.endsWith(suffix[0])) {
                return suffix[1];
            }
        }
        return null;
    }

    private static String normalizeTinkers(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String itemPath = path(value).toLowerCase(Locale.ROOT);
        if (WeaponBonusTables.tinkers(itemPath) != null) {
            return itemPath;
        }
        if (itemPath.contains("scythe")) return "scythe";
        if (itemPath.contains("cleaver")) return "cleaver";
        if (itemPath.contains("broad_axe") || itemPath.contains("broadaxe")) return "broad_axe";
        if (itemPath.contains("hand_axe") || itemPath.contains("handaxe")) return "hand_axe";
        if (itemPath.contains("sledge_hammer") || itemPath.contains("sledgehammer")) return "sledge_hammer";
        if (itemPath.contains("vein_hammer") || itemPath.contains("veinhammer")) return "vein_hammer";
        if (itemPath.contains("war_pick") || itemPath.contains("warpick")) return "war_pick";
        if (itemPath.contains("mattock")) return "mattock";
        if (itemPath.contains("pickadze")) return "pickadze";
        if (itemPath.contains("excavator")) return "excavator";
        if (itemPath.contains("battlesign") || itemPath.contains("battle_sign")) return "battlesign";
        if (itemPath.contains("swasher")) return "swasher";
        if (itemPath.contains("minotaur_axe")) return "minotaur_axe";
        if (itemPath.contains("melting_pan") || itemPath.contains("meltingpan")) return "melting_pan";
        if (itemPath.contains("dagger")) return "dagger";
        if (itemPath.contains("kama")) return "kama";
        if (itemPath.contains("javelin")) return "javelin";
        if (itemPath.contains("shuriken")) return "shuriken";
        if (itemPath.contains("throwing_axe")) return "throwing_axe";
        if (itemPath.contains("crossbow")) return "crossbow";
        if (itemPath.contains("longbow")) return "longbow";
        if (itemPath.contains("sky_staff")) return "sky_staff";
        if (itemPath.contains("earth_staff")) return "earth_staff";
        if (itemPath.contains("ichor_staff")) return "ichor_staff";
        if (itemPath.contains("ender_staff")) return "ender_staff";
        if ("sword".equals(itemPath) || itemPath.contains("_sword") || itemPath.contains("sword_")) {
            return "sword";
        }
        return null;
    }

    private static double[] shieldProfile(ItemStack stack) {
        double durability = 336.0;
        try {
            int max = stack.m_41776_();
            if (max > 0) {
                durability = max;
            }
        } catch (Throwable ignored) {
        }
        double scale = durability / 336.0;
        double[] profile = new double[] {Double.NaN, Double.NaN, 0.10 * scale, 0.03 * scale, Double.NaN, Double.NaN};
        return profile;
    }

    private static double attackDamage(ItemStack stack) {
        try {
            com.google.common.collect.Multimap<Attribute, AttributeModifier> map =
                    stack.m_41638_(EquipmentSlot.MAINHAND);
            if (map == null) {
                return 1.0;
            }
            Collection<AttributeModifier> modifiers = map.get(Attributes.f_22281_);
            if (modifiers == null || modifiers.isEmpty()) {
                return 1.0;
            }
            double addition = 0.0;
            double multiplyBase = 0.0;
            double multiplyTotal = 1.0;
            for (AttributeModifier modifier : modifiers) {
                double amount = modifier.m_22218_();
                AttributeModifier.Operation operation = modifier.m_22217_();
                if (operation == AttributeModifier.Operation.ADDITION) {
                    addition += amount;
                } else if (operation == AttributeModifier.Operation.MULTIPLY_BASE) {
                    multiplyBase += amount;
                } else if (operation == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    multiplyTotal *= (1.0 + amount);
                }
            }
            double result = (1.0 + addition) * (1.0 + multiplyBase) * multiplyTotal;
            if (Double.isFinite(result) && result > 0.0) {
                return result;
            }
        } catch (Throwable ignored) {
        }
        return 1.0;
    }

    private static String tinkersDefinition(ItemStack stack) {
        Method method = toolDefinition(stack.m_41720_().getClass());
        if (method == null) {
            return "";
        }
        try {
            Object definition = method.invoke(stack.m_41720_());
            if (definition == null) {
                return "";
            }
            Object id = definition.getClass().getMethod("getId").invoke(definition);
            return id == null ? "" : String.valueOf(id);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static Method toolDefinition(Class<?> type) {
        Method cached = TOOL_DEFINITION.get(type);
        if (cached != null || NO_TOOL_DEFINITION.contains(type)) {
            return cached;
        }
        try {
            Method method = type.getMethod("getToolDefinition");
            TOOL_DEFINITION.put(type, method);
            return method;
        } catch (Throwable ignored) {
            NO_TOOL_DEFINITION.add(type);
            return null;
        }
    }

    private static void clearWeaponBonuses(BonusStats bonus) {
        removeName(bonus, BONUS_MAIN);
        removeName(bonus, BONUS_OFF);
        removeName(bonus, BONUS_SLOT9);
        for (String slot : OLD_SLOTS) {
            for (String stat : WeaponBonusTables.STATS) {
                removeName(bonus, "DMZLegacyCNPCWeapon_" + slot + "_" + stat);
                removeName(bonus, "DMZLegacyWeapon_" + slot + "_" + stat);
            }
        }
        removeName(bonus, "DMZLegacyWeapon_STR");
        removeName(bonus, "DMZLegacyWeapon_SKP");
        removeName(bonus, "DMZLegacyWeapon_RES");
        removeName(bonus, "DMZLegacyWeapon_VIT");
        removeName(bonus, "DMZLegacyWeapon_PWR");
        removeName(bonus, "DMZLegacyWeapon_ENE");
    }

    private static void removeName(BonusStats bonus, String name) {
        for (String stat : CLEAR_STATS) {
            try {
                bonus.removeBonus(stat, name);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void apply(BonusStats bonus, Result result) {
        if (result == null) {
            return;
        }
        for (int i = 0; i < WeaponBonusTables.STATS.length; i++) {
            double percent = result.stats[i];
            if (!Double.isFinite(percent) || percent <= 0.0) {
                continue;
            }
            String stat = WeaponBonusTables.STATS[i];
            String name = bonusName(result.slot);
            double factor = 1.0 + percent;
            try {
                if ("RES".equals(stat)) {
                    bonus.addBonusSplit("RES", name, "*", factor, false);
                } else {
                    bonus.addBonus(stat, name, "*", factor, false);
                }
            } catch (Throwable t) {
                HotLog.debug("[DMZ Weapon V11] apply failed: " + t);
            }
        }
    }

    private static String bonusName(String slot) {
        if ("MAIN".equals(slot)) {
            return BONUS_MAIN;
        }
        if ("OFF".equals(slot)) {
            return BONUS_OFF;
        }
        return BONUS_SLOT9;
    }

    private static String raw(ItemStack stack, Info info) {
        String id = ItemIds.id(stack);
        if (id.isEmpty()) {
            return "EMPTY";
        }
        if (info == null) {
            return id + ":NONE";
        }
        return id + ":" + info.family + ":" + info.definition;
    }

    private static String signed(Result result) {
        if (result == null) {
            return "NONE";
        }
        return result.slot + ":" + result.id + ":" + result.family + ":" + result.definition + ":"
                + String.format(Locale.US, "%.4f", result.damage) + ":"
                + String.format(Locale.US, "%.6f", result.stats[0]) + ":"
                + String.format(Locale.US, "%.6f", result.stats[1]) + ":"
                + String.format(Locale.US, "%.6f", result.stats[2]) + ":"
                + String.format(Locale.US, "%.6f", result.stats[3]) + ":"
                + String.format(Locale.US, "%.6f", result.stats[4]) + ":"
                + String.format(Locale.US, "%.6f", result.stats[5]);
    }

    private static int statIndex(String stat) {
        if (stat == null) {
            return -1;
        }
        for (int i = 0; i < WeaponBonusTables.STATS.length; i++) {
            if (WeaponBonusTables.STATS[i].equals(stat)) {
                return i;
            }
        }
        return -1;
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static final class Info {
        final String id;
        final String family;
        final String definition;
        final double[] profile;
        final boolean unique;
        final boolean weapon;
        final boolean shield;

        Info(String id, String source, String family, String definition, double[] profile,
                boolean unique, boolean weapon, boolean shield) {
            this.id = id;
            this.family = family;
            this.definition = definition;
            this.profile = profile;
            this.unique = unique;
            this.weapon = weapon;
            this.shield = shield;
        }
    }

    private static final class Result {
        final String slot;
        final String id;
        final String family;
        final String definition;
        final double damage;
        final double[] stats;

        Result(String slot, String id, String family, String definition, double damage, double[] stats) {
            this.slot = slot;
            this.id = id;
            this.family = family;
            this.definition = definition;
            this.damage = damage;
            this.stats = stats;
        }
    }
}
