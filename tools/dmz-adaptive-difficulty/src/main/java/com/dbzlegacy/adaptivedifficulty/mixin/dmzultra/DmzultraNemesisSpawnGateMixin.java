package com.dbzlegacy.adaptivedifficulty.mixin.dmzultra;

import com.dbzlegacy.adaptivedifficulty.rival.NemesisSpawnMode;
import com.dmzultra.battlepower.FoeScale;
import com.dmzultra.journal.Journal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Per-player nemesis spawn gate for dmzultra's rival journal
 * ({@code NemesisSpawnMode}). dmzultra is not a compile-shipped dependency of
 * the server runtime, so the target uses the string form with {@code require 0}
 * and the mixin plugin gates application on the class being present.
 *
 * <p>Bytecode call graph (dmzultra-0.2.9 {@code com.dmzultra.journal.Rival}):
 * every nemesis spawn flows through {@code meet(...)} — {@code onTick} calls it
 * with trigger {@code "tired"} / {@code "found"} (the auto-visits) and the
 * manual journal {@code challenge} calls it with {@code "appear"} — or through
 * {@code champion(...)}, which calls {@code spawn} directly. Returning
 * {@code false} from {@code meet} and {@code null} from {@code spawn} are both
 * safe: the mod treats them exactly like its own "no spawn" rolls and shows its
 * stock fallback text.
 */
@Mixin(targets = "com.dmzultra.journal.Rival", remap = false)
public abstract class DmzultraNemesisSpawnGateMixin {

    /**
     * OFF suppresses every trigger (auto and manual). MANUAL keeps only the
     * player-started challenge flow ({@code "appear"}); tick visits never spawn.
     */
    @Inject(method = "meet", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void lm$gateNemesisMeet(
            ServerPlayer player,
            Journal.State state,
            CompoundTag identity,
            String trigger,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (player == null || cir == null) {
                return;
            }
            if (!NemesisSpawnMode.allowManual(player)) {
                cir.setReturnValue(false);
                return;
            }
            if (!NemesisSpawnMode.allowTickSpawn(player)
                    && !"appear".equalsIgnoreCase(trigger)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Final gate: with the toggle OFF no path may materialize the mob. */
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void lm$gateNemesisSpawn(
            ServerPlayer player,
            EntityType<?> type,
            long power,
            Component name,
            FoeScale.Spec spec,
            CallbackInfoReturnable<Mob> cir) {
        try {
            if (player != null && cir != null && !NemesisSpawnMode.allowManual(player)) {
                cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }
}
