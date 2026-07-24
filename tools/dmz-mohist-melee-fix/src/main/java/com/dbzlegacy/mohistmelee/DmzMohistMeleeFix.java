package com.dbzlegacy.mohistmelee;

import com.dbzlegacy.mohistmelee.RateLog;
import com.dbzlegacy.mohistmelee.RepairEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(value="dmz_mohist_melee_fix")
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";

    public DmzMohistMeleeFix() {
        MinecraftForge.EVENT_BUS.register((Object)new RepairEvents());
        RateLog.logger().info(
                "[{}] v2.12.0-fixed: 2.11.0-fixed base + intentional DMZ stat-reset snapshot adopt",
                (Object)MOD_ID
        );
    }
}
