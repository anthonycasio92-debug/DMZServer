package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist breaks DragonMineZ melee because DMZ calls {@code Player.attack}, which
 * Mohist routes through a flaky Bukkit EntityDamage bridge (and {@code hurt()}
 * often hits the same bridge). This mixin fires Forge LivingHurt then applies
 * damage via {@code actuallyHurt}, bypassing Bukkit — no login kills/teleports/keepInventory.
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] v1.0.1 CombatAttackRequest → actuallyHurt Bukkit-bypass (single LivingHurt)", MOD_ID);
        MeleeFixSelfTest.registerIfEnabled();
    }
}
