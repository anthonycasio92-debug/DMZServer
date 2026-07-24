package com.dbzlegacy.mohistmelee;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";

    public DmzMohistMeleeFix() {
        MinecraftForge.EVENT_BUS.register(new RepairEvents());
        ResetSelfTest.registerIfEnabled();
        RateLog.logger().info(
                "[{}] v2.12.1: optimized 2.11.0-fixed layout + hardened intentional DMZ stat-reset adopt",
                MOD_ID
        );
    }
}
