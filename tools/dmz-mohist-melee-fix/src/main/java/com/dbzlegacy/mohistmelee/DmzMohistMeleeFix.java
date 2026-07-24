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
                "[{}] v2.6.0 empty-hand/cross-dim fix: restore primary STR + fist range floor (no ki_damage writes)",
                MOD_ID
        );
        ReachRepairEvents.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
