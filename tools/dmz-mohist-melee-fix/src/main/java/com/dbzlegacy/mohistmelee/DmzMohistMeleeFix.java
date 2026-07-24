package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist + DMZ melee fix: cancelled ki blasts / no-PvP hits can brick M1 because
 * Mohist's Bukkit damage bridge stays broken until death. This mod probes Bukkit
 * cancels safely, applies damage via LivingHurt + setHealth, and repairs attacker
 * combat state after denied hits.
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] v1.0.4 unlock stale strikeLocked + melee/ki/strike Bukkit-bypass", MOD_ID);
        MeleeFixJoinProbe.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
