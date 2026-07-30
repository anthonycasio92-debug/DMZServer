package com.dbzlegacy.adaptivedifficulty.bukkit;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/** PlaceholderAPI: {@code %dmzdiff_<id>%} for CMI lore / scoreboards. */
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
        if (player == null) {
            return "";
        }
        String value = ForgeBridge.placeholder(player, params);
        return value == null ? "" : value;
    }
}
