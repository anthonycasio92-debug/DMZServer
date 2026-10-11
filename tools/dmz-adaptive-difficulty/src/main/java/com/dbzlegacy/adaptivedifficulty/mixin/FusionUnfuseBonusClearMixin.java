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
 * Dragon Mine Z bug workaround.
 *
 * <p>{@code FusionLogic.calculateAndApplyStats} stores the fusion stat bump with
 * {@code BonusStats.addBonusSplit(stat, "FusionBonus", "+", amount, true)} on the
 * leader. A resistance split is written onto defense and stamina, not {@code RES}.
 * {@code endFusion} calls {@code removeAllBonuses("FusionBonus")} only when the
 * leader's {@code StatsData} is online. The partner is never cleared, and the
 * stats packet is sent before this method returns, so a leftover key stays on
 * the player who just unfused.
 *
 * <p>This inject runs at the tail of {@code endFusion}, after that packet, and
 * removes {@code FusionBonus} from the caller and from the partner captured at
 * the head (unfuse wipes the partner id before the tail). Stock fusion does not
 * add a second zenkai key. Saiyan zenkai is {@code Zenkai_} plus the racial
 * count in {@code SaiyanPassiveHandler} and is left alone. A bonus whose name
 * contains both "fusion" and "zenkai" is removed, because that would be a
 * fusion-scoped zenkai rather than the permanent racial one.
 *
 * <p>The mixin names {@code com.dragonminez.server.util.FusionLogic} as a string.
 * A class literal loads that class while the mixin config is applied, and a
 * missing Dragon Mine Z jar then crashes startup. Each inject uses
 * {@code require = 0}. Mixin 0.8.5's {@code @Mixin} annotation has no
 * {@code require} parameter.
 */
@Mixin(targets = "com.dragonminez.server.util.FusionLogic", remap = false)
public abstract class FusionUnfuseBonusClearMixin {

    private static final String FUSION_BONUS = "FusionBonus";
    private static final String[] STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};
    /** Partner id from the head. Unfuse clears it before the tail runs. */
    private static final ThreadLocal<UUID> PARTNER = new ThreadLocal<>();

    @Inject(method = "endFusion", at = @At("HEAD"), remap = false, require = 0)
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

    @Inject(method = "endFusion", at = @At("TAIL"), remap = false, require = 0)
    private static void lm$clearFusionSplitBonuses(ServerPlayer player, StatsData data, boolean forced, CallbackInfo ci) {
        UUID partnerId = PARTNER.get();
        PARTNER.remove();
        try {
            FusionBonusOwner.runRaw(() -> clearAndSync(player, data));
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
            FusionBonusOwner.runRaw(() -> clearAndSync(partner, partnerData));
        } catch (Throwable ignored) {
        }
    }

    private static void clearAndSync(ServerPlayer player, StatsData data) {
        if (!clearFusionBonuses(data) || player == null) {
            return;
        }
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    /** @return true when a fusion split bonus was removed */
    private static boolean clearFusionBonuses(StatsData data) {
        if (data == null) {
            return false;
        }
        BonusStats bonuses = data.getBonusStats();
        if (bonuses == null) {
            return false;
        }
        boolean hadFusion = hasBonus(bonuses, FUSION_BONUS);
        bonuses.removeAllBonuses(FUSION_BONUS);
        Set<String> zenkai = fusionZenkaiNames(bonuses);
        for (String name : zenkai) {
            bonuses.removeAllBonuses(name);
        }
        return hadFusion || !zenkai.isEmpty();
    }

    private static boolean hasBonus(BonusStats bonuses, String name) {
        for (String stat : STATS) {
            List<BonusStats.StatBonus> list = bonuses.getBonuses(stat);
            if (list == null) {
                continue;
            }
            for (BonusStats.StatBonus bonus : list) {
                if (bonus != null && name.equals(bonus.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> fusionZenkaiNames(BonusStats bonuses) {
        Set<String> names = new LinkedHashSet<>();
        for (String stat : STATS) {
            List<BonusStats.StatBonus> list = bonuses.getBonuses(stat);
            if (list == null) {
                continue;
            }
            for (BonusStats.StatBonus bonus : list) {
                if (bonus == null || bonus.name == null) {
                    continue;
                }
                String lower = bonus.name.toLowerCase(Locale.ROOT);
                if (lower.contains("fusion") && lower.contains("zenkai")) {
                    names.add(bonus.name);
                }
            }
        }
        return names;
    }
}
