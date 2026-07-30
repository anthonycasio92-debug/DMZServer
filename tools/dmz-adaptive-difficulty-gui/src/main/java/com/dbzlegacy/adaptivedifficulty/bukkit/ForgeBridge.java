package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;

/** Reflects into the Forge mod for live difficulty values (Mohist shared JVM). */
public final class ForgeBridge {
    private ForgeBridge() {}

    public static Object nmsPlayer(Player player) {
        try {
            return player.getClass().getMethod("getHandle").invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Map<String, String> placeholders(Player player) {
        Map<String, String> out = new HashMap<>();
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return out;
        }
        try {
            Class<?> sp = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> cache = Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache");
            Object snap = cache.getMethod("refresh", sp).invoke(null, nms);
            if (snap == null) {
                return out;
            }

            long active = longField(snap, "active");
            long available = longField(snap, "availableMax");
            long calculated = longField(snap, "calculated");
            long purchased = longField(snap, "purchased");
            long personalMax = longField(snap, "personalMax");
            long teamBonus = longField(snap, "teamThresholdBonus");
            long teamContrib = longField(snap, "teamContribution");
            int level = intField(snap, "dmzLevel");
            int prestige = intField(snap, "prestige");
            Object teamMode = field(snap, "teamMode");

            out.put("active", String.valueOf(active));
            out.put("available", String.valueOf(available));
            out.put("available_max", String.valueOf(available));
            out.put("calculated", String.valueOf(calculated));
            out.put("purchased", String.valueOf(purchased));
            out.put("personal_max", String.valueOf(personalMax));
            out.put("team_bonus", String.valueOf(teamBonus));
            out.put("team_contrib", String.valueOf(teamContrib));
            out.put("team_contribution", String.valueOf(teamContrib));
            out.put("level", String.valueOf(level));
            out.put("prestige", String.valueOf(prestige));
            out.put("team_mode", teamMode == null ? "?" : String.valueOf(teamMode));

            Class<?> tierCls = Class.forName("com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier");
            Object tier = tierCls.getMethod("of", long.class).invoke(null, active);
            out.put("tier", String.valueOf(field(tier, "display")));

            Class<?> team = Class.forName("com.dbzlegacy.adaptivedifficulty.team.TeamScaling");
            out.put("team_name", String.valueOf(team.getMethod("teamName", sp).invoke(null, nms)));
            out.put("team_source", String.valueOf(team.getMethod("teamSourceLabel").invoke(null)));
            Object mates = team.getMethod("teammates", sp).invoke(null, nms);
            out.put("team_size", mates instanceof List<?> list ? String.valueOf(list.size()) : "0");

            Class<?> currency = Class.forName("com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge");
            out.put("balance", String.valueOf(currency.getMethod("balanceText", sp).invoke(null, nms)));
            out.put("currency", String.valueOf(currency.getMethod("currencyLabel").invoke(null)));

            Class<?> calc = Class.forName("com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator");
            long c100 = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 100L);
            long c1k = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 1000L);
            long c10k = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 10000L);
            Method format = currency.getMethod("formatCost", long.class);
            out.put("cost_100", String.valueOf(format.invoke(null, c100)));
            out.put("cost_1000", String.valueOf(format.invoke(null, c1k)));
            out.put("cost_10000", String.valueOf(format.invoke(null, c10k)));
            double mult = (Double) calc.getMethod("rewardMultiplier", long.class).invoke(null, active);
            out.put("reward_mult", String.format(Locale.US, "%.2f", mult));
        } catch (Throwable ignored) {
            // Forge mod not loaded / Mohist bridge unavailable
        }
        return out;
    }

    public static String placeholder(Player player, String id) {
        if (id == null) {
            return "";
        }
        Map<String, String> map = placeholders(player);
        return map.getOrDefault(id.toLowerCase(Locale.ROOT), "");
    }

    private static Object field(Object obj, String name) throws Exception {
        Field f = obj.getClass().getField(name);
        return f.get(obj);
    }

    private static long longField(Object obj, String name) throws Exception {
        return ((Number) field(obj, name)).longValue();
    }

    private static int intField(Object obj, String name) throws Exception {
        return ((Number) field(obj, name)).intValue();
    }
}
