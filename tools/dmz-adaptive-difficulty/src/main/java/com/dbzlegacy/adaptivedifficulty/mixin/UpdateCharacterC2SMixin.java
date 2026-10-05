package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.character.DmzClassChangeCapture;
import com.dbzlegacy.adaptivedifficulty.character.DmzCharacterClassChangeHooks;
import com.dbzlegacy.adaptivedifficulty.character.RaceChangeClassPickFlow;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.character.RaceChangeClassPickPacketGuard;
import com.dbzlegacy.adaptivedifficulty.character.RaceHeadBoneSync;
import com.dbzlegacy.adaptivedifficulty.character.ReskinSessionGuard;
import com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion;
import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpdateCharacterC2S.class, remap = false)
public abstract class UpdateCharacterC2SMixin {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void lm$reskinLockClassOnPacket(
            UpdateCharacterC2S packet,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci
    ) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null) {
                return;
            }
            ReskinSessionGuard.applyPacketClassLock(packet, player);
            RaceChangeClassPickPacketGuard.applyUpdateCharacterPacket(packet, player);
            RaceHeadBoneSync.applyPacketHeadBone(packet, player);
            lm$captureClassChangeSnapshot(packet, player);
        } catch (Throwable ignored) {
        }
    }

    private static void lm$captureClassChangeSnapshot(UpdateCharacterC2S packet, ServerPlayer player) {
        if (packet == null || player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        String packetClass = "";
        try {
            Field classField = UpdateCharacterC2S.class.getDeclaredField("className");
            classField.setAccessible(true);
            Object raw = classField.get(packet);
            if (raw instanceof String s) {
                packetClass = s;
            }
        } catch (Throwable ignored) {
        }
        if (packetClass.isBlank()) {
            return;
        }
        if (RaceChangeClassPickFlow.isActive(player)) {
            DmzClassChangeCapture.store(
                    player,
                    data.snapshotMultiplierResources(),
                    RaceChangeClassPickFlow.priorFightingClass(player));
            return;
        }
        String current = "";
        try {
            var ch = data.getCharacter();
            if (ch != null) {
                current = ch.getCharacterClass();
            }
        } catch (Throwable ignored) {
        }
        if (current == null || !packetClass.equalsIgnoreCase(current)) {
            DmzClassChangeCapture.store(player, data.snapshotMultiplierResources(), current);
        }
    }

    /**
     * After DMZ applies {@code UpdateCharacterC2S} (enqueueWork), not at {@code handle} return.
     * Dragon Mine Z 2.1.3 has no named apply method — the body is only {@code lambda$handle$0}.
     * {@code require = 0} so a lambda renumber skips this hook instead of refusing to boot.
     */
    @Inject(method = "lambda$handle$0", at = @At("RETURN"), remap = false, require = 0)
    private static void lm$afterUpdateCharacterApplied(
            UpdateCharacterC2S packet,
            ServerPlayer player,
            StatsData data,
            CallbackInfo ci
    ) {
        try {
            if (player == null) {
                return;
            }
            ReskinSessionGuard.enforceOnCharacter(player);
            if (RaceHeadBoneSync.syncCharacter(player)) {
                RaceHeadBoneSync.syncClient(player);
            }
            if (data != null) {
                String packetClass = "";
                try {
                    Field classField = UpdateCharacterC2S.class.getDeclaredField("className");
                    classField.setAccessible(true);
                    Object raw = classField.get(packet);
                    if (raw instanceof String s) {
                        packetClass = s;
                    }
                } catch (Throwable ignored) {
                }
                DmzCharacterClassChangeHooks.onDmzPacketFinished(player, data, packetClass);
                AndroidConversion.stripIfRaceIneligible(player, DmzProgression.race(player));
            } else {
                RaceChangeClassPickFlow.clear(player);
            }
        } catch (Throwable ignored) {
        }
    }
}
