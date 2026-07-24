package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist + DMZ melee fix: cancelled ki/strike/no-PvP can brick M1 until death.
 * v1.0.5 clears charge/block flags and syncs them to the client, flushes stuck
 * client upswing state, and soft-recreates the player once on Mohist join
 * (same recovery as suicide, without dying or keepInventory).
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] v1.0.5 client sync + soft refresh (no-suicide recovery)", MOD_ID);
        ClientCombatReset.registerIfClient();
        MeleeFixJoinProbe.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
