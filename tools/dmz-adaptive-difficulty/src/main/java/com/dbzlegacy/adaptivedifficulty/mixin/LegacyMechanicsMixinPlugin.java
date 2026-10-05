package com.dbzlegacy.adaptivedifficulty.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * On a dedicated server every mixin applies. On a client only the stamina and ki
 * hook applies, so the rest of this server mod stays out of the client.
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
        if (dedicatedServer()) {
            return true;
        }
        return mixinClassName != null && mixinClassName.endsWith("StatsDataStatScalingMixin");
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
