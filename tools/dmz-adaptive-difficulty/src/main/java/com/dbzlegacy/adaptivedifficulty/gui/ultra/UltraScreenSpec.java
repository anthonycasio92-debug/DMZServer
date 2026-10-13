package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.net.gui.GuiOpenPacket;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds a GuiOpenPacket payload for KaiPath.
 *
 * <p>Panel defaults are the compact Ultra dialog ({@link UltraStyle#PANEL_W}×{@link
 * UltraStyle#PANEL_H}), centered with a screen margin. Button action ids are qualified as
 * {@code @&lt;screenId&gt;:&lt;action&gt;} so KaiPath can omit or mishandle {@code screenId} and
 * the server still routes correctly.
 *
 * <p>Layout JSON includes shared body/button metrics ({@code lineHeight}, {@code lineGap},
 * {@code bodyMaxHeight}, {@code scrollBody}, {@code buttonPad}, …) so every screen benefits.
 * Optional {@link #playerModel} declares a client-rendered entity preview (JLDK implements
 * rendering; unknown clients ignore the field).
 */
public final class UltraScreenSpec {
    private static final Gson GSON = new Gson();

    /** When set, {@link #send} captures packets instead of transmitting (startup self-test). */
    private static final ThreadLocal<List<GuiOpenPacket>> PROBE = new ThreadLocal<>();

    private final String screenId;
    private String title = "";
    private String pageId = "main";
    private String activeTab = "home";
    private final List<Tab> tabs = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private final List<Input> inputs = new ArrayList<>();
    /** Compact title + tab row; content starts immediately below. */
    private int nextRow = UltraStyle.CONTENT_ROW;
    private int panelWidth = UltraStyle.PANEL_W;
    private int panelHeight = UltraStyle.PANEL_H;
    private boolean centerPanel = true;
    private int panelMargin = UltraStyle.PANEL_MARGIN;
    private PlayerModel playerModel;
    /** Lowest button top Y seen so far; used to size the body scroll region. */
    private int firstButtonY = -1;

    private UltraScreenSpec(String screenId) {
        this.screenId = screenId == null || screenId.isBlank() ? "hub" : screenId.trim();
    }

    public static UltraScreenSpec of(String screenId) {
        return new UltraScreenSpec(screenId);
    }

    /**
     * Run {@code work} while capturing every {@link #send} into a list (no network).
     * Used by {@link UltraScreenSelfTest}.
     */
    public static List<GuiOpenPacket> captureSends(Runnable work) {
        List<GuiOpenPacket> captured = new ArrayList<>();
        PROBE.set(captured);
        try {
            if (work != null) {
                work.run();
            }
        } finally {
            PROBE.remove();
        }
        return captured;
    }

    public UltraScreenSpec title(String title) {
        this.title = UltraStyle.header(title);
        return this;
    }

    public UltraScreenSpec page(String pageId) {
        this.pageId = pageId == null ? "main" : pageId;
        return this;
    }

    public UltraScreenSpec activeTab(String tabId) {
        this.activeTab = tabId == null ? "home" : tabId;
        return this;
    }

    /** Explicit panel size in GUI pixels (DMZUltra-style centered dialog). */
    public UltraScreenSpec panel(int width, int height) {
        this.panelWidth = Math.max(UltraStyle.PANEL_MIN_W, width);
        this.panelHeight = Math.max(UltraStyle.PANEL_MIN_H, height);
        return this;
    }

    public UltraScreenSpec center(boolean center) {
        this.centerPanel = center;
        return this;
    }

    public UltraScreenSpec margin(int margin) {
        this.panelMargin = Math.max(0, margin);
        return this;
    }

    /**
     * Optional client-rendered player model preview. Unknown clients ignore this field;
     * layout and buttons still work. JLDK draws the entity; LM only declares bounds.
     */
    public UltraScreenSpec playerModel(
            int x, int y, int width, int height, float scale, boolean rotate) {
        return playerModel(x, y, width, height, scale, rotate, null);
    }

    /**
     * Player model preview with an optional {@code parts} snapshot (head bones, form identity,
     * extras). See {@link PlayerModelParts} and {@code docs/KAIPATH_GUI_PROTOCOL.md}.
     */
    public UltraScreenSpec playerModel(
            int x,
            int y,
            int width,
            int height,
            float scale,
            boolean rotate,
            JsonObject parts) {
        this.playerModel = new PlayerModel(
                x,
                y,
                Math.max(16, width),
                Math.max(16, height),
                scale <= 0f ? 1f : scale,
                rotate,
                parts);
        return this;
    }

    /**
     * Bounds plus a live {@link PlayerModelParts#snapshot} for {@code player} (Character
     * Services).
     */
    public UltraScreenSpec playerModel(
            ServerPlayer player,
            int x,
            int y,
            int width,
            int height,
            float scale,
            boolean rotate) {
        return playerModel(x, y, width, height, scale, rotate, PlayerModelParts.snapshot(player));
    }

    /** Clear any previously set player model (default: off). */
    public UltraScreenSpec clearPlayerModel() {
        this.playerModel = null;
        return this;
    }

    public UltraScreenSpec tabs(String... idLabelPairs) {
        tabs.clear();
        if (idLabelPairs == null) {
            return this;
        }
        for (String pair : idLabelPairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int bar = pair.indexOf('|');
            if (bar <= 0) {
                tabs.add(new Tab(pair, pair));
            } else {
                tabs.add(new Tab(pair.substring(0, bar), pair.substring(bar + 1)));
            }
        }
        // Content starts below title + tab strip when tabs are present.
        if (!tabs.isEmpty() && nextRow < UltraStyle.CONTENT_ROW_TABS) {
            nextRow = UltraStyle.CONTENT_ROW_TABS;
        }
        return this;
    }

    public UltraScreenSpec line(String text) {
        if (text != null && !text.isBlank()) {
            lines.add(text);
        }
        return this;
    }

    public UltraScreenSpec labelValue(String label, String value) {
        return line(UltraStyle.labelValue(label, value));
    }

    public UltraScreenSpec notice(String body) {
        return line(UltraStyle.notice(body));
    }

    public UltraScreenSpec button(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec buttonLeft(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec buttonRight(String actionId, String label, String style) {
        return button(actionId, label, style, UltraStyle.COL_R, nextRow, UltraStyle.BTN_W);
    }

    public UltraScreenSpec button(
            String actionId, String label, String style, int x, int y, int w) {
        return button(actionId, label, style, x, y, w, null);
    }

    /**
     * Button that asks KaiPath to copy a text field's current value into {@code args.value}
     * before sending {@link com.dbzlegacy.adaptivedifficulty.net.gui.GuiActionPacket}.
     * {@code bindInputId} is the same id passed to {@link #textField}.
     */
    public UltraScreenSpec buttonBound(
            String actionId, String label, String style, String bindInputId) {
        return button(
                actionId, label, style, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W, bindInputId);
    }

    public UltraScreenSpec button(
            String actionId, String label, String style, int x, int y, int w, String bindInputId) {
        UltraScreenIds.requireValidActionId(screenId, actionId, label);
        int width = Math.max(UltraStyle.BTN_MIN_W, w);
        String resolvedStyle = normalizeStyle(style);
        if (firstButtonY < 0 || y < firstButtonY) {
            firstButtonY = y;
        }
        String bind = null;
        if (bindInputId != null && !bindInputId.isBlank()) {
            UltraScreenIds.requireValidActionId(screenId, bindInputId, "bindInput");
            bind = qualifyAction(bindInputId);
        }
        buttons.add(new Button(
                qualifyAction(actionId),
                UltraStyle.buttonLabel(label),
                resolvedStyle,
                x,
                y,
                width,
                UltraStyle.BTN_H,
                UltraStyle.BTN_PAD,
                bind));
        return this;
    }

    public UltraScreenSpec rowGap() {
        nextRow += UltraStyle.BTN_H + UltraStyle.BTN_GAP;
        return this;
    }

    public UltraScreenSpec setRow(int y) {
        nextRow = y;
        return this;
    }

    public int row() {
        return nextRow;
    }

    /** Home tab footer: back to main menu (no Refresh — pages reopen via actions/tabs). */
    public UltraScreenSpec footerBackToMain() {
        return button(
                "nav:hub",
                UltraStyle.BACK_TO_MAIN,
                UltraStyle.STYLE_DIM,
                UltraStyle.COL_L,
                nextRow,
                UltraStyle.BTN_W);
    }

    /** Sub-page footer: back to system home (no Refresh). */
    public UltraScreenSpec footerBackHome() {
        return button(
                "nav:home", UltraStyle.BACK, UltraStyle.STYLE_DIM, UltraStyle.COL_L, nextRow, UltraStyle.BTN_W);
    }

    /**
     * Editable text field (GuiOpenPacket field 11 {@code inputsJson}). KaiPath <b>1.3.7</b>
     * still does not render or advertise {@code textInput}; clients that do advertise it
     * render the field. Older / current clients ignore trailing field 11.
     *
     * <p>Pair with a button whose {@code args.bindInput} matches {@code inputId} so the client
     * copies the typed value into {@code args.value} on click / Enter.
     */
    public UltraScreenSpec textField(
            String inputId,
            String value,
            String placeholder,
            int x,
            int y,
            int w,
            int h,
            int maxLength) {
        UltraScreenIds.requireValidActionId(screenId, inputId, "text");
        if (firstButtonY < 0 || y < firstButtonY) {
            // Treat inputs like interactive rows for body/button packing.
            firstButtonY = y;
        }
        inputs.add(
                new Input(
                        qualifyAction(inputId),
                        "text",
                        value == null ? "" : value,
                        placeholder == null ? "" : placeholder,
                        x,
                        y,
                        Math.max(40, w),
                        Math.max(14, h),
                        Math.max(1, Math.min(256, maxLength <= 0 ? 64 : maxLength))));
        return this;
    }

    /** Full-width text field on the current row (left column). */
    public UltraScreenSpec textFieldRow(String inputId, String value, String placeholder) {
        return textField(
                inputId,
                value,
                placeholder,
                UltraStyle.COL_L,
                nextRow,
                UltraStyle.PANEL_W - UltraStyle.COL_L * 2,
                UltraStyle.BTN_H,
                128);
    }

    /** Build the wire packet without sending. */
    public GuiOpenPacket toPacket() {
        int contentBottom = nextRow + UltraStyle.BTN_H + UltraStyle.PANEL_MARGIN;
        int height = panelHeight;
        if (contentBottom > height) {
            height = Math.min(UltraStyle.PANEL_MAX_H, contentBottom);
        }

        int bodyTop = tabs.isEmpty() ? UltraStyle.BODY_TOP : UltraStyle.BODY_TOP_TABS;
        int lineStep = UltraStyle.LINE_H + UltraStyle.LINE_GAP;
        int naturalBodyH =
                lines.isEmpty()
                        ? 0
                        : UltraStyle.BODY_PAD * 2 + lines.size() * lineStep;

        // Grow toward PANEL_MAX_H when body text needs more viewport than the default
        // BODY_MIN_H band — shift buttons down so extra height expands the body, not
        // empty space under the footer.
        int buttonTop = firstButtonY;
        int buttonStackH =
                buttonTop < 0
                        ? UltraStyle.PANEL_MARGIN
                        : Math.max(
                                UltraStyle.BTN_H + UltraStyle.PANEL_MARGIN,
                                contentBottom - buttonTop);
        int maxBodyAtCap =
                Math.max(
                        UltraStyle.BODY_MIN_H,
                        UltraStyle.PANEL_MAX_H - bodyTop - buttonStackH);
        int targetBodyH =
                Math.max(
                        UltraStyle.BODY_MIN_H,
                        Math.min(Math.max(naturalBodyH, UltraStyle.BODY_MIN_H), maxBodyAtCap));
        int targetHeight = bodyTop + targetBodyH + buttonStackH;
        if (targetHeight > height) {
            height = Math.min(UltraStyle.PANEL_MAX_H, targetHeight);
        }

        int buttonShift = 0;
        if (buttonTop >= 0) {
            int currentBodyRoom = Math.max(0, buttonTop - UltraStyle.BTN_GAP - bodyTop);
            if (targetBodyH > currentBodyRoom) {
                int need = targetBodyH - currentBodyRoom;
                int slack = Math.max(0, height - contentBottom);
                buttonShift = Math.min(need, slack);
                // If still short of targetBodyH, grow panel once more (capped) then shift.
                if (buttonShift < need && height < UltraStyle.PANEL_MAX_H) {
                    int grow = Math.min(need - buttonShift, UltraStyle.PANEL_MAX_H - height);
                    height += grow;
                    slack = Math.max(0, height - contentBottom);
                    buttonShift = Math.min(need, slack);
                }
            }
        }

        // Body region sits under title/tabs and above the first button row (or panel bottom).
        int effectiveButtonTop = buttonTop < 0 ? -1 : buttonTop + buttonShift;
        int bodyBottom =
                effectiveButtonTop < 0
                        ? height - UltraStyle.PANEL_MARGIN
                        : Math.min(
                                effectiveButtonTop - UltraStyle.BTN_GAP,
                                height - UltraStyle.PANEL_MARGIN);
        int bodyMaxH =
                Math.max(
                        UltraStyle.BODY_MIN_H,
                        Math.max(lineStep + UltraStyle.BODY_PAD * 2, bodyBottom - bodyTop));
        boolean scrollBody =
                naturalBodyH > bodyMaxH || contentBottom + buttonShift > UltraStyle.PANEL_MAX_H;

        JsonArray tabArr = new JsonArray();
        for (Tab tab : tabs) {
            if (tab == null || tab.id == null) {
                continue;
            }
            UltraScreenIds.requireValidActionId(screenId, "tab:" + tab.id, tab.label);
            JsonObject o = new JsonObject();
            o.addProperty("id", qualifyAction("tab:" + tab.id));
            boolean active = tab.id.equalsIgnoreCase(activeTab);
            o.addProperty(
                    "label",
                    active
                            ? UltraStyle.HEADER + UltraStyle.plain(tab.label)
                            : UltraStyle.LABEL + UltraStyle.plain(tab.label));
            tabArr.add(o);
        }
        JsonArray lineArr = new JsonArray();
        for (String line : lines) {
            if (line != null) {
                lineArr.add(line);
            }
        }
        JsonArray btnArr = new JsonArray();
        for (Button b : buttons) {
            if (b == null) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", b.actionId);
            o.addProperty("label", b.label);
            o.addProperty("style", b.style);
            o.addProperty("x", b.x);
            o.addProperty("y", b.y + buttonShift);
            o.addProperty("w", b.w);
            o.addProperty("h", b.h);
            o.addProperty("pad", b.pad);
            JsonObject args = new JsonObject();
            // Logical system — echoed by KaiPath into GuiActionPacket even when wire screenId is hub.
            args.addProperty("screen", screenId);
            args.addProperty(UltraScreenIds.META_SYSTEM, screenId);
            args.addProperty("page", pageId);
            if (b.bindInput != null && !b.bindInput.isBlank()) {
                args.addProperty("bindInput", b.bindInput);
            }
            o.add("args", args);
            btnArr.add(o);
        }
        JsonArray inputArr = new JsonArray();
        for (Input in : inputs) {
            if (in == null || in.id == null || in.id.isBlank()) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", in.id);
            o.addProperty("type", in.type);
            o.addProperty("x", in.x);
            o.addProperty("y", in.y + buttonShift);
            o.addProperty("w", in.w);
            o.addProperty("h", in.h);
            o.addProperty("value", in.value);
            o.addProperty("placeholder", in.placeholder);
            o.addProperty("maxLength", in.maxLength);
            JsonObject args = new JsonObject();
            args.addProperty("screen", screenId);
            args.addProperty(UltraScreenIds.META_SYSTEM, screenId);
            args.addProperty("page", pageId);
            o.add("args", args);
            inputArr.add(o);
        }
        JsonObject layout = new JsonObject();
        layout.addProperty("panelWidth", panelWidth);
        layout.addProperty("panelHeight", height);
        layout.addProperty("width", panelWidth);
        layout.addProperty("height", height);
        layout.addProperty("center", centerPanel);
        layout.addProperty("margin", panelMargin);
        // Always present — KaiPath must center titles when value is "center" (not left-default).
        layout.addProperty("titleAlign", UltraStyle.TITLE_ALIGN);
        // Shared body / button metrics — every screen benefits.
        layout.addProperty("lineHeight", UltraStyle.LINE_H);
        layout.addProperty("lineGap", UltraStyle.LINE_GAP);
        layout.addProperty("bodyTop", bodyTop);
        layout.addProperty("bodyPad", UltraStyle.BODY_PAD);
        layout.addProperty("bodyMaxHeight", bodyMaxH);
        layout.addProperty("scrollBody", scrollBody);
        layout.addProperty("buttonPad", UltraStyle.BTN_PAD);
        layout.addProperty("buttonGap", UltraStyle.BTN_GAP);
        layout.addProperty("buttonMinWidth", UltraStyle.BTN_MIN_W);
        layout.addProperty("buttonHeight", UltraStyle.BTN_H);
        // Logical system metadata (survives hub wire shim in layoutJson).
        layout.addProperty(UltraScreenIds.META_SYSTEM, screenId);
        layout.addProperty("screen", screenId);
        if (playerModel != null) {
            JsonObject pm = new JsonObject();
            pm.addProperty("x", playerModel.x);
            pm.addProperty("y", playerModel.y);
            pm.addProperty("width", playerModel.width);
            pm.addProperty("height", playerModel.height);
            pm.addProperty("scale", playerModel.scale);
            pm.addProperty("rotate", playerModel.rotate);
            if (playerModel.parts != null && !playerModel.parts.entrySet().isEmpty()) {
                pm.add("parts", playerModel.parts);
            }
            layout.add("playerModel", pm);
        }
        return new GuiOpenPacket(
                screenId,
                title,
                pageId,
                GSON.toJson(tabArr),
                activeTab,
                GSON.toJson(lineArr),
                GSON.toJson(btnArr),
                panelWidth,
                height,
                GSON.toJson(layout),
                GSON.toJson(inputArr));
    }

    public void send(ServerPlayer player) {
        GuiOpenPacket packet = toPacket();
        List<GuiOpenPacket> probe = PROBE.get();
        if (probe != null) {
            probe.add(packet);
            return;
        }
        if (player == null) {
            return;
        }
        LmGuiNetwork.sendOpen(player, packet);
    }

    /** Prefix {@code @screenId:} unless the action is already qualified. */
    String qualifyAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return "@" + screenId + ":nav:home";
        }
        String raw = actionId.trim();
        if (raw.startsWith("@")) {
            return raw;
        }
        return "@" + screenId + ":" + raw;
    }

    private static String normalizeStyle(String style) {
        if (style == null || style.isBlank()) {
            return UltraStyle.STYLE_NORMAL;
        }
        String s = style.trim().toLowerCase();
        return switch (s) {
            case "confirm" -> UltraStyle.STYLE_CONFIRM;
            case "danger" -> UltraStyle.STYLE_DANGER;
            case "dim" -> UltraStyle.STYLE_DIM;
            case "normal" -> UltraStyle.STYLE_NORMAL;
            default -> UltraStyle.STYLE_NORMAL;
        };
    }

    private record Tab(String id, String label) {}

    private record Button(
            String actionId,
            String label,
            String style,
            int x,
            int y,
            int w,
            int h,
            int pad,
            String bindInput) {}

    private record Input(
            String id,
            String type,
            String value,
            String placeholder,
            int x,
            int y,
            int w,
            int h,
            int maxLength) {}

    private record PlayerModel(
            int x, int y, int width, int height, float scale, boolean rotate, JsonObject parts) {}
}
