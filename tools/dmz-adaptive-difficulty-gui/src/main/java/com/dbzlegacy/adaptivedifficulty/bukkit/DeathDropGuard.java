package com.dbzlegacy.adaptivedifficulty.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Mohist/Paper fire {@link PlayerDropItemEvent} for death loot. GriefPrevention
 * 16.18.x with {@code AllowCombatItemDrop: false} cancels those while
 * {@code inPvpCombat()} without an {@code isDead()} guard, which destroys the
 * inventory before Corpse can store it — empty corpse, all gear gone.
 * <p>
 * Un-cancel death drops only; live Q-drop during combat stays blocked by GP.
 *
 * @see <a href="https://github.com/PaperMC/Paper/issues/10044">Paper #10044</a>
 * @see <a href="https://github.com/GriefPrevention/GriefPrevention/pull/2181">GP #2181</a>
 */
public final class DeathDropGuard implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event == null || !event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null && player.isDead()) {
            event.setCancelled(false);
        }
    }
}
