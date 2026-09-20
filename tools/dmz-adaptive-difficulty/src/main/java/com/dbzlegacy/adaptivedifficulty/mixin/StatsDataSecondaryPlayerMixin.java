package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@link StatsData#getMaxEnergy()} / {@link StatsData#getMaxStamina()} read
 * {@code MAX_* } via {@code getSecondaryAttributeValue}, which uses the {@code player} field
 * directly — not {@link StatsData#getPlayer()}. On Mohist the field is often null while
 * {@link StatsDataGetPlayerMixin} can still resolve the owner; without this redirect the server
 * skips secondary attributes (defaults to 20) and caps/regen sit at ~1× while the HUD shows the
 * full Overhaul/form-scaled bar (~2×).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataSecondaryPlayerMixin {
    @Shadow(remap = false)
    private Player player;

    @Redirect(
            method = "getSecondaryAttributeValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$secondaryAttrPlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    @Redirect(
            method = "getSecondaryAttributeBaseValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$secondaryBasePlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    @Redirect(
            method = "getArmorToughnessValue",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dragonminez/common/stats/StatsData;player:Lnet/minecraft/world/entity/player/Player;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private Player lm$armorToughnessPlayer(StatsData instance) {
        return lm$playerForSecondary(instance);
    }

    private static Player lm$playerForSecondary(StatsData instance) {
        if (instance == null) {
            return null;
        }
        Player resolved = instance.getPlayer();
        if (resolved != null && resolved.m_6084_()) {
            return resolved;
        }
        return ((StatsDataSecondaryPlayerMixin) (Object) instance).player;
    }
}
