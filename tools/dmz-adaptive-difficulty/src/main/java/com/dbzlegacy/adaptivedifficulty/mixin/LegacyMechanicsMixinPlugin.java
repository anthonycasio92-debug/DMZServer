package com.dbzlegacy.adaptivedifficulty.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * LegacyMechanics runs on the dedicated server only. A client copy applies no mixins.
 */
public final class LegacyMechanicsMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!dedicatedServer()) {
            return false;
        }
        if (mixinClassName != null && mixinClassName.endsWith("DojoWarSenseMixin")) {
            return senseNetworkPresent();
        }
        return dedicatedServer();
    }

    /** Noea's tracking channel. Absent on a server that does not run Noea. */
    private static boolean senseNetworkPresent() {
        try {
            Class.forName(
                    "com.butterjaffa.noeabosses.sense.SenseNetwork",
                    false,
                    LegacyMechanicsMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean dedicatedServer() {
        try {
            Class<?> loader = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            Object dist = loader.getMethod("getDist").invoke(null);
            return dist != null && "DEDICATED_SERVER".equals(String.valueOf(dist));
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
