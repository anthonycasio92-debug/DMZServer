package com.dbzlegacy.adaptivedifficulty.quest;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Paid saga replay. The menu is whatever {@link QuestRegistry#getAllSagas()} currently loaded
 * (vanilla story plus datapack sagas). Cost is {@link SagaResetConfig#costFor}.
 */
public final class SagaResetService {
    private SagaResetService() {}

    public static List<String> cards(ServerPlayer player) {
        List<String> cards = new ArrayList<>();
        StatsData stats = DmzProgression.stats(player);
        PlayerQuestData data = stats == null ? null : stats.getPlayerQuestData();
        for (Saga saga : loadedSagas()) {
            String id = saga.getId();
            if (id == null || id.isBlank()) {
                continue;
            }
            int total = saga.getQuests() == null ? 0 : saga.getQuests().size();
            int started = data == null ? 0 : startedCount(data, id);
            long cost = PaidFeatureAccess.bypassAncientCoinCost(player)
                    ? 0L
                    : SagaResetConfig.get().costFor(id);
            cards.add(id + "\t" + displayName(saga) + "\t" + started + "\t" + total + "\t" + cost);
        }
        return cards;
    }

    public static List<String> confirmLines(ServerPlayer player, String sagaId) {
        Saga saga = find(sagaId);
        List<String> lines = new ArrayList<>();
        if (saga == null) {
            lines.add("That saga is not loaded.");
            return lines;
        }
        StatsData stats = DmzProgression.stats(player);
        PlayerQuestData data = stats == null ? null : stats.getPlayerQuestData();
        int total = saga.getQuests() == null ? 0 : saga.getQuests().size();
        int started = data == null ? 0 : startedCount(data, saga.getId());
        lines.add(displayName(saga));
        lines.add(started + " of " + total + " quests on your record");
        if (PaidFeatureAccess.bypassAncientCoinCost(player)) {
            lines.add("No Ancient Coin charge");
        } else {
            lines.add(AncientCoinEconomy.formatExactCost(SagaResetConfig.get().costFor(saga.getId())));
        }
        lines.add("Clears your progress in this saga. Rewards you already received stay.");
        if (data != null && data.isInParty()) {
            lines.add("Leave your quest party before resetting.");
        } else if (started <= 0) {
            lines.add("You have not started this saga.");
        }
        return lines;
    }

    /**
     * @return a notice line, or null when the reset completed
     */
    public static String reset(ServerPlayer player, String sagaId) {
        if (player == null) {
            return "Could not reset that saga.";
        }
        if (!SagaResetConfig.get().enabled) {
            return "Saga reset is turned off.";
        }
        Saga saga = find(sagaId);
        if (saga == null || saga.getId() == null) {
            return "That saga is not loaded.";
        }
        StatsData stats = DmzProgression.stats(player);
        if (stats == null || stats.getPlayerQuestData() == null) {
            return "Your quest data is not loaded yet. Try again in a moment.";
        }
        PlayerQuestData data = stats.getPlayerQuestData();
        if (data.isInParty()) {
            return "Leave your quest party before resetting a saga.";
        }
        String id = saga.getId();
        if (startedCount(data, id) <= 0) {
            return "You have not started " + displayName(saga) + ".";
        }
        long cost = SagaResetConfig.get().costFor(id);
        boolean free = PaidFeatureAccess.bypassAncientCoinCost(player);
        if (!free && !AncientCoinEconomy.canAfford(player, cost)) {
            return AncientCoinEconomy.missingText(player, cost);
        }
        if (!free && !AncientCoinEconomy.charge(player, cost)) {
            return "Could not take the Ancient Coins.";
        }
        data.resetSaga(id);
        discardOwnedSagaEntities(player, id);
        try {
            QuestService.syncQuestState(player);
        } catch (Throwable ignored) {
        }
        String paid = free ? "" : " Paid " + AncientCoinEconomy.formatExactCost(cost) + ".";
        return "Reset " + displayName(saga) + "." + paid;
    }

    public static String displayName(Saga saga) {
        if (saga == null) {
            return "Saga";
        }
        String raw = saga.getName();
        if (raw == null || raw.isBlank()) {
            raw = saga.getId();
        }
        if (raw == null || raw.isBlank()) {
            return "Saga";
        }
        String name = raw;
        if (name.startsWith("dmz.")) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                name = name.substring(dot + 1);
            }
        }
        name = name.replace('_', ' ').trim();
        if (name.isEmpty()) {
            return saga.getId();
        }
        StringBuilder out = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static List<Saga> loadedSagas() {
        List<Saga> sagas = new ArrayList<>();
        try {
            Map<String, Saga> all = QuestRegistry.getAllSagas();
            if (all != null) {
                sagas.addAll(all.values());
            }
        } catch (Throwable ignored) {
        }
        sagas.removeIf(saga -> saga == null || saga.getId() == null || saga.getId().isBlank());
        sagas.sort(Comparator.comparing(SagaResetService::displayName, String.CASE_INSENSITIVE_ORDER));
        return sagas;
    }

    private static Saga find(String sagaId) {
        if (sagaId == null || sagaId.isBlank()) {
            return null;
        }
        for (Saga saga : loadedSagas()) {
            if (saga.getId().equalsIgnoreCase(sagaId.trim())) {
                return saga;
            }
        }
        return null;
    }

    private static int startedCount(PlayerQuestData data, String sagaId) {
        String prefix = sagaId + ":";
        Set<String> keys = new LinkedHashSet<>();
        addMatching(keys, data.getAcceptedQuestIds(), prefix);
        addMatching(keys, data.getCompletedQuestIds(), prefix);
        addMatching(keys, data.getFailedQuestIds(), prefix);
        return keys.size();
    }

    private static void addMatching(Set<String> keys, Set<String> source, String prefix) {
        if (source == null) {
            return;
        }
        for (String key : source) {
            if (key != null && key.startsWith(prefix)) {
                keys.add(key);
            }
        }
    }

    /** Remove this player's live quest mobs for the saga. Other players' mobs stay. */
    private static void discardOwnedSagaEntities(ServerPlayer player, String sagaId) {
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        String owner = player.m_20148_().toString();
        String prefix = sagaId + ":";
        for (ServerLevel level : server.m_129785_()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity entity : level.m_8583_()) {
                if (entity == null || entity instanceof ServerPlayer) {
                    continue;
                }
                CompoundTag tag = PersistentDataAccess.get(entity);
                if (!PersistentDataAccess.isWritable(tag)) {
                    continue;
                }
                if (!owner.equals(tag.m_128461_("dmz_quest_owner"))) {
                    continue;
                }
                String saga = tag.m_128461_("dmz_saga_id");
                String questKey = tag.m_128461_("dmz_quest_key");
                if (sagaId.equals(saga) || (questKey != null && questKey.startsWith(prefix))) {
                    doomed.add(entity);
                }
            }
            for (Entity entity : doomed) {
                entity.m_146870_();
            }
        }
    }
}
