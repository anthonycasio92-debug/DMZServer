package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI: {@code %dmzdiff_<id>%}.
 * Tier-centric surface + titles + per-type Ancient Coin counts.
 */
public final class DmzDiffExpansion extends PlaceholderExpansion {
    private final AdaptiveDifficultyGuiPlugin plugin;

    public DmzDiffExpansion(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "dmzdiff";
    }

    @Override
    public String getAuthor() {
        return "DBZ Legacy Reborn";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        // Removed player-facing difficulty-point / old reward placeholders.
        if (key.startsWith("cost_up_") || key.equals("cost_max")
                || key.startsWith("cost_buy_") || key.equals("raise_room")
                || key.equals("reward_mult") || key.equals("kill_tp")) {
            return "";
        }
        String value = ForgeBridge.placeholder(player, key);
        return value == null ? "" : value;
    }
}
