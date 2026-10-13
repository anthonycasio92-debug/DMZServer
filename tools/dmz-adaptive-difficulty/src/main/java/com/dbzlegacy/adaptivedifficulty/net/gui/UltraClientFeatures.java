package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player Ultra GUI optional-feature advertisement (KaiPath → LM).
 *
 * <p>Known optional features LM already sends and expects clients to honor:
 *
 * <ul>
 *   <li>{@link #FEATURE_STYLE} — button {@code style} ({@code normal}/{@code confirm}/
 *       {@code danger}/{@code dim})
 *   <li>{@link #FEATURE_ACTIVE_TAB} — {@link GuiOpenPacket} field 5 {@code activeTab} highlight
 *   <li>{@link #FEATURE_TITLE_ALIGN} — {@code layoutJson.titleAlign} ({@code center}/{@code left})
 *   <li>{@link #FEATURE_PLAYER_MODEL} — {@code layoutJson.playerModel} entity preview
 *   <li>{@link #FEATURE_TEXT_INPUT} — GuiOpenPacket field 11 {@code inputsJson} text fields
 * </ul>
 *
 * <p>Clients that never send {@link GuiFeaturesPacket} are treated as the KaiPath <b>1.3.1
 * baseline</b>: dynamic layout/buttons only. KaiPath <b>1.3.7</b> advertises
 * style/activeTab/titleAlign/playerModel (still not {@link #FEATURE_TEXT_INPUT}).
 */
public final class UltraClientFeatures {
    public static final String FEATURE_STYLE = "style";
    public static final String FEATURE_ACTIVE_TAB = "activetab";
    public static final String FEATURE_TITLE_ALIGN = "titlealign";
    public static final String FEATURE_PLAYER_MODEL = "playermodel";
    public static final String FEATURE_TEXT_INPUT = "textinput";

    /** Wire / doc spellings accepted for {@link #FEATURE_ACTIVE_TAB}. */
    public static final String FEATURE_ACTIVE_TAB_DOC = "activeTab";
    /** Wire / doc spelling for {@link #FEATURE_TITLE_ALIGN}. */
    public static final String FEATURE_TITLE_ALIGN_DOC = "titleAlign";
    /** Wire / doc spelling for {@link #FEATURE_PLAYER_MODEL}. */
    public static final String FEATURE_PLAYER_MODEL_DOC = "playerModel";
    /** Wire / doc spelling for {@link #FEATURE_TEXT_INPUT}. */
    public static final String FEATURE_TEXT_INPUT_DOC = "textInput";

    /** Optional features the LM bridge expects a fully conforming client to advertise. */
    public static final Set<String> EXPECTED_OPTIONAL =
            Set.of(
                    FEATURE_STYLE,
                    FEATURE_ACTIVE_TAB,
                    FEATURE_TITLE_ALIGN,
                    FEATURE_PLAYER_MODEL,
                    FEATURE_TEXT_INPUT);

    /**
     * Features KaiPath <b>1.3.7</b> already advertises/honors. Missing any of these is a real
     * client gap (wrong jar / old client).
     */
    public static final Set<String> SHIPPED_ON_RECOMMENDED =
            Set.of(
                    FEATURE_STYLE,
                    FEATURE_ACTIVE_TAB,
                    FEATURE_TITLE_ALIGN,
                    FEATURE_PLAYER_MODEL);

    /**
     * Still pending on KaiPath after 1.3.7 — Weights shop + Config typed fields need this.
     * Reference kit: {@code tools/ultra-gui-kit/kaipath-client/}.
     */
    public static final Set<String> PENDING_NEXT = Set.of(FEATURE_TEXT_INPUT);

    private static final Map<UUID, State> BY_PLAYER = new ConcurrentHashMap<>();

    private UltraClientFeatures() {}

    public record State(boolean advertised, Set<String> features, String rawJson) {
        public State {
            features = features == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(features));
            rawJson = rawJson == null ? "[]" : rawJson;
        }

        public boolean has(String feature) {
            if (feature == null || feature.isBlank()) {
                return false;
            }
            String key = normalize(feature);
            return features.contains(key);
        }
    }

    /** Normalize feature ids ({@code activeTab} → {@code activetab}). */
    public static String normalize(String feature) {
        if (feature == null) {
            return "";
        }
        return feature.trim().toLowerCase(Locale.ROOT);
    }

    public static void recordAdvertisement(ServerPlayer player, Set<String> features, String rawJson) {
        if (player == null) {
            return;
        }
        Set<String> normalized = new LinkedHashSet<>();
        if (features != null) {
            for (String f : features) {
                String n = normalize(f);
                if (!n.isEmpty()) {
                    normalized.add(n);
                }
            }
        }
        State state = new State(true, normalized, rawJson == null ? "[]" : rawJson);
        BY_PLAYER.put(player.m_20148_(), state);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] Ultra client features ← player={} advertised=true features={} raw={}",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                normalized,
                state.rawJson());
    }

    /**
     * Current state. If the client never advertised, returns baseline (empty features,
     * {@code advertised=false}).
     */
    public static State get(ServerPlayer player) {
        if (player == null) {
            return new State(false, Set.of(), "[]");
        }
        return BY_PLAYER.getOrDefault(player.m_20148_(), new State(false, Set.of(), "[]"));
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            BY_PLAYER.remove(player.m_20148_());
        }
    }

    public static void clear(UUID id) {
        if (id != null) {
            BY_PLAYER.remove(id);
        }
    }

    /** Optional features from {@link #EXPECTED_OPTIONAL} the client does not report. */
    public static List<String> missingOptional(State state) {
        List<String> missing = new ArrayList<>();
        missing.addAll(missingShipped(state));
        missing.addAll(missingPending(state));
        return missing;
    }

    /** Gaps vs KaiPath 1.3.7 shipped set — warn on login/guitest. */
    public static List<String> missingShipped(State state) {
        List<String> missing = new ArrayList<>();
        if (state == null || !state.has(FEATURE_STYLE)) {
            missing.add(FEATURE_STYLE);
        }
        if (state == null || !state.has(FEATURE_ACTIVE_TAB)) {
            missing.add(FEATURE_ACTIVE_TAB_DOC);
        }
        if (state == null || !state.has(FEATURE_TITLE_ALIGN)) {
            missing.add(FEATURE_TITLE_ALIGN_DOC);
        }
        if (state == null || !state.has(FEATURE_PLAYER_MODEL)) {
            missing.add(FEATURE_PLAYER_MODEL_DOC);
        }
        return missing;
    }

    /** Gaps still expected after 1.3.7 (today: {@code textInput} only). */
    public static List<String> missingPending(State state) {
        List<String> missing = new ArrayList<>();
        if (state == null || !state.has(FEATURE_TEXT_INPUT)) {
            missing.add(FEATURE_TEXT_INPUT_DOC);
        }
        return missing;
    }

    public static boolean hasTextInput(ServerPlayer player) {
        return get(player).has(FEATURE_TEXT_INPUT);
    }

    /** Human-readable impact lines for missing features. */
    public static List<String> impactLines(List<String> missing) {
        List<String> lines = new ArrayList<>();
        if (missing == null) {
            return lines;
        }
        for (String m : missing) {
            String key = normalize(m);
            if (FEATURE_STYLE.equals(key)) {
                lines.add(
                        "all buttons render identically, no green/red/dim distinction "
                                + "(style=confirm/danger/dim ignored)");
            } else if (FEATURE_ACTIVE_TAB.equals(key)) {
                lines.add("active tab is not highlighted (GuiOpenPacket.activeTab ignored)");
            } else if (FEATURE_TITLE_ALIGN.equals(key)) {
                lines.add(
                        "titles left-align instead of layoutJson.titleAlign=center");
            } else if (FEATURE_PLAYER_MODEL.equals(key)) {
                lines.add(
                        "Character playerModel preview not drawn (layoutJson.playerModel ignored)");
            } else if (FEATURE_TEXT_INPUT.equals(key)) {
                lines.add(
                        "text fields ignored (GuiOpenPacket.inputsJson / bindInput not rendered)");
            }
        }
        return lines;
    }

    /**
     * Log when 1.3.7-shipped features are missing (real gap). Pending {@code textInput} is
     * logged at info so 1.3.7 clients are not warned as broken.
     */
    public static void warnMissingOptionalFeatures(ServerPlayer player, String context) {
        if (player == null) {
            return;
        }
        State state = get(player);
        List<String> shippedGaps = missingShipped(state);
        List<String> pending = missingPending(state);
        String version = resolveKaiPathVersion(player);
        String verLabel = version == null || version.isBlank() ? "<unknown>" : version;
        String ctx = context == null ? "?" : context;
        if (!shippedGaps.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra client gap (pre-1.3.7 features) player={} kaipathVersion={} "
                            + "context={} advertised={} features={} missingShipped={} — {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    verLabel,
                    ctx,
                    state.advertised(),
                    state.features().isEmpty() ? "[]" : state.features(),
                    shippedGaps,
                    impactLines(shippedGaps));
        }
        if (!pending.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra pending after KaiPath 1.3.7 player={} kaipathVersion={} "
                            + "context={} pending={} — Weights/Config typed fields need textInput",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    verLabel,
                    ctx,
                    pending);
        }
    }

    /** One-line summary for guitest / staff chat. */
    public static String summarizeForStaff(ServerPlayer player) {
        State state = get(player);
        List<String> shippedGaps = missingShipped(state);
        List<String> pending = missingPending(state);
        String version = resolveKaiPathVersion(player);
        StringBuilder sb = new StringBuilder();
        sb.append("features=");
        if (!state.advertised()) {
            sb.append("none (1.3.1 baseline — no GuiFeaturesPacket)");
        } else if (state.features().isEmpty()) {
            sb.append("[] (advertised empty)");
        } else {
            sb.append(state.features());
        }
        sb.append(" kaipath=").append(version == null || version.isBlank() ? "?" : version);
        sb.append(" missingShipped=")
                .append(shippedGaps.isEmpty() ? "none" : shippedGaps);
        sb.append(" pendingNext=").append(pending.isEmpty() ? "none" : pending);
        return sb.toString();
    }

    private static String resolveKaiPathVersion(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            // Reuse LM's Forge mod-list probe path via package-private helper when available.
            return LmGuiNetwork.probeKaiPathVersion(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeName(ServerPlayer player) {
        if (player == null) {
            return "<null>";
        }
        try {
            return player.m_7755_().getString();
        } catch (Throwable ignored) {
            return "<name?>";
        }
    }
}
