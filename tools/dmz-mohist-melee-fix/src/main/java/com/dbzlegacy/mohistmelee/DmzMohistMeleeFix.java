package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minimal Mohist + DMZ fix: repair collapsed Forge ENTITY_REACH only.
 * No damage redirects, soft-respawn, or combat-lock changes.
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] v2.0.0 ENTITY_REACH collapse repair only", MOD_ID);
        ReachRepairEvents.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
