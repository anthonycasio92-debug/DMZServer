package com.dbzlegacy.adaptivedifficulty.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Skips dmzrevamp mixins when Overhaul is not installed (dev/test servers).
 */
public final class LegacyMechanicsMixinPlugin implements IMixinConfigPlugin {
    private static final String REVAMP_PRESTIGE = "com.dmzrevamp.revamp.prestige.PrestigeSystem";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if ("com.dbzlegacy.adaptivedifficulty.mixin.DmzRevampPrestigeCapMixin".equals(mixinClassName)) {
            try {
                Class.forName(REVAMP_PRESTIGE, false, LegacyMechanicsMixinPlugin.class.getClassLoader());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        return true;
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
