package com.dbzlegacy.mohistmelee.mixin;

import net.minecraft.world.entity.player.Player;
import net.shurui.dev.sdu.util.SduPerms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SDU {@code SduPerms.canEdit} is only {@code player.hasPermissions(2)}.
 * On Mohist, Bukkit OP / LuckPerms staff often do not map to Forge permission level 2,
 * so race/form/saga editor open and save packets return silently.
 */
@Mixin(value = SduPerms.class, remap = false)
public abstract class SduPermsCanEditMixin {

    @Inject(method = "canEdit", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dbzlegacy$mohistStaffCanEdit(
            Player player, CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            return;
        }
        if (player == null) {
            return;
        }
        if (bukkitIsOp(player)
                || hasBukkitPermission(player, "difficulty.admin")
                || hasBukkitPermission(player, "sdu.edit")) {
            cir.setReturnValue(true);
        }
    }

    private static boolean bukkitIsOp(Player player) {
        try {
            Object result = player.getClass().getMethod("isOp").invoke(player);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasBukkitPermission(Player player, String node) {
        try {
            Object result = player.getClass()
                    .getMethod("hasPermission", String.class)
                    .invoke(player, node);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
