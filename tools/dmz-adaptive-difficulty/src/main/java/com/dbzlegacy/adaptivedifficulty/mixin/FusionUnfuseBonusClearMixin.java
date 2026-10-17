package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.FusionBonusOwner;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
 * <p>Removal is an allowlist sweep, not a name heuristic: every stored bonus
 * name is enumerated from the serialized map ({@code BonusStats.save()}) and
 * removed with {@code removeAllBonuses}, except keys starting with
 * {@code Zenkai_}, {@code Assimilation_}, or {@code Absorption_}. That covers
 * {@code FusionBonus}, fusion-zenkai name variants the old contains("fusion")
 * heuristic could miss, and rows on any stat id DMZ adds later; removal matches
 * with {@code String.equals}, so a racial key that merely mentions fusion in the
 * rest of its name stays. The same sweep runs for the partner, and a
 * {@code StatsSyncS2C} follows each player. Other writers of non-racial keys
 * (weapon bonus scaling, piercing, DMZ passives) re-apply on their own pulses
 * and login events — the sweep only drops what a fusion produced.
 *
 * <p>The class is named as a string so mixin config does not load
 * {@code FusionLogic} while the config is applied. {@link FusionBonusReadGateMixin}
 * stays in place and hides a key the delete did not reach.
 */
@Mixin(targets = "com.dragonminez.server.util.FusionLogic", remap = false)
public abstract class FusionUnfuseBonusClearMixin {
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
            deleteNonRacialKeys(data);
            return;
        }
        deleteNonRacialKeys(data);
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Allowlist sweep: delete every stored bonus key except the racial
     * {@code Zenkai_} / {@code Assimilation_} / {@code Absorption_} prefixes.
     * Names are snapshotted from the serialized map so every stat id DMZ
     * stores is covered, then removed by exact name through
     * {@code removeAllBonuses}, which walks all stat lists including the
     * DEF+STM split copies.
     */
    private static void deleteNonRacialKeys(StatsData data) {
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
        FusionBonusOwner.runRaw(() -> collectNonRacialNames(bonuses, names));
        for (String name : names) {
            try {
                bonuses.removeAllBonuses(name);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Read-gate is off inside {@link FusionBonusOwner#runRaw} so every stored key is visible. */
    private static void collectNonRacialNames(BonusStats bonuses, Set<String> names) {
        try {
            CompoundTag root = bonuses.save();
            if (root == null) {
                return;
            }
            for (String statKey : root.m_128431_()) {
                ListTag rows = root.m_128437_(statKey, 10);
                if (rows == null) {
                    continue;
                }
                for (int i = 0; i < rows.size(); i++) {
                    CompoundTag row = rows.m_128728_(i);
                    if (row == null) {
                        continue;
                    }
                    String name = row.m_128461_("Name");
                    if (name != null && !name.isEmpty() && !racialKey(name)) {
                        names.add(name);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
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
