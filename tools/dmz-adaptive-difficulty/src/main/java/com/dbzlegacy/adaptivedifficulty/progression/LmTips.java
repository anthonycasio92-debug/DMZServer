package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.ScreenNotify;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Periodic on-screen Legacy Mechanics tips so newer ranks learn {@code /lm}.
 * Uses title/subtitle only — no chat spam.
 * <p>
 * Frequent cadence (default 4 min) for LuckPerms primary groups
 * {@code default}, {@code student}, {@code novice}; everyone else gets the longer interval.
 */
public final class LmTips {
    private static final String KEY_NEXT_AT = "lm_tip_next_at_ms";
    private static final String KEY_INDEX = "lm_tip_index";
    private static final String KEY_FREQ_CACHE = "lm_tip_freq_cached";
    private static final String KEY_FREQ_UNTIL = "lm_tip_freq_until_ms";
    private static final String CD_KEY = "lm_tip_screen";
    private static final long GROUP_CACHE_MS = 60_000L;

    private record Tip(String title, String subtitle) {}

    private static final Tip[] TIPS = {
            new Tip("Legacy Mechanics", "Type /lm to open the hub"),
            new Tip("Difficulty", "/lm → Difficulty — unlock & activate tiers"),
            new Tip("Ancient Coins", "Scaled kills drop coins — spend them in /lm"),
            new Tip("Rival", "/lm → Rival — declare rivals & challenges"),
            new Tip("Spar", "/lm → Spar — sparring TP & mentor bonds"),
            new Tip("Prestige", "/lm → Prestige — turn-in, skills & breakthroughs"),
            new Tip("Personal Cap", "/lm → Prestige — raise your level breakthrough"),
            new Tip("Skill Check", "Talk to a Skill Check NPC — or /lm if you have access"),
            new Tip("Legacy Mechanics", "Everything lives under /lm — open it anytime"),
    };

    private LmTips() {}

    /** Schedule the first tip after login (short delay so the world finishes loading). */
    public static void onLogin(ServerPlayer player) {
        if (player == null || !enabled()) {
            return;
        }
        // Refresh group cache on login so a rank-up mid-session is picked up next pulse window.
        ProgressionData.tempRemove(player, KEY_FREQ_UNTIL);
        long delayMs = Math.max(15_000L, DifficultyConfig.get().lmTipLoginDelaySeconds * 1000L);
        ProgressionData.tempPut(player, KEY_NEXT_AT, System.currentTimeMillis() + delayMs);
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null || !enabled() || tick % 40 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            try {
                pulsePlayer(player, now);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void pulsePlayer(ServerPlayer player, long nowMs) {
        if (!player.m_6084_() || player.m_5833_()) {
            return;
        }
        long next = ProgressionData.tempGetLong(player, KEY_NEXT_AT, 0L);
        if (next <= 0L) {
            // Mid-session join without login hook — schedule from interval.
            long interval = intervalMs(player);
            ProgressionData.tempPut(player, KEY_NEXT_AT, nowMs + interval);
            return;
        }
        if (nowMs < next) {
            return;
        }
        showNext(player);
        ProgressionData.tempPut(player, KEY_NEXT_AT, nowMs + intervalMs(player));
    }

    private static void showNext(ServerPlayer player) {
        int index = Math.max(0, (int) ProgressionData.storedGetLong(player, KEY_INDEX, 0L));
        Tip tip = TIPS[index % TIPS.length];
        // Cooldown slightly under the shortest interval so ScreenNotify never double-blocks.
        ScreenNotify.hint(player, tip.title, tip.subtitle, CD_KEY, 20_000L);
        ProgressionData.storedPut(player, KEY_INDEX, (index + 1L) % TIPS.length);
    }

    /**
     * LuckPerms primary {@code default} / {@code student} / {@code novice} → frequent tips.
     * Higher primary ranks keep the gentler cadence.
     */
    private static long intervalMs(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int baseSec = Math.max(60, cfg.lmTipIntervalSeconds);
        int frequentSec = Math.max(45, cfg.lmTipNewPlayerIntervalSeconds);
        if (usesFrequentTips(player)) {
            return frequentSec * 1000L;
        }
        return baseSec * 1000L;
    }

    private static boolean usesFrequentTips(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long until = ProgressionData.tempGetLong(player, KEY_FREQ_UNTIL, 0L);
        if (until > now) {
            return "1".equals(ProgressionData.tempGet(player, KEY_FREQ_CACHE, "0"));
        }
        boolean frequent = resolveFrequentGroup(player);
        ProgressionData.tempPut(player, KEY_FREQ_CACHE, frequent ? "1" : "0");
        ProgressionData.tempPut(player, KEY_FREQ_UNTIL, now + GROUP_CACHE_MS);
        return frequent;
    }

    private static boolean resolveFrequentGroup(ServerPlayer player) {
        List<String> groups = DifficultyConfig.get().lmTipFrequentGroups;
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        String primary = luckPermsPrimaryGroup(player);
        if (primary != null && !primary.isBlank()) {
            String p = primary.toLowerCase(Locale.ROOT).trim();
            for (String g : groups) {
                if (g != null && p.equals(g.toLowerCase(Locale.ROOT).trim())) {
                    return true;
                }
            }
            return false;
        }
        // LP missing / user not loaded — fall back to low level so tips still help newcomers.
        try {
            int level = com.dbzlegacy.adaptivedifficulty.calc.DmzProgression
                    .dmzLevelForProgression(player);
            return level < Math.max(1, DifficultyConfig.get().lmTipNewPlayerMaxLevel);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** LuckPerms primary group via API reflection; null if unavailable. */
    private static String luckPermsPrimaryGroup(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = provider.getMethod("get").invoke(null);
            if (lp == null) {
                return null;
            }
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            UUID id = player.m_20148_();
            Object user = userManager.getClass()
                    .getMethod("getUser", UUID.class)
                    .invoke(userManager, id);
            if (user == null) {
                return null;
            }
            Object primary = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return primary == null ? null : primary.toString();
        } catch (Throwable ignored) {
        }
        // Soft fallback: Bukkit hasPermission("group.<name>") when LP API user isn't loaded.
        try {
            Object bukkit = player.getClass().getMethod("getBukkitEntity").invoke(player);
            if (bukkit == null) {
                return null;
            }
            List<String> groups = DifficultyConfig.get().lmTipFrequentGroups;
            if (groups == null) {
                return null;
            }
            for (String g : groups) {
                if (g == null || g.isBlank()) {
                    continue;
                }
                String node = "group." + g.toLowerCase(Locale.ROOT).trim();
                Object has = bukkit.getClass().getMethod("hasPermission", String.class)
                        .invoke(bukkit, node);
                if (has instanceof Boolean b && b) {
                    return g.trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean enabled() {
        try {
            return DifficultyConfig.get().enableLmTips;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
