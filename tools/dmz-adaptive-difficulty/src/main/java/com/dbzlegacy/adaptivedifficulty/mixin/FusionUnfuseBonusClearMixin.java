package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.FusionBonusOwner;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deletes leftover fusion split bonuses when a fusion ends.
 *
 * <p>DMZ 2.1.3 {@code FusionStatusHandler} has no {@code endFusion}. Its tick
 * handler, and {@code ForgeCommonEvents.endFusionIfNeeded}, both call
 * {@code FusionLogic.endFusion}. That method writes {@code FusionBonus} on fuse
 * via {@code addBonusSplit} and, on unfuse, calls {@code removeAllBonuses("FusionBonus")}
 * on the leader only, and only when that leader's stats are online.
 * {@code removeAllBonuses} already walks every stat list and drops an exact name,
 * including rows written by {@code addBonusSplit}. The partner is never cleared,
 * and a fusion-zenkai name is not equal to {@code FusionBonus}.
 * {@code clearFusionState} then drops the partner UUID, so the tail has to use
 * the id captured at the head.
 *
 * <p>{@code clearAllSplit(String)} is not a name wipe. Its argument is a stat
 * id: {@code RES} clears the DEF and STM columns, and any other argument
 * calls {@code clearAll} on that stat. {@code clearAllSplit("FusionBonus")}
 * looks up a stat named {@code FUSIONBONUS} and removes nothing.
 * {@code clearBonus} matches with {@code String.contains} and can delete a
 * racial key. This mixin does not call those methods.
 *
 * <p>Removal uses {@code removeAllBonuses} and {@code removeBonusSplit}, which
 * match the bonus name with {@code String.equals}. Racial keys
 * {@code Zenkai_}, {@code Assimilation_}, and {@code Absorption_} are never
 * passed to those methods. A name that contains both "fusion" and "zenkai"
 * and is not one of those racial prefixes is a fusion-scoped zenkai and is
 * removed by its exact name.
 *
 * <p>The class is named as a string so mixin config does not load
 * {@code FusionLogic} while the config is applied. {@link FusionBonusReadGateMixin}
 * stays in place and hides a key the delete did not reach.
 */
@Mixin(targets = "com.dragonminez.server.util.FusionLogic", remap = false)
public abstract class FusionUnfuseBonusClearMixin {
    private static final String FUSION_BONUS = "FusionBonus";
    /** RES is included so {@code removeBonusSplit} also clears the DEF+STM split. */
    private static final String[] SPLIT_STATS = {"STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE"};
    private static final String ZENKAI_PREFIX = "Zenkai_";
    private static final String ASSIMILATION_PREFIX = "Assimilation_";
    private static final String ABSORPTION_PREFIX = "Absorption_";
    /** Partner id from the head. Unfuse clears it before the tail runs. */
    private static final ThreadLocal<UUID> PARTNER = new ThreadLocal<>();

    @Inject(method = "endFusion", at = @At("HEAD"), remap = false)
    private static void lm$rememberFusionPartner(ServerPlayer player, StatsData data, boolean forced, CallbackInfo ci) {
        UUID partner = null;
        try {
            if (data != null && data.getStatus() != null) {
                partner = data.getStatus().getFusionPartnerUUID();
            }
        } catch (Throwable ignored) {
        }
        PARTNER.set(partner);
    }

    @Inject(method = "endFusion", at = @At("TAIL"), remap = false)
    private static void lm$clearFusionSplitBonuses(ServerPlayer player, StatsData data, boolean forced, CallbackInfo ci) {
        UUID partnerId = PARTNER.get();
        PARTNER.remove();
        try {
            clearAndSync(player, data);
            if (player == null || partnerId == null || partnerId.equals(player.m_20148_())) {
                return;
            }
            MinecraftServer server = player.m_20194_();
            if (server == null) {
                return;
            }
            ServerPlayer partner = server.m_6846_().m_11259_(partnerId);
            if (partner == null) {
                return;
            }
            StatsData partnerData = DmzProgression.stats(partner);
            if (partnerData == data) {
                return;
            }
            clearAndSync(partner, partnerData);
        } catch (Throwable ignored) {
        }
    }

    private static void clearAndSync(ServerPlayer player, StatsData data) {
        if (player == null) {
            deleteFusionKeys(data);
            return;
        }
        deleteFusionKeys(data);
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Drop {@code FusionBonus} and fusion-zenkai names. Racial
     * {@code Zenkai_} / {@code Assimilation_} / {@code Absorption_} keys stay.
     */
    private static void deleteFusionKeys(StatsData data) {
        if (data == null) {
            return;
        }
        BonusStats bonuses;
        try {
            bonuses = data.getBonusStats();
        } catch (Throwable ignored) {
            return;
        }
        if (bonuses == null) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(FUSION_BONUS);
        FusionBonusOwner.runRaw(() -> collectFusionZenkaiNames(bonuses, names));
        for (String name : names) {
            if (!deletableFusionKey(name)) {
                continue;
            }
            try {
                bonuses.removeAllBonuses(name);
            } catch (Throwable ignored) {
            }
            for (String stat : SPLIT_STATS) {
                try {
                    bonuses.removeBonusSplit(stat, name);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** Read-gate is off inside {@link FusionBonusOwner#runRaw} so the stored key is visible. */
    private static void collectFusionZenkaiNames(BonusStats bonuses, Set<String> names) {
        for (String stat : SPLIT_STATS) {
            List<BonusStats.StatBonus> list;
            try {
                list = bonuses.getBonuses(stat);
            } catch (Throwable ignored) {
                continue;
            }
            if (list == null) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                BonusStats.StatBonus bonus = list.get(i);
                if (bonus == null || bonus.name == null) {
                    continue;
                }
                if (deletableFusionKey(bonus.name) && !FUSION_BONUS.equals(bonus.name)) {
                    names.add(bonus.name);
                }
            }
        }
    }

    /**
     * Exact fusion keys only. A racial prefix is preserved even when the rest
     * of the name also mentions fusion.
     */
    private static boolean deletableFusionKey(String name) {
        if (name == null || name.isEmpty() || racialKey(name)) {
            return false;
        }
        if (FUSION_BONUS.equals(name)) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("fusion") && lower.contains("zenkai");
    }

    private static boolean racialKey(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.startsWith(ZENKAI_PREFIX)
                || name.startsWith(ASSIMILATION_PREFIX)
                || name.startsWith(ABSORPTION_PREFIX);
    }
}
