package com.dbzlegacy.adaptivedifficulty.client.gui;

import com.dbzlegacy.adaptivedifficulty.network.DifficultyActionPacket;
import com.dbzlegacy.adaptivedifficulty.network.DifficultyNet;
import com.dbzlegacy.adaptivedifficulty.network.OpenDifficultyScreenPacket;
import com.dbzlegacy.adaptivedifficulty.network.RequestOpenDifficultyPacket;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * SDU-style Adaptive Difficulty hub screen.
 * Buttons send C2S actions; server replies with a refreshed Open packet.
 */
public final class DifficultyScreen extends Screen {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 220;

    private OpenDifficultyScreenPacket data;
    private int left;
    private int top;

    public DifficultyScreen(OpenDifficultyScreenPacket data) {
        super(Component.m_237113_("Adaptive Difficulty"));
        this.data = data;
    }

    public boolean samePage(String page) {
        return data != null && data.page.equalsIgnoreCase(page == null ? "main" : page);
    }

    public void apply(OpenDifficultyScreenPacket packet) {
        this.data = packet;
        m_6574_(this.f_96541_, this.f_96543_, this.f_96544_);
    }

    @Override
    protected void m_7856_() {
        super.m_7856_();
        this.left = (this.f_96543_ - PANEL_W) / 2;
        this.top = (this.f_96544_ - PANEL_H) / 2;
        m_169413_();

        String page = data.page == null ? "main" : data.page.toLowerCase();
        if ("rewards".equals(page)) {
            initRewards();
        } else if ("tiers".equals(page) || "enemies".equals(page)) {
            initTiers();
        } else if ("stats".equals(page) || "statistics".equals(page)) {
            initStats();
        } else {
            initMain();
        }
    }

    private void initMain() {
        int y = top + PANEL_H - 78;
        addBtn(left + 12, y, 70, 20, "§a▲ +100", () -> action(DifficultyActions.ACT_UP, 100));
        addBtn(left + 88, y, 70, 20, "§c▼ -100", () -> action(DifficultyActions.ACT_DOWN, 100));
        addBtn(left + 164, y, 70, 20, "§eMax", () -> action(DifficultyActions.ACT_SET_MAX, 0));
        addBtn(left + 240, y, 68, 20, "§bTeam", () -> action(DifficultyActions.ACT_TEAM, 0));

        y += 24;
        addBtn(left + 12, y, 96, 20, "§6Buy +100", () -> action(DifficultyActions.ACT_BUY, 100));
        addBtn(left + 114, y, 96, 20, "§6Buy +1k", () -> action(DifficultyActions.ACT_BUY, 1000));
        addBtn(left + 216, y, 92, 20, "§6Buy +10k", () -> action(DifficultyActions.ACT_BUY, 10000));

        y += 24;
        addBtn(left + 12, y, 70, 20, "Rewards", () -> requestPage("rewards"));
        addBtn(left + 88, y, 70, 20, "Tiers", () -> requestPage("tiers"));
        addBtn(left + 164, y, 70, 20, "Stats", () -> requestPage("stats"));
        addBtn(left + 240, y, 68, 20, "Close", this::m_7379_);
    }

    private void initRewards() {
        addBtn(left + 12, top + PANEL_H - 30, 80, 20, "« Back", () -> requestPage("main"));
        addBtn(left + PANEL_W - 80, top + PANEL_H - 30, 68, 20, "Close", this::m_7379_);
    }

    private void initTiers() {
        addBtn(left + 12, top + PANEL_H - 30, 80, 20, "« Back", () -> requestPage("main"));
        addBtn(left + PANEL_W - 80, top + PANEL_H - 30, 68, 20, "Close", this::m_7379_);
    }

    private void initStats() {
        addBtn(left + 12, top + PANEL_H - 30, 80, 20, "« Back", () -> requestPage("main"));
        addBtn(left + PANEL_W - 80, top + PANEL_H - 30, 68, 20, "Close", this::m_7379_);
    }

    private void addBtn(int x, int y, int w, int h, String label, Runnable onPress) {
        m_142416_(Button.m_253074_(Component.m_237113_(label), b -> onPress.run())
                .m_252987_(x, y, w, h)
                .m_253136_());
    }

    private void action(String action, long amount) {
        DifficultyNet.sendToServer(new DifficultyActionPacket(action, amount, data.page));
    }

    private void requestPage(String page) {
        DifficultyNet.sendToServer(new RequestOpenDifficultyPacket(page));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        m_280273_(graphics);
        int x1 = left;
        int y1 = top;
        int x2 = left + PANEL_W;
        int y2 = top + PANEL_H;

        // Dark panel
        graphics.m_280509_(x1, y1, x2, y2, 0xE0101218);
        graphics.m_280509_(x1, y1, x2, y1 + 22, 0xF0181C28);
        graphics.m_280509_(x1, y1, x2, y1 + 1, 0xFFD4A017);
        graphics.m_280509_(x1, y2 - 1, x2, y2, 0xFFD4A017);

        String page = data.page == null ? "main" : data.page.toLowerCase();
        String title = "Adaptive Difficulty";
        if ("rewards".equals(page)) {
            title = "Rewards";
        } else if ("tiers".equals(page) || "enemies".equals(page)) {
            title = "Enemy Tiers";
        } else if ("stats".equals(page) || "statistics".equals(page)) {
            title = "Statistics";
        }
        graphics.m_280488_(this.f_96547_, title, x1 + 12, y1 + 7, 0xFFE8C36A);

        int y = y1 + 30;
        if ("rewards".equals(page)) {
            drawRewards(graphics, y);
        } else if ("tiers".equals(page) || "enemies".equals(page)) {
            drawTiers(graphics, y);
        } else if ("stats".equals(page) || "statistics".equals(page)) {
            drawStats(graphics, y);
        } else {
            drawMain(graphics, y);
        }

        super.m_88315_(graphics, mouseX, mouseY, partialTick);
    }

    private void drawMain(GuiGraphics graphics, int y) {
        int x = left + 14;
        line(graphics, x, y, "Active", String.valueOf(data.active) + " / " + data.availableMax, colorFromCode(data.stateColor));
        y += 12;
        line(graphics, x, y, "Calculated", String.valueOf(data.calculated) + "  (Lv " + data.dmzLevel + " · Prestige " + data.prestige + ")", 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Purchased", String.valueOf(data.purchased), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Personal Max", String.valueOf(data.personalMax), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Team Bonus", data.teamThresholdBonus + "  |  Contribution " + data.teamContribution, 0xFFB8A0E0);
        y += 12;
        line(graphics, x, y, "Team", data.teamName + "  (" + data.teammateCount + " online via " + data.teamSource + ")", 0xFF8FD3FF);
        y += 12;
        line(graphics, x, y, "Team Mode", data.teamMode, 0xFF8FD3FF);
        y += 12;
        line(graphics, x, y, "Enemy Tier", data.enemyTier, 0xFFFFB070);
        y += 12;
        line(graphics, x, y, "Balance", data.balanceText + "  (" + data.currencyLabel + ")", 0xFFE8C36A);
        y += 14;
        graphics.m_280488_(this.f_96547_, "Buy costs: +100 " + data.cost100Text
                + "   +1k " + data.cost1kText
                + "   +10k " + data.cost10kText, x, y, 0xFF9AA0A8);
    }

    private void drawRewards(GuiGraphics graphics, int y) {
        int x = left + 14;
        double mult = 1.0 + (data.active / 1000.0);
        line(graphics, x, y, "Active", String.valueOf(data.active), 0xFFE8C36A);
        y += 14;
        line(graphics, x, y, "TP Multiplier", "x" + String.format("%.2f", mult), 0xFFCCCCCC);
        y += 14;
        graphics.m_280488_(this.f_96547_, "Formula: 1 + Difficulty / RewardScaling", x, y, 0xFF9AA0A8);
        y += 12;
        graphics.m_280488_(this.f_96547_, "Elite kills grant bonus TP. Bosses scale harder.", x, y, 0xFF9AA0A8);
    }

    private void drawTiers(GuiGraphics graphics, int y) {
        int x = left + 14;
        line(graphics, x, y, "Current", data.enemyTier + " @ " + data.active, 0xFFE8C36A);
        y += 14;
        String[] rows = {
                "100 Awakened",
                "500 Enhanced",
                "1000 Elite",
                "2500 Advanced",
                "5000 Master",
                "10000 Legendary",
                "25000 God",
                "50000 Divine",
                "100000 Impossible"
        };
        for (String row : rows) {
            graphics.m_280488_(this.f_96547_, "· " + row, x, y, 0xFFCCCCCC);
            y += 11;
        }
    }

    private void drawStats(GuiGraphics graphics, int y) {
        int x = left + 14;
        line(graphics, x, y, "DMZ Level", String.valueOf(data.dmzLevel), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Prestige", String.valueOf(data.prestige), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Calculated", String.valueOf(data.calculated), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Purchased", String.valueOf(data.purchased), 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Personal / Available", data.personalMax + " / " + data.availableMax, 0xFFCCCCCC);
        y += 12;
        line(graphics, x, y, "Active", String.valueOf(data.active), colorFromCode(data.stateColor));
        y += 12;
        line(graphics, x, y, "Currency", data.currencyLabel + " · " + data.balanceText, 0xFFE8C36A);
        y += 12;
        line(graphics, x, y, "Teams", data.teamSource + " · " + data.teamName, 0xFF8FD3FF);
    }

    private void line(GuiGraphics graphics, int x, int y, String label, String value, int valueColor) {
        graphics.m_280488_(this.f_96547_, label + ":", x, y, 0xFF8A9098);
        graphics.m_280488_(this.f_96547_, value, x + 96, y, valueColor);
    }

    private static int colorFromCode(String code) {
        if (code == null) {
            return 0xFFFFFFFF;
        }
        return switch (code) {
            case "a" -> 0xFF55FF55;
            case "e" -> 0xFFFFFF55;
            case "6" -> 0xFFFFAA00;
            case "d" -> 0xFFFF55FF;
            case "c" -> 0xFFFF5555;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public boolean m_7043_() {
        return true;
    }
}
