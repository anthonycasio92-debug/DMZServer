package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dragonminez.client.gui.character.minigames.UltimateChallenge;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Old Kai UltimateChallenge is client-side and hardcoded as:
 * Control → Gravity → Memory → Precision → Rhythm (each to level 5).
 * Drop Precision from the stage list.
 * <p>
 * Must be present on the <b>client</b> (and server jar can include it; client mixins
 * are not applied on dedicated servers).
 */
@Mixin(value = UltimateChallenge.class, remap = false)
public abstract class UltimateChallengeNoPrecisionMixin {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static boolean LOGGED;

    /**
     * Jar order: Control, Gravity, Memory, Precision, Rhythm.
     * Keep all except Precision (4th arg).
     */
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private static List<?> dbzlegacy$skipPrecisionStage(
            Object control,
            Object gravity,
            Object memory,
            Object precision,
            Object rhythm
    ) {
        if (!LOGGED) {
            LOGGED = true;
            LOGGER.info(
                    "[{}] Old Kai UltimateChallenge: skipping Precision stage",
                    DmzMohistMeleeFix.MOD_ID
            );
        }
        return List.of(control, gravity, memory, rhythm);
    }
}
