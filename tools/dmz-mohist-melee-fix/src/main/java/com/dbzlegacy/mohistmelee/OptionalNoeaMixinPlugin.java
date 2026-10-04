package com.dbzlegacy.mohistmelee;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Apply Noea grab block mixins only when {@code noeabosses} is on the classpath. */
public final class OptionalNoeaMixinPlugin implements IMixinConfigPlugin {
    private static volatile boolean noeaPresent;

    @Override
    public void onLoad(String mixinPackage) {
        noeaPresent = classPresent("com.butterjaffa.noeabosses.GrabService");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return noeaPresent;
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

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, OptionalNoeaMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
