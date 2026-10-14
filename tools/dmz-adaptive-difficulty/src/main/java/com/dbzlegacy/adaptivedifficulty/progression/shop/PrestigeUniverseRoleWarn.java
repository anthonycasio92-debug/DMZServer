package com.dbzlegacy.adaptivedifficulty.progression.shop;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Soft warnings when prestiging would strip a universe role (God of Destruction / Supreme Kai).
 *
 * <p>Noea and KaiPath are optional at compile time — resolve via reflection, same pattern as
 * {@code HakaiDestroyerGateMixin}. A missing mod or any reflection failure returns false so
 * prestige is never blocked by a failed check.
 */
public final class PrestigeUniverseRoleWarn {
    /** Player-facing GoD warning (plain ASCII). */
    public static final String GOD_OF_DESTRUCTION =
            "You are a God of Destruction - prestiging will remove you from your universe role.";

    /** Player-facing Supreme Kai warning (plain ASCII). */
    public static final String SUPREME_KAI =
            "You are a Supreme Kai - prestiging will remove you from your universe role.";

    /** Cached {@code DestroyerRoleService.assignment(ServerPlayer)}. Misses are not cached. */
    private static volatile Method NOEA_ASSIGNMENT;
    /** Cached {@code Assignment.rank()}. Misses are not cached. */
    private static volatile Method NOEA_RANK;
    /** Cached {@code KaiApi.isSupremeKai(ServerPlayer)}. Misses are not cached. */
    private static volatile Method KAI_IS_SUPREME;

    private PrestigeUniverseRoleWarn() {}

    /**
     * True when Noea reports this player as a God of Destruction ({@code DestroyerRank.DESTROYER}).
     * False when Noea is absent or the check fails.
     */
    public static boolean isGodOfDestruction(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            Method assignmentMethod = NOEA_ASSIGNMENT;
            if (assignmentMethod == null) {
                Class<?> service = Class.forName(
                        "com.butterjaffa.noeabosses.DestroyerRoleService",
                        false,
                        PrestigeUniverseRoleWarn.class.getClassLoader());
                assignmentMethod = service.getMethod("assignment", ServerPlayer.class);
                NOEA_ASSIGNMENT = assignmentMethod;
            }
            Object assignment = assignmentMethod.invoke(null, player);
            if (assignment == null) {
                return false;
            }
            Method rankMethod = NOEA_RANK;
            if (rankMethod == null) {
                rankMethod = assignment.getClass().getMethod("rank");
                NOEA_RANK = rankMethod;
            }
            Object rank = rankMethod.invoke(assignment);
            if (rank == null) {
                return false;
            }
            // Compare via toString so we never reference DestroyerRank at compile time.
            return "DESTROYER".equals(rank.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when KaiPath reports this player as a Supreme Kai. False when KaiPath is absent or
     * the check fails.
     */
    public static boolean isSupremeKai(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            Method method = KAI_IS_SUPREME;
            if (method == null) {
                Class<?> api = Class.forName(
                        "com.dmzlegacy.kaipath.KaiApi",
                        false,
                        PrestigeUniverseRoleWarn.class.getClassLoader());
                method = api.getMethod("isSupremeKai", ServerPlayer.class);
                KAI_IS_SUPREME = method;
            }
            Object result = method.invoke(null, player);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
