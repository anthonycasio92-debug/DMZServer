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

    // this.discard() after the superclass constructor. Calling it before super()
    // uses an uninitialized this and the class fails verification.
    // m_146870_ is Entity.discard() in the 1.20.1 SRG map (obfuscated ai).
    function discardAfterSuper(method) {
        var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
        var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
        var insn = method.instructions.getFirst();
        var afterSuper = null;
        while (insn != null) {
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                afterSuper = insn.getNext();
                break;
            }
            insn = insn.getNext();
        }
        if (afterSuper == null) {
            afterSuper = method.instructions.getFirst();
        }
        method.instructions.insertBefore(afterSuper, new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.insertBefore(afterSuper, new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            'net/minecraft/world/entity/Entity',
            'm_146870_',
            '()V',
            false
        ));
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
        },
        // The constructor finishes super(), then discards. addFreshEntity
        // refuses an entity that is already removed, so clients never get it.
        'DiscardExplosionOnSpawn': {
            'target': {
                'type': 'METHOD',
                'class': 'com.butterjaffa.noeabosses.entity.DeepSpaceExplosionEntity',
                'methodName': '<init>',
                'methodDesc': '(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V'
            },
            'transformer': discardAfterSuper
        }
    };
}
