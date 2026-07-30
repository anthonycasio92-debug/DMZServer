package com.dbzlegacy.adaptivedifficulty.client;

import com.dbzlegacy.adaptivedifficulty.client.gui.DifficultyScreen;
import com.dbzlegacy.adaptivedifficulty.network.OpenDifficultyScreenPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void openDifficulty(OpenDifficultyScreenPacket packet) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc == null) {
            return;
        }
        if (mc.f_91080_ instanceof DifficultyScreen existing && existing.samePage(packet.page)) {
            existing.apply(packet);
            return;
        }
        mc.m_91152_(new DifficultyScreen(packet));
    }
}
