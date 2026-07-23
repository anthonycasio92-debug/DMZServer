package com.dbzlegacy.mohistmelee;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mohist breaks DragonMineZ melee because DMZ calls {@code Player.attack}, which
 * Mohist routes through a flaky Bukkit bridge. This mod's mixin redirects that
 * call to {@code LivingEntity.hurt} so DMZ's LivingHurt handler can apply
 * getMeleeDamage() reliably — without login kills, teleports, or keepInventory.
 */
@Mod(DmzMohistMeleeFix.MOD_ID)
public final class DmzMohistMeleeFix {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public DmzMohistMeleeFix() {
        LOGGER.info("[{}] DMZ CombatAttackRequest LivingEntity hits use hurt() instead of Player.attack()", MOD_ID);
    }
}
