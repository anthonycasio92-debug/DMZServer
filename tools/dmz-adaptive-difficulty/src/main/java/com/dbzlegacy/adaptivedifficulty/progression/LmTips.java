package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.ScreenNotify;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Periodic on-screen Legacy Mechanics tips so new (and returning) players learn {@code /lm}.
 * Uses title/subtitle only — no chat spam.
 */
public final class LmTips {
    private static final String KEY_NEXT_AT = "lm_tip_next_at_ms";
    private static final String KEY_INDEX = "lm_tip_index";
    private static final String CD_KEY = "lm_tip_screen";

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
     * Newer players (low DMZ level, no prestiges completed) see tips more often.
     * Veterans keep a gentler cadence so it stays useful without nagging.
     */
    private static long intervalMs(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int baseSec = Math.max(60, cfg.lmTipIntervalSeconds);
        int newSec = Math.max(45, cfg.lmTipNewPlayerIntervalSeconds);
        if (isNewPlayer(player)) {
            return newSec * 1000L;
        }
        return baseSec * 1000L;
    }

    private static boolean isNewPlayer(ServerPlayer player) {
        try {
            int level = com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.dmzLevelForProgression(player);
            if (level < Math.max(1, DifficultyConfig.get().lmTipNewPlayerMaxLevel)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            int completed = com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem
                    .getCompleted(player);
            return completed <= 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean enabled() {
        try {
            return DifficultyConfig.get().enableLmTips;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
