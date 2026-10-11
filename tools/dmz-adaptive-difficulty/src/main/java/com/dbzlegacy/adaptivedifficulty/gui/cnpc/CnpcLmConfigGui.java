package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import net.minecraft.server.level.ServerPlayer;

/** Opens the reflection config editor. Kept so older command routes still land here. */
public final class CnpcLmConfigGui {
    private CnpcLmConfigGui() {}

    public static void open(ServerPlayer player, String page) {
        CnpcConfigEditor.open(player, page);
    }
}
