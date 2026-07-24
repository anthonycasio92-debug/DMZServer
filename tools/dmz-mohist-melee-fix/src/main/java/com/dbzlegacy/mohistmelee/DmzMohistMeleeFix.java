package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist breaks DragonMineZ melee because DMZ calls {@code Player.attack}, which
 * Mohist routes through a flaky Bukkit EntityDamage bridge. This mixin fires Forge
 * LivingHurt once then applies HP via {@code setHealth}, never calling
 * attack/hurt/actuallyHurt — no login kills/teleports/keepInventory.
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] v1.0.2 CombatAttackRequest → LivingHurt + setHealth (full Bukkit bypass)", MOD_ID);
        MeleeFixJoinProbe.register();
        MeleeFixSelfTest.registerIfEnabled();
    }
}
