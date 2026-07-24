package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.combat.util.Minecraft_DMZ;
import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Watchdog for DMZ {@code MinecraftMixin} combat state. If the local player is
 * missing mid-upswing, ticks never finish the swing and {@code attackCooldown}
 * can stay at 10000 — cancelling all further M1 until something flushes state.
 */
@Mixin(targets = "net.minecraft.client.Minecraft", remap = false)
public abstract class ClientMinecraftCombatMixin {

    @Unique
    private boolean dbzlegacy$sawNullPlayer;

    @Unique
    private static Field dbzlegacy$playerField;

    @Inject(method = {"m_91383_", "tick"}, at = @At("HEAD"), require = 0, remap = false)
    private void dbzlegacy$flushStuckCombat(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof Minecraft_DMZ)) {
            return;
        }
        Minecraft_DMZ dmz = (Minecraft_DMZ) self;
        Object player = dbzlegacy$readPlayer();
        if (player == null) {
            dbzlegacy$sawNullPlayer = true;
            return;
        }
        boolean stuckUpswing = dmz.getUpswingTicks() > 30;
        if (dbzlegacy$sawNullPlayer || stuckUpswing) {
            dmz.cancelUpswing();
            dbzlegacy$sawNullPlayer = false;
        }
    }

    @Unique
    private Object dbzlegacy$readPlayer() {
        try {
            if (dbzlegacy$playerField == null) {
                Class<?> c = this.getClass();
                while (c != null) {
                    try {
                        Field f = c.getDeclaredField("f_91074_");
                        f.setAccessible(true);
                        dbzlegacy$playerField = f;
                        break;
                    } catch (NoSuchFieldException e) {
                        c = c.getSuperclass();
                    }
                }
                if (dbzlegacy$playerField == null) {
                    Field f = this.getClass().getDeclaredField("player");
                    f.setAccessible(true);
                    dbzlegacy$playerField = f;
                }
            }
            return dbzlegacy$playerField.get(this);
        } catch (Throwable t) {
            return null;
        }
    }
}
