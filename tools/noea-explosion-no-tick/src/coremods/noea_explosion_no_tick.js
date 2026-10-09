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
        // That tick only sends particles and sounds.
        'DisablePlanetExplosionTick': {
            'target': {
                'type': 'METHOD',
                'class': 'com.butterjaffa.noeabosses.entity.DeepSpaceExplosionEntity',
                'methodName': 'm_8119_',
                'methodDesc': '()V'
            },
            'transformer': voidNoOp
        },
        // Per-tick block breaks, vanilla explosions, and particle bursts.
        // The destruction start and collapse timer still run, so the planet
        // is still marked destroyed and the entity is still discarded.
        'DisablePlanetDetonationLag': {
            'target': {
                'type': 'METHOD',
                'class': 'com.butterjaffa.noeabosses.DeepSpaceEvents',
                'methodName': 'handlePlanetDetonationTick',
                'methodDesc': '(Lnet/minecraft/server/level/ServerLevel;Lcom/butterjaffa/noeabosses/entity/DeepSpacePlanetEntity;I)V'
            },
            'transformer': voidNoOp
        }
    };
}
