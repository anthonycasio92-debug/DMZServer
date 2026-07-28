package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist + DMZ M1 fix without damage redirects:
 * <ul>
 *   <li>Repair collapsed Forge ENTITY_REACH</li>
 *   <li>Clear stale strikeLocked that eats CombatAttackRequest packets</li>
 *   <li>Leave vanilla ServerPlayer.attack intact (NPCs die normally)</li>
 * </ul>
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info(
                "[{}] v2.12.15: block priceless stack-form spam buys via lambda$handle$0 + computeTpCost",
                MOD_ID
        );
        ReachRepairEvents.register();
        StatsResetCommands.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
