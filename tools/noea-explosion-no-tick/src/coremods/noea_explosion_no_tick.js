function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

    // RETURN first. Clearing the body drops StackMapTable frames and entity
    // registration fails with NoClassDefFoundError.
    function voidNoOp(method) {
        var first = method.instructions.getFirst();
        method.instructions.insertBefore(first, new InsnNode(Opcodes.RETURN));
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
