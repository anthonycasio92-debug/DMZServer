package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;

/**
 * Registers Character Services Bukkit permission defaults when {@code LegacyMechanicsGUI} is not
 * loaded (Forge-only / Mohist). Matches {@code plugin.yml} defaults from the GUI companion.
 */
public final class CharacterServicesPermissionBootstrap {
    private CharacterServicesPermissionBootstrap() {}

    public static void register() {
        CharacterServicesConfig cfg = CharacterServicesConfig.get();
        if (cfg.permissions == null) {
            return;
        }
        registerNode(cfg.permissions.services, true, "Open Character Services from /lm");
        registerNode(cfg.permissions.race, true, "Use race change");
        registerNode(cfg.permissions.classChange, true, "Use class change");
        registerNode(cfg.permissions.reskin, true, "Use reskin (DMZ recustomize)");
        registerNode(cfg.permissions.headBones, true, "Use Head Parts Shop");
        registerNode(cfg.permissions.bypassCost, false, "Skip Ancient Coin charges for character services");
        registerNode(cfg.permissions.bypassCooldown, false, "Skip character service cooldowns");
        registerNode(cfg.permissions.admin, false, "Full character services admin bypass");
    }

    private static void registerNode(String node, boolean defaultAllow, String description) {
        if (node == null || node.isBlank()) {
            return;
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pm = bukkit.getMethod("getPluginManager").invoke(null);
            Object existing = pm.getClass().getMethod("getPermission", String.class).invoke(pm, node);
            if (existing != null) {
                return;
            }
            Class<?> permClass = Class.forName("org.bukkit.permissions.Permission");
            Class<?> defClass = Class.forName("org.bukkit.permissions.PermissionDefault");
            Object def = defClass.getField(defaultAllow ? "TRUE" : "OP").get(null);
            Object perm = permClass
                    .getConstructor(String.class, String.class, defClass)
                    .newInstance(node, description == null ? node : description, def);
            pm.getClass().getMethod("addPermission", permClass).invoke(pm, perm);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] character permission register skipped for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    node,
                    t.toString());
        }
    }
}
