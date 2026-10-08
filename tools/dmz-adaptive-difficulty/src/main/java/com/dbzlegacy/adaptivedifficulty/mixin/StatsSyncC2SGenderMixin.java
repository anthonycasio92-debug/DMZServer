package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.character.ReskinSessionGuard;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ locks gender after character creation ({@code StatsSyncC2S.handle}). During an LM reskin
 * session the editor already sends the player's gender pick — honor it server-side.
 * Races with {@code hasGender: false} stay male via {@link Character#canHaveGender()}.
 */
@Mixin(value = StatsSyncC2S.class, remap = false)
public abstract class StatsSyncC2SGenderMixin {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void lm$queueReskinGender(
            StatsSyncC2S msg,
            Supplier<NetworkEvent.Context> ctxSupplier,
            CallbackInfo ci) {
        try {
            NetworkEvent.Context ctx = ctxSupplier == null ? null : ctxSupplier.get();
            ServerPlayer player = ctx == null ? null : ctx.getSender();
            if (player == null || msg == null) {
                return;
            }
            if (ReskinSessionGuard.lockedClass(player) == null) {
                return;
            }
            String want = readGender(msg);
            String stored;
            if (Character.GENDER_MALE.equalsIgnoreCase(want)) {
                stored = Character.GENDER_MALE;
            } else if (Character.GENDER_FEMALE.equalsIgnoreCase(want)) {
                stored = Character.GENDER_FEMALE;
            } else {
                return;
            }
            // Queued before DMZ's own enqueueWork (HEAD runs first, FIFO). DMZ never
            // writes gender after creation, so this set is not overwritten.
            ctx.enqueueWork(() -> {
                try {
                    StatsData data = DmzProgression.stats(player);
                    Character ch = data == null ? null : data.getCharacter();
                    if (ch == null || !ch.canHaveGender()) {
                        return;
                    }
                    if (stored.equalsIgnoreCase(ch.getGender())) {
                        return;
                    }
                    ch.setGender(stored);
                    NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static String readGender(StatsSyncC2S msg) {
        try {
            Field f = StatsSyncC2S.class.getDeclaredField("gender");
            f.setAccessible(true);
            Object v = f.get(msg);
            return v instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}
