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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Global head-bone unlock shop and equip (cross-race cosmetics). */
public final class CosmeticHeadBoneService {
    public static final int CARDS_PER_PAGE = 14;

    /** Prior active bone while a shop card is being previewed. Not written to player json. */
    private static final Map<UUID, String> PREVIEW_STASH = new ConcurrentHashMap<>();

    private CosmeticHeadBoneService() {}

    public static boolean isPreviewing(ServerPlayer player) {
        return player != null && PREVIEW_STASH.containsKey(player.m_20148_());
    }

    /** Bone currently shown, when a preview is active. */
    public static String previewBoneId(ServerPlayer player) {
        return isPreviewing(player) ? activeBone(player) : "";
    }

    public static String previewLabel(ServerPlayer player) {
        String id = previewBoneId(player);
        if (id.isBlank()) {
            return "";
        }
        CosmeticHeadBoneCatalog.Entry entry = CosmeticHeadBoneCatalog.get(id);
        return entry == null ? CosmeticHeadBoneCatalog.prettyId(id) : entry.displayName();
    }

    /**
     * Show {@code boneId} on the player without charging, unlocking, or saving
     * {@code equippedHeadBone}. The bone they had on is kept until restore, a new preview,
     * or a real equip.
     */
    public static String previewBone(ServerPlayer player, String boneId) {
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
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return "§cCharacter data unavailable.";
        }
        PREVIEW_STASH.putIfAbsent(player.m_20148_(), activeBone(player));
        try {
            if (!bone.equalsIgnoreCase(activeBone(player))) {
                ch.setActiveHeadBone(bone);
                RaceHeadBoneSync.syncClient(player);
            }
            return "";
        } catch (Throwable t) {
            return "§cCould not preview that head part.";
        }
    }

    /** Put back the bone from before the preview. */
    public static String restorePreview(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        String prior = PREVIEW_STASH.remove(player.m_20148_());
        if (prior == null) {
            return "";
        }
        return applyActiveOnly(player, prior);
    }

    /** Drop the stash without restoring. A purchase or real equip replaces the preview. */
    public static void discardPreview(ServerPlayer player) {
        if (player != null) {
            PREVIEW_STASH.remove(player.m_20148_());
        }
    }

    /**
     * After a restart, an unpaid preview must not stay on the character. A saved
     * equipped bone wins. A bone the player does not own is cleared.
     */
    public static void settleAfterLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PREVIEW_STASH.remove(player.m_20148_());
        String equipped = equippedBone(player).toLowerCase(Locale.ROOT);
        String active = activeBone(player).toLowerCase(Locale.ROOT);
        if (!equipped.isEmpty() && isBoneAllowed(player, equipped) && !equipped.equals(active)) {
            applyActiveOnly(player, equipped);
            return;
        }
        if (!active.isEmpty() && !isBoneAllowed(player, active)) {
            String fallback = unequipHeadBoneId(player);
            if (!fallback.isEmpty() && !isBoneAllowed(player, fallback)) {
                fallback = "";
            }
            applyActiveOnly(player, fallback);
        }
    }

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

    /** Persisted cosmetic choice (survives race change unlock list; re-applied after forms). */
    public static String equippedBone(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        String stored = rec.equippedHeadBone;
        return stored == null ? "" : stored.trim();
    }

    public static void persistEquippedBone(ServerPlayer player, String boneId) {
        if (player == null) {
            return;
        }
        String bone = boneId == null ? "" : boneId.trim().toLowerCase(Locale.ROOT);
        CharacterServicesStore.PlayerRecord rec =
                CharacterServicesStore.get().record(player.m_20148_().toString());
        rec.equippedHeadBone = bone;
        CharacterServicesStore.get().markDirty();
    }

    /**
     * DMZ often clears or resets {@code activeHeadBone} when transforming. Restore the player's
     * chosen part on the next server tick so it runs after DMZ applies the new form mesh.
     */
    public static void scheduleReapplyAfterFormChange(ServerPlayer player) {
        if (player == null || !persistThroughFormsEnabled()) {
            return;
        }
        var server = player.m_20194_();
        Runnable task = () -> reapplyAfterFormChange(player);
        if (server != null) {
            server.execute(task);
        } else {
            task.run();
        }
    }

    /**
     * @return true when character data was updated and clients were synced
     */
    public static boolean reapplyAfterFormChange(ServerPlayer player) {
        if (player == null || !persistThroughFormsEnabled()) {
            return false;
        }
        String want = resolveEquippedBone(player);
        if (want.isEmpty() || !isBoneAllowed(player, want)) {
            return false;
        }
        String current = activeBone(player);
        if (want.equalsIgnoreCase(current)) {
            return false;
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return false;
        }
        try {
            ch.setActiveHeadBone(want);
            RaceHeadBoneSync.syncClient(player);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean persistThroughFormsEnabled() {
        return CharacterServicesConfig.get().headBoneShop.persistThroughForms;
    }

    private static String resolveEquippedBone(ServerPlayer player) {
        if (isPreviewing(player)) {
            String stored = equippedBone(player);
            return stored.isEmpty() ? "" : stored.toLowerCase(Locale.ROOT);
        }
        String stored = equippedBone(player);
        if (!stored.isEmpty()) {
            return stored.toLowerCase(Locale.ROOT);
        }
        // Legacy players: remember current allowed part once.
        String active = activeBone(player);
        if (active.isEmpty() || !isBoneAllowed(player, active)) {
            return "";
        }
        String bone = active.toLowerCase(Locale.ROOT);
        persistEquippedBone(player, bone);
        return bone;
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
        discardPreview(player);
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
        discardPreview(player);
        String prior = priorActiveBone == null ? "" : priorActiveBone.trim();
        if (!prior.isEmpty() && isBoneAllowed(player, prior)) {
            Character ch = DmzProgression.character(player);
            if (ch != null) {
                try {
                    String bone = prior.toLowerCase(Locale.ROOT);
                    ch.setActiveHeadBone(bone);
                    persistEquippedBone(player, bone);
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

    /** First head bone in this race's config (DMZ default for that race). */
    public static String raceDefaultHeadBoneId(ServerPlayer player) {
        String race = DmzProgression.race(player);
        List<String> bones = DmzContentDiscovery.headBonesForRaceOrdered(race);
        if (bones.isEmpty()) {
            return "hair";
        }
        return bones.get(0);
    }

    /**
     * Minimal look: {@code hair} when the race supports it, otherwise clear extra head bone
     * (empty — DMZ uses base head without add-on parts).
     */
    public static String unequipHeadBoneId(ServerPlayer player) {
        String race = DmzProgression.race(player);
        List<String> bones = DmzContentDiscovery.headBonesForRaceOrdered(race);
        for (String bone : bones) {
            if ("hair".equals(bone)) {
                return "hair";
            }
        }
        return "";
    }

    public static String executeRaceDefaultHeadBone(ServerPlayer player) {
        if (!canUse(player)) {
            return "§cHead bone shop is unavailable.";
        }
        String bone = raceDefaultHeadBoneId(player);
        return applyHeadBoneDirect(player, bone, "§aSet race default head part §f");
    }

    public static String executeUnequipHeadBone(ServerPlayer player) {
        if (!canUse(player)) {
            return "§cHead bone shop is unavailable.";
        }
        String bone = unequipHeadBoneId(player);
        if (bone.isEmpty()) {
            return applyHeadBoneDirect(player, "", "§aUnequipped extra head parts.");
        }
        return applyHeadBoneDirect(player, bone, "§aUnequipped to §f");
    }

    private static String applyHeadBoneDirect(ServerPlayer player, String bone, String prefix) {
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return "§cCharacter data unavailable.";
        }
        discardPreview(player);
        try {
            String normalized = bone == null ? "" : bone.trim().toLowerCase(Locale.ROOT);
            ch.setActiveHeadBone(normalized);
            persistEquippedBone(player, normalized);
            RaceHeadBoneSync.syncClient(player);
            if (prefix.contains("Unequipped extra")) {
                return prefix;
            }
            CosmeticHeadBoneCatalog.Entry entry = CosmeticHeadBoneCatalog.get(bone);
            String label = entry == null ? CosmeticHeadBoneCatalog.prettyId(bone) : entry.displayName();
            if (label.isBlank()) {
                label = bone.isBlank() ? "none" : bone;
            }
            return prefix + label + "§a.";
        } catch (Throwable t) {
            return "§cCould not update head part.";
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
        discardPreview(player);
        try {
            ch.setActiveHeadBone(bone);
            persistEquippedBone(player, bone);
            RaceHeadBoneSync.syncClient(player);
            CosmeticHeadBoneCatalog.Entry entry = CosmeticHeadBoneCatalog.get(bone);
            String label = entry == null ? CosmeticHeadBoneCatalog.prettyId(bone) : entry.displayName();
            return "§aEquipped head part §f" + label + "§a.";
        } catch (Throwable t) {
            return "§cCould not equip that head part.";
        }
    }

    /** Visual only. Does not write {@code equippedHeadBone}. */
    private static String applyActiveOnly(ServerPlayer player, String bone) {
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return "§cCharacter data unavailable.";
        }
        try {
            String normalized = bone == null ? "" : bone.trim().toLowerCase(Locale.ROOT);
            ch.setActiveHeadBone(normalized);
            RaceHeadBoneSync.syncClient(player);
            return "§7Restored your previous head part.";
        } catch (Throwable t) {
            return "§cCould not restore that head part.";
        }
    }
}
