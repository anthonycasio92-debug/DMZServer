package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Global head-bone unlock shop and equip (cross-race cosmetics). */
public final class CosmeticHeadBoneService {
    public static final int CARDS_PER_PAGE = 28;

    private CosmeticHeadBoneService() {}

    public static boolean shopEnabled() {
        return CharacterServicesConfig.get().enabled
                && CharacterServicesConfig.get().headBoneShop.enabled;
    }

    public static boolean canUse(ServerPlayer player) {
        return shopEnabled()
                && CharacterServicesAccess.canUseServices(player)
                && CharacterServicesAccess.canHeadBoneShop(player);
    }

    public static boolean isNativeForRace(String raceId, String boneId) {
        if (raceId == null || boneId == null) {
            return false;
        }
        if (!CharacterServicesConfig.get().headBoneShop.nativeRaceBonesFree) {
            return false;
        }
        String race = raceId.trim().toLowerCase(Locale.ROOT);
        String bone = boneId.trim().toLowerCase(Locale.ROOT);
        try {
            RaceCharacterConfig cfg = ConfigManager.getRaceCharacter(race);
            if (cfg == null) {
                return false;
            }
            String[] bones = cfg.getHeadBones();
            if (bones == null) {
                return false;
            }
            for (String b : bones) {
                if (b != null && bone.equals(b.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean hasUnlock(ServerPlayer player, String boneId) {
        if (player == null || boneId == null || boneId.isBlank()) {
            return false;
        }
        if (CharacterServicesAccess.bypassCost(player)) {
            return true;
        }
        String bone = boneId.trim().toLowerCase(Locale.ROOT);
        String race = DmzProgression.race(player);
        if (isNativeForRace(race, bone)) {
            return true;
        }
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        Set<String> unlocked = rec.unlockedHeadBones;
        if (unlocked == null) {
            return false;
        }
        for (String u : unlocked) {
            if (u != null && bone.equals(u.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBoneAllowed(ServerPlayer player, String boneId) {
        if (player == null || boneId == null || boneId.isBlank()) {
            return false;
        }
        if (!CosmeticHeadBoneCatalog.isKnown(boneId)) {
            return false;
        }
        return hasUnlock(player, boneId);
    }

    public static Set<String> allowedBoneIds(ServerPlayer player) {
        Set<String> out = new HashSet<>();
        for (CosmeticHeadBoneCatalog.Entry e : CosmeticHeadBoneCatalog.all()) {
            if (hasUnlock(player, e.id())) {
                out.add(e.id());
            }
        }
        return out;
    }

    public static String activeBone(ServerPlayer player) {
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return "";
        }
        try {
            String raw = ch.getActiveHeadBone();
            return raw == null ? "" : raw.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    public static long unlockCost(ServerPlayer player, String boneId) {
        CharacterServicesConfig.HeadBoneShop shop = CharacterServicesConfig.get().headBoneShop;
        if (hasUnlock(player, boneId)) {
            return 0L;
        }
        long base = shop.defaultUnlockCostCopper;
        if (shop.boneCosts != null && boneId != null) {
            Long override = shop.boneCosts.get(boneId.trim().toLowerCase(Locale.ROOT));
            if (override == null) {
                override = shop.boneCosts.get(boneId);
            }
            if (override != null && override >= 0L) {
                base = override;
            }
        }
        return CharacterServicesSystem.payableCost(player, base, shop.levelCostMultiplier);
    }

    public static int pageCount() {
        int n = CosmeticHeadBoneCatalog.all().size();
        if (n <= 0) {
            return 1;
        }
        return (n + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
    }

    /**
     * GUI card rows: {@code boneId\\tdisplay\\tstate\\tcostText} where state is
     * {@code E} equipped, {@code U} unlocked, {@code N} native, {@code L} locked.
     */
    public static List<String> cards(ServerPlayer player, int page) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        List<CosmeticHeadBoneCatalog.Entry> all = CosmeticHeadBoneCatalog.all();
        int pages = pageCount();
        int p = Math.max(0, Math.min(page, pages - 1));
        int from = p * CARDS_PER_PAGE;
        int to = Math.min(all.size(), from + CARDS_PER_PAGE);
        String equipped = activeBone(player).toLowerCase(Locale.ROOT);
        String race = DmzProgression.race(player);
        for (int i = from; i < to; i++) {
            CosmeticHeadBoneCatalog.Entry e = all.get(i);
            String state;
            if (e.id().equalsIgnoreCase(equipped)) {
                state = "E";
            } else if (hasUnlock(player, e.id())) {
                state = isNativeForRace(race, e.id()) ? "N" : "U";
            } else {
                state = "L";
            }
            String cost = state.equals("L")
                    ? CharacterServicesSystem.formatCost(unlockCost(player, e.id()))
                    : "";
            out.add(e.id() + "\t" + e.displayName() + "\t" + state + "\t" + cost);
        }
        return out;
    }

    public static String executeUnlock(ServerPlayer player, String boneId) {
        if (!canUse(player)) {
            return "§cHead bone shop is unavailable.";
        }
        if (boneId == null || boneId.isBlank()) {
            return "§cPick a head part.";
        }
        String bone = boneId.trim().toLowerCase(Locale.ROOT);
        if (!CosmeticHeadBoneCatalog.isKnown(bone)) {
            return "§cUnknown head part.";
        }
        if (hasUnlock(player, bone)) {
            return executeEquip(player, bone);
        }
        long cost = CharacterServicesAccess.bypassCost(player) ? 0L : unlockCost(player, bone);
        AncientCoinEconomy.migrateWalletToItems(player);
        if (cost > 0L && !AncientCoinEconomy.canAfford(player, cost)) {
            return "§c" + AncientCoinEconomy.missingText(player, cost);
        }
        if (cost > 0L && !AncientCoinEconomy.charge(player, cost)) {
            return "§c" + AncientCoinEconomy.missingText(player, cost);
        }
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        if (rec.unlockedHeadBones == null) {
            rec.unlockedHeadBones = new java.util.LinkedHashSet<>();
        }
        rec.unlockedHeadBones.add(bone);
        CharacterServicesStore.get().markDirty();
        CharacterServicesStore.get().save();
        return executeEquip(player, bone);
    }

    /** Shop unlocks live in {@link CharacterServicesStore} and are never removed by race change. */
    public static boolean hasPersistedUnlock(ServerPlayer player, String boneId) {
        if (player == null || boneId == null || boneId.isBlank()) {
            return false;
        }
        String bone = boneId.trim().toLowerCase(Locale.ROOT);
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        if (rec.unlockedHeadBones == null) {
            return false;
        }
        for (String u : rec.unlockedHeadBones) {
            if (u != null && bone.equals(u.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * After race change: keep purchased unlocks and re-apply the previous part when still valid
     * for the new race (cross-race cosmetics stay usable).
     */
    public static void reapplyHeadBoneAfterRaceChange(ServerPlayer player, String priorActiveBone) {
        if (player == null) {
            return;
        }
        String prior = priorActiveBone == null ? "" : priorActiveBone.trim();
        if (!prior.isEmpty() && isBoneAllowed(player, prior)) {
            Character ch = DmzProgression.character(player);
            if (ch != null) {
                try {
                    ch.setActiveHeadBone(prior.toLowerCase(Locale.ROOT));
                    RaceHeadBoneSync.syncClient(player);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
        if (RaceHeadBoneSync.syncCharacter(player)) {
            RaceHeadBoneSync.syncClient(player);
        }
    }

    public static String executeEquip(ServerPlayer player, String boneId) {
        if (!canUse(player)) {
            return "§cHead bone shop is unavailable.";
        }
        if (boneId == null || boneId.isBlank()) {
            return "§cPick a head part.";
        }
        String bone = boneId.trim().toLowerCase(Locale.ROOT);
        if (!isBoneAllowed(player, bone)) {
            long cost = unlockCost(player, bone);
            return "§cUnlock §f" + CosmeticHeadBoneCatalog.prettyId(bone)
                    + " §cfirst (§f" + CharacterServicesSystem.formatCost(cost) + "§c).";
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return "§cCharacter data unavailable.";
        }
        try {
            ch.setActiveHeadBone(bone);
            RaceHeadBoneSync.syncClient(player);
            CosmeticHeadBoneCatalog.Entry entry = CosmeticHeadBoneCatalog.get(bone);
            String label = entry == null ? CosmeticHeadBoneCatalog.prettyId(bone) : entry.displayName();
            return "§aEquipped head part §f" + label + "§a.";
        } catch (Throwable t) {
            return "§cCould not equip that head part.";
        }
    }
}
