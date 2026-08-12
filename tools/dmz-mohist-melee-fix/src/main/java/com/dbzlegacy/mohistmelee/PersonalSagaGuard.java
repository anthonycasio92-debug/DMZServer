package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.PlayerQuestData.QuestStatus;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tracks quests a player personally earned (solo or while in the party at completion).
 * <p>
 * Anti-cheese rules:
 * <ul>
 *   <li>Party sync must not copy already-{@code SUCCESS} quests onto someone who was not
 *       actively on that quest / had not earned it.</li>
 *   <li>Reward claims are cancelled unless the quest is personally earned.</li>
 * </ul>
 * Never wipe existing saga progress on party leave, login, or death. Only strip completions
 * that appeared <em>during</em> a party merge and were not earned / not previously accepted.
 */
public final class PersonalSagaGuard {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    /** Set while {@link com.dragonminez.common.quest.PartyManager} is syncing quest state. */
    public static final ThreadLocal<ServerPlayer> MERGE_TARGET = new ThreadLocal<>();

    private static final String ROOT_KEY = DmzMohistMeleeFix.MOD_ID;
    private static final String EARNED_KEY = "personalEarnedQuests";
    private static final String BOOT_KEY = "personalEarnBootstrapped";

    private static final ConcurrentHashMap<UUID, Set<String>> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> BOOTSTRAPPED = new ConcurrentHashMap<>();
    private static final AtomicInteger STRIP_LOGS = new AtomicInteger();

    private PersonalSagaGuard() {}

    public static boolean hasEarned(ServerPlayer player, String questKey) {
        if (player == null || questKey == null || questKey.isBlank()) {
            return false;
        }
        ensureBootstrapped(player);
        Set<String> earned = CACHE.get(player.m_20148_());
        return earned != null && earned.contains(questKey);
    }

    public static boolean canInheritCompletion(ServerPlayer target, String questKey, QuestStatus ownStatus) {
        if (target == null || questKey == null || questKey.isBlank()) {
            return false;
        }
        if (hasEarned(target, questKey)) {
            return true;
        }
        // True co-op only: member must already be on the quest.
        return ownStatus == QuestStatus.ACCEPTED;
    }

    /**
     * After a party quest merge: remove only completions that were newly applied by this merge
     * and were not personally earned / previously accepted. Never touches older progress.
     *
     * @return true if any quest was stripped
     */
    public static boolean stripNewlyBorrowedCompletions(
            ServerPlayer player,
            PlayerQuestData data,
            Set<String> completedBefore,
            Set<String> acceptedBefore
    ) {
        if (player == null || data == null) {
            return false;
        }
        ensureBootstrapped(player);
        Set<String> beforeCompleted = completedBefore != null ? completedBefore : Set.of();
        Set<String> beforeAccepted = acceptedBefore != null ? acceptedBefore : Set.of();
        boolean changed = false;
        for (String questId : new ArrayList<>(data.getCompletedQuestIds())) {
            if (questId == null || questId.isBlank()) {
                continue;
            }
            if (beforeCompleted.contains(questId)) {
                continue; // already had this completion — never wipe
            }
            if (beforeAccepted.contains(questId) || hasEarned(player, questId)) {
                // Co-op member who was on the quest, or already credited.
                markEarned(player, questId);
                continue;
            }
            data.resetQuest(questId);
            changed = true;
            int n = STRIP_LOGS.incrementAndGet();
            if (n <= 60) {
                LOGGER.info(
                        "[{}] stripped newly borrowed SUCCESS: player={} quest={}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_36316_().getName(),
                        questId
                );
            }
        }
        return changed;
    }

    public static Set<String> snapshotCompleted(PlayerQuestData data) {
        if (data == null) {
            return Set.of();
        }
        return new HashSet<>(data.getCompletedQuestIds());
    }

    public static Set<String> snapshotAccepted(PlayerQuestData data) {
        if (data == null) {
            return Set.of();
        }
        return new HashSet<>(data.getAcceptedQuestIds());
    }

    public static void markEarned(ServerPlayer player, String questKey) {
        if (player == null || questKey == null || questKey.isBlank()) {
            return;
        }
        ensureBootstrapped(player);
        UUID id = player.m_20148_();
        Set<String> earned = CACHE.computeIfAbsent(id, u -> ConcurrentHashMap.newKeySet());
        if (earned.add(questKey)) {
            BOOTSTRAPPED.put(id, Boolean.TRUE);
            save(player);
        }
    }

    public static void markEarnedAll(Iterable<ServerPlayer> players, String questKey) {
        if (players == null || questKey == null || questKey.isBlank()) {
            return;
        }
        for (ServerPlayer player : players) {
            markEarned(player, questKey);
        }
    }

    public static void clearAll(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        CACHE.put(id, ConcurrentHashMap.newKeySet());
        BOOTSTRAPPED.put(id, Boolean.TRUE);
        save(player);
    }

    public static void clearSaga(ServerPlayer player, String sagaId) {
        if (player == null || sagaId == null || sagaId.isBlank()) {
            return;
        }
        ensureBootstrapped(player);
        String prefix = sagaId + ":";
        Set<String> earned = CACHE.get(player.m_20148_());
        if (earned == null || earned.isEmpty()) {
            return;
        }
        boolean changed = earned.removeIf(key -> key != null && key.startsWith(prefix));
        if (changed) {
            BOOTSTRAPPED.put(player.m_20148_(), Boolean.TRUE);
            save(player);
        }
    }

    public static void clearQuest(ServerPlayer player, String questKey) {
        if (player == null || questKey == null || questKey.isBlank()) {
            return;
        }
        Set<String> earned = CACHE.get(player.m_20148_());
        if (earned != null && earned.remove(questKey)) {
            BOOTSTRAPPED.put(player.m_20148_(), Boolean.TRUE);
            save(player);
        }
    }

    public static void unload(UUID playerId) {
        if (playerId == null) {
            return;
        }
        CACHE.remove(playerId);
        BOOTSTRAPPED.remove(playerId);
    }

    /**
     * Death/respawn: same UUID — keep memory cache and copy earn NBT onto the new entity.
     * Do <b>not</b> unload/re-bootstrap from an empty new player (that wiped earn marks).
     */
    public static void carryOverAfterClone(ServerPlayer original, ServerPlayer respawned) {
        if (original == null || respawned == null) {
            return;
        }
        UUID id = respawned.m_20148_();
        if (!CACHE.containsKey(id)) {
            // Prefer reading from the original entity (still has NBT).
            loadFromEntity(original, id);
        }
        if (!Boolean.TRUE.equals(BOOTSTRAPPED.get(id))) {
            // Seed from original quest data if present, else respawned.
            PlayerQuestData pqd = questData(original);
            if (pqd == null) {
                pqd = questData(respawned);
            }
            seedBootstrap(respawned, id, pqd);
        }
        save(respawned);
    }

    public static void ensureBootstrapped(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        if (!CACHE.containsKey(id)) {
            loadFromEntity(player, id);
        }
        if (!Boolean.TRUE.equals(BOOTSTRAPPED.get(id))) {
            seedBootstrap(player, id, questData(player));
            return;
        }
        // Repair death/clone desync: bootstrapped with empty earns but live SUCCESS remains.
        // Safe because party merge no longer grants borrowed SUCCESS (and delta-strips if it does).
        repairEmptyEarnFromCompletions(player);
    }

    /**
     * If earn marks were lost (e.g. old clone bug) but quest completions remain, re-credit them
     * so legitimate claims are not blocked forever.
     */
    public static void repairEmptyEarnFromCompletions(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        Set<String> earned = CACHE.get(id);
        if (earned == null || !earned.isEmpty()) {
            return;
        }
        PlayerQuestData pqd = questData(player);
        if (pqd == null) {
            return;
        }
        Set<String> completed = pqd.getCompletedQuestIds();
        if (completed == null || completed.isEmpty()) {
            return;
        }
        for (String questId : completed) {
            if (questId != null && !questId.isBlank()) {
                earned.add(questId);
            }
        }
        BOOTSTRAPPED.put(id, Boolean.TRUE);
        save(player);
        LOGGER.info(
                "[{}] repaired empty earn marks from {} live completion(s) for {}",
                DmzMohistMeleeFix.MOD_ID,
                earned.size(),
                player.m_36316_().getName()
            );
    }

    private static void seedBootstrap(ServerPlayer player, UUID id, PlayerQuestData pqd) {
        Set<String> seeded = CACHE.computeIfAbsent(id, u -> ConcurrentHashMap.newKeySet());
        if (pqd != null) {
            for (String questId : pqd.getCompletedQuestIds()) {
                if (questId != null && !questId.isBlank()) {
                    seeded.add(questId);
                }
            }
        }
        BOOTSTRAPPED.put(id, Boolean.TRUE);
        save(player);
        if (!seeded.isEmpty()) {
            LOGGER.info(
                    "[{}] personal saga earn bootstrap: {} seeded {} completed quest(s)",
                    DmzMohistMeleeFix.MOD_ID,
                    player.m_36316_().getName(),
                    seeded.size()
            );
        }
    }

    /**
     * Resolve the online owner of a {@link PlayerQuestData} instance (identity match).
     */
    public static ServerPlayer findOwner(PlayerQuestData questData) {
        if (questData == null) {
            return null;
        }
        ServerPlayer mergeTarget = MERGE_TARGET.get();
        if (mergeTarget != null) {
            PlayerQuestData tagged = questData(mergeTarget);
            if (tagged == questData) {
                return mergeTarget;
            }
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            PlayerQuestData data = questData(player);
            if (data == questData) {
                return player;
            }
        }
        return null;
    }

    public static Set<String> viewEarned(ServerPlayer player) {
        if (player == null) {
            return Collections.emptySet();
        }
        ensureBootstrapped(player);
        Set<String> earned = CACHE.get(player.m_20148_());
        return earned == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(earned));
    }

    private static void loadFromEntity(ServerPlayer player, UUID id) {
        CompoundTag root = PersistentDataAccess.get(player);
        CompoundTag mod = root.m_128469_(ROOT_KEY);
        Set<String> earned = ConcurrentHashMap.newKeySet();
        if (mod.m_128425_(EARNED_KEY, 9)) {
            ListTag list = mod.m_128437_(EARNED_KEY, 8);
            for (int i = 0; i < list.size(); i++) {
                String key = list.m_128778_(i);
                if (key != null && !key.isBlank()) {
                    earned.add(key);
                }
            }
        }
        CACHE.put(id, earned);
        BOOTSTRAPPED.put(id, mod.m_128471_(BOOT_KEY));
    }

    private static void save(ServerPlayer player) {
        UUID id = player.m_20148_();
        CompoundTag root = PersistentDataAccess.get(player);
        CompoundTag mod = root.m_128469_(ROOT_KEY);
        ListTag list = new ListTag();
        Set<String> earned = CACHE.getOrDefault(id, Collections.emptySet());
        for (String key : earned) {
            if (key != null && !key.isBlank()) {
                list.add(StringTag.m_129297_(key));
            }
        }
        mod.m_128365_(EARNED_KEY, list);
        mod.m_128379_(BOOT_KEY, Boolean.TRUE.equals(BOOTSTRAPPED.get(id)));
        root.m_128365_(ROOT_KEY, mod);
    }

    private static PlayerQuestData questData(ServerPlayer player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(StatsData::getPlayerQuestData)
                .orElse(null);
    }
}
