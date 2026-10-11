package com.dbzlegacy.adaptivedifficulty.mixin;

import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * KaiPath 1.3.0 {@code LmUltraConsumer.bootstrap} skips {@code registerClient()} when
 * {@code legacymechanics} is loaded, assuming LM owns the client GUI. LM is server-only and
 * registers no client channel — so a client that also has the LM jar ends up with
 * <em>no</em> {@code legacymechanics:gui} consumer. Clicks then die in KaiPath
 * {@code sendAction} ({@code channel == null} / {@code !isRemotePresent}) with no log.
 *
 * <p>Redirect the mod-id checks so KaiPath still registers its LM consumer.
 */
@Mixin(
        targets = "com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.LmUltraConsumer",
        remap = false)
public final class KaiPathLmConsumerBootstrapMixin {
    @Redirect(
            method = "bootstrap",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraftforge/fml/ModList;isLoaded(Ljava/lang/String;)Z",
                            remap = false),
            remap = false)
    private static boolean lm$kaipathShouldTreatLmAsAbsent(ModList list, String modId) {
        if ("legacymechanics".equals(modId) || "legacymechanicsultra".equals(modId)) {
            return false;
        }
        return list != null && list.isLoaded(modId);
    }
}
