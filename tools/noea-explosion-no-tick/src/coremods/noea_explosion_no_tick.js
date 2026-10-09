function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

    function voidNoOp(method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 0;
        return method;
    }

    return {
        // Live Noea 1.2.0 stores Entity.tick() under the SRG name below.
        // The other void no-arg method on this class only registers synched data.
        'DisablePlanetExplosionTick': {
            'target': {
                'type': 'METHOD',
                'class': 'com.butterjaffa.noeabosses.entity.DeepSpaceExplosionEntity',
                'methodName': 'm_8119_',
                'methodDesc': '()V'
            },
            'transformer': voidNoOp
        },
        // Block breaks and the particle bursts live in CelestialDestructionService, not the explosion entity.
        'DisableCelestialDestructionBegin': {
            'target': {
                'type': 'METHOD',
                'class': 'com.butterjaffa.noeabosses.CelestialDestructionService',
                'methodName': 'begin',
                'methodDesc': '(Lnet/minecraft/server/level/ServerLevel;Lcom/butterjaffa/noeabosses/entity/DeepSpacePlanetEntity;I)V'
            },
            'transformer': voidNoOp
        }
    };
}
