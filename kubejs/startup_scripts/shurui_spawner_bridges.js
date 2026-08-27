// kubejs/startup_scripts/shurui_spawner_bridges.js
// Port of forge_scripts.json (3 CustomNPCs Global Forge Scripts) → KubeJS.
//
// 1) Shurui Advanced Spawner → Iron's Spellbooks mob stat bridge
// 2) Shurui Advanced Spawner → Native DMZ Ki Blast Damage bridge
// 3) Shurui Advanced Spawner → Direct bow/arrow base-damage bridge
//
// MUST be startup_scripts — ForgeEvents.onEvent only registers on first load.
// Requires a full server restart after adding/changing this file.
//
// After install: disable CNPC forge scripts (forge_scripts.json ScriptEnabled=0)
// so these do not double-apply.
//
// Original CNPC hooks: entityJoinLevelEvent
// KubeJS hook: Forge EntityJoinLevelEvent

console.info("[Shurui Bridges] registering EntityJoinLevelEvent...");

// ============================================================================
// SHARED CONFIG
// ============================================================================

var DEBUG_IRON = false;
var DEBUG_NATIVE_KI = false;
var DEBUG_ARROW = false;

// --- Iron's spell bridge ---
var MAX_MANA = 1000000;
var SPELL_POWER_DIVISOR = 20.0;
var MINIMUM_SPELL_POWER = 1.0;
var MAXIMUM_SPELL_POWER = 0;
var SCALE_SUMMON_DAMAGE = true;
var SUMMON_DAMAGE_MULTIPLIER = 1.0;
var MINIMUM_SUMMON_DAMAGE = 1.0;
var MAXIMUM_SUMMON_DAMAGE = 0;
var PRESERVE_HIGHER_NATIVE_VALUES = true;

// --- Native DMZ Ki bridge ---
var KI_DAMAGE_MULTIPLIER = 1.0;
var KEEP_NATIVE_KI_IF_SPAWNER_ZERO = true;
var DMZ_MAX_KI_DAMAGE = 2000000000;

// --- Arrow bridge ---
var REFERENCE_PHYSICAL_DAMAGE = 4.0;
var PROJECTILE_DAMAGE_MULTIPLIER = 1.0;
var ONLY_INCREASE_DAMAGE = true;
var MAX_ARROW_BASE_DAMAGE = 0;

// ============================================================================
// JAVA
// ============================================================================

var LivingEntity = Java.loadClass("net.minecraft.world.entity.LivingEntity");
var BlockPos = Java.loadClass("net.minecraft.core.BlockPos");
var Attributes = Java.loadClass(
    "net.minecraft.world.entity.ai.attributes.Attributes"
);
var AbstractArrow = Java.loadClass(
    "net.minecraft.world.entity.projectile.AbstractArrow"
);
var ProjectileWeaponItem = Java.loadClass(
    "net.minecraft.world.item.ProjectileWeaponItem"
);
var EquipmentSlot = Java.loadClass(
    "net.minecraft.world.entity.EquipmentSlot"
);

var AdvancedSpawnerBlockEntity = null;
var DBSagasEntity = null;
var SduDmzFighter = null;
var AttributeRegistry = null;
var MagicData = null;
var javaReady = false;

function loadOptionalJava() {
    if (javaReady) return true;
    try {
        AdvancedSpawnerBlockEntity = Java.loadClass(
            "net.shurui.dev.shuruis_dmz_dungeons.block.AdvancedSpawnerBlockEntity"
        );
    } catch (e) {
        console.error("[Shurui Bridges] AdvancedSpawnerBlockEntity missing: " + e);
        return false;
    }
    try {
        DBSagasEntity = Java.loadClass(
            "com.dragonminez.common.init.entities.sagas.DBSagasEntity"
        );
    } catch (e2) {
        DBSagasEntity = null;
    }
    try {
        SduDmzFighter = Java.loadClass(
            "net.shurui.dev.sdu.entity.SduDmzFighter"
        );
    } catch (e3) {
        SduDmzFighter = null;
    }
    try {
        AttributeRegistry = Java.loadClass(
            "io.redspace.ironsspellbooks.api.registry.AttributeRegistry"
        );
        MagicData = Java.loadClass(
            "io.redspace.ironsspellbooks.api.magic.MagicData"
        );
    } catch (e4) {
        AttributeRegistry = null;
        MagicData = null;
    }
    javaReady = true;
    return true;
}

function isInstance(cls, obj) {
    if (cls == null || obj == null) return false;
    try {
        if (cls.isInstance(obj)) return true;
    } catch (e0) {}
    try {
        if (obj instanceof cls) return true;
    } catch (e1) {}
    return false;
}

function getEntity(event) {
    try {
        if (event.getEntity) return event.getEntity();
    } catch (e0) {}
    try {
        return event.entity;
    } catch (e1) {}
    return null;
}

function getLevel(event, entity) {
    try {
        if (event.getLevel) return event.getLevel();
    } catch (e0) {}
    try {
        if (entity.level) return entity.level;
    } catch (e1) {}
    try {
        if (entity.getCommandSenderWorld) return entity.getCommandSenderWorld();
    } catch (e2) {}
    try {
        // ServerEntity / Entity.level()
        if (entity.m_9236_) return entity.m_9236_();
    } catch (e3) {}
    return null;
}

function isClientLevel(level) {
    if (level == null) return true;
    try {
        if (level.isClientSide !== undefined) return !!level.isClientSide;
    } catch (e0) {}
    try {
        if (level.clientSide !== undefined) return !!level.clientSide;
    } catch (e1) {}
    try {
        if (level.m_5776_) return !!level.m_5776_();
    } catch (e2) {}
    return false;
}

function loadedFromDisk(event) {
    try {
        if (event.loadedFromDisk) return !!event.loadedFromDisk();
    } catch (e0) {}
    try {
        if (event.isLoadedFromDisk) return !!event.isLoadedFromDisk();
    } catch (e1) {}
    return false;
}

function persistentData(entity) {
    try {
        if (entity.getPersistentData) return entity.getPersistentData();
    } catch (e0) {}
    try {
        if (entity.persistentData) return entity.persistentData;
    } catch (e1) {}
    return null;
}

function dataHas(tag, key) {
    if (tag == null) return false;
    try {
        if (tag.contains) return !!tag.contains(key);
    } catch (e0) {}
    return false;
}

function dataGetBoolean(tag, key) {
    try {
        return !!tag.getBoolean(key);
    } catch (e0) {}
    return false;
}

function dataGetLong(tag, key) {
    try {
        return tag.getLong(key);
    } catch (e0) {}
    return 0;
}

function decodeBlockPos(packed) {
    try {
        // Production SRG (verified in original forge scripts / SDU)
        if (BlockPos.m_122022_) return BlockPos.m_122022_(packed);
    } catch (e0) {}
    try {
        if (BlockPos.of) return BlockPos.of(packed);
    } catch (e1) {}
    return null;
}

function getBlockEntity(level, pos) {
    if (level == null || pos == null) return null;
    try {
        if (level.getBlockEntity) return level.getBlockEntity(pos);
    } catch (e0) {}
    try {
        // Level.getBlockEntity SRG
        if (level.m_7702_) return level.m_7702_(pos);
    } catch (e1) {}
    return null;
}

function entityName(entity) {
    try {
        if (entity.getName) {
            var c = entity.getName();
            if (c && c.getString) return c.getString();
            return String(c);
        }
    } catch (e0) {}
    try {
        return String(entity.getType().toShortString());
    } catch (e1) {}
    return "?";
}

function entityTypeName(entity) {
    try {
        return String(entity.getType().toString());
    } catch (e0) {}
    try {
        return String(entity.getClass().getName());
    } catch (e1) {}
    return "?";
}

function getAttribute(entity, attribute) {
    try {
        if (entity.getAttribute) return entity.getAttribute(attribute);
    } catch (e0) {}
    try {
        if (entity.m_21051_) return entity.m_21051_(attribute);
    } catch (e1) {}
    return null;
}

function attrBase(instance) {
    try {
        if (instance.getBaseValue) return Number(instance.getBaseValue());
    } catch (e0) {}
    try {
        if (instance.m_22115_) return Number(instance.m_22115_());
    } catch (e1) {}
    return 0;
}

function attrSetBase(instance, value) {
    try {
        if (instance.setBaseValue) {
            instance.setBaseValue(Number(value));
            return;
        }
    } catch (e0) {}
    try {
        if (instance.m_22100_) instance.m_22100_(Number(value));
    } catch (e1) {}
}

function attrValue(instance) {
    try {
        if (instance.getValue) return Number(instance.getValue());
    } catch (e0) {}
    try {
        if (instance.m_22135_) return Number(instance.m_22135_());
    } catch (e1) {}
    return 0;
}

function attackDamageAttribute() {
    try {
        if (Attributes.ATTACK_DAMAGE) return Attributes.ATTACK_DAMAGE;
    } catch (e0) {}
    try {
        // SRG field used by original forge script
        if (Attributes.f_22281_) return Attributes.f_22281_;
    } catch (e1) {}
    return null;
}

function getMainHandItem(living) {
    try {
        if (living.getMainHandItem) return living.getMainHandItem();
    } catch (e0) {}
    try {
        if (living.getItemBySlot)
            return living.getItemBySlot(EquipmentSlot.MAINHAND);
    } catch (e1) {}
    try {
        if (living.m_21205_) return living.m_21205_();
    } catch (e2) {}
    return null;
}

function getOffhandItem(living) {
    try {
        if (living.getOffhandItem) return living.getOffhandItem();
    } catch (e0) {}
    try {
        if (living.getItemBySlot)
            return living.getItemBySlot(EquipmentSlot.OFFHAND);
    } catch (e1) {}
    try {
        if (living.m_21206_) return living.m_21206_();
    } catch (e2) {}
    return null;
}

function stackItem(stack) {
    if (stack == null) return null;
    try {
        if (stack.getItem) return stack.getItem();
    } catch (e0) {}
    try {
        if (stack.m_41720_) return stack.m_41720_();
    } catch (e1) {}
    return null;
}

function stackEmpty(stack) {
    if (stack == null) return true;
    try {
        if (stack.isEmpty) return !!stack.isEmpty();
    } catch (e0) {}
    try {
        if (stack.m_41619_) return !!stack.m_41619_();
    } catch (e1) {}
    return false;
}

function stackName(stack) {
    try {
        var id = stack.getItem().toString();
        return String(id);
    } catch (e0) {}
    try {
        return String(stack);
    } catch (e1) {}
    return "unknown";
}

function arrowOwner(arrow) {
    try {
        if (arrow.getOwner) return arrow.getOwner();
    } catch (e0) {}
    try {
        if (arrow.m_19749_) return arrow.m_19749_();
    } catch (e1) {}
    return null;
}

function arrowGetBase(arrow) {
    try {
        if (arrow.getBaseDamage) return Number(arrow.getBaseDamage());
    } catch (e0) {}
    try {
        if (arrow.m_36789_) return Number(arrow.m_36789_());
    } catch (e1) {}
    return NaN;
}

function arrowSetBase(arrow, value) {
    try {
        if (arrow.setBaseDamage) {
            arrow.setBaseDamage(Number(value));
            return;
        }
    } catch (e0) {}
    try {
        if (arrow.m_36781_) arrow.m_36781_(Number(value));
    } catch (e1) {}
}

function getSpawnerConfig(data, level) {
    if (!dataHas(data, "sdd_spawner")) return null;
    var packed = dataGetLong(data, "sdd_spawner");
    var pos = decodeBlockPos(packed);
    if (pos == null) return null;
    var be = getBlockEntity(level, pos);
    if (!isInstance(AdvancedSpawnerBlockEntity, be)) return null;
    var config = null;
    try {
        config = be.getConfig();
    } catch (e) {
        return null;
    }
    if (config == null) return null;
    return { config: config, pos: pos };
}

function isBossEntity(data) {
    return dataHas(data, "sdd_boss") && dataGetBoolean(data, "sdd_boss");
}

function rollSpawnerKi(config, isBoss) {
    var kiPower = 0;
    var kiMin = 0;
    var kiMax = 0;
    var savedNpcRef = "";

    if (isBoss) {
        kiPower = Number(config.bossKiPower);
        kiMin = Number(config.bossKiDmgMin);
        kiMax = Number(config.bossKiDmgMax);
        if (config.bossSavedNpcRef != null)
            savedNpcRef = "" + config.bossSavedNpcRef;
    } else {
        kiPower = Number(config.kiPower);
        kiMin = Number(config.kiDmgMin);
        kiMax = Number(config.kiDmgMax);
        if (config.savedNpcRef != null)
            savedNpcRef = "" + config.savedNpcRef;
    }

    if (isNaN(kiPower)) kiPower = 0;
    if (isNaN(kiMin)) kiMin = 0;
    if (isNaN(kiMax)) kiMax = 0;

    kiPower = Math.max(0, kiPower);
    kiMin = Math.max(0, Math.floor(kiMin));
    kiMax = Math.max(0, Math.floor(kiMax));

    var low = Math.max(0, Math.min(kiMin, kiMax));
    var high = Math.max(0, Math.max(kiMin, kiMax));
    var damage = 0;
    var source = "";

    if (high <= 0) {
        damage = kiPower;
        source = "KiPower";
    } else if (low >= high) {
        damage = low;
        source = "Fixed Ki Range";
    } else {
        damage = low + Math.floor(Math.random() * (high - low + 1));
        source = "Ki Range";
    }

    return {
        damage: damage,
        source: source,
        savedNpcRef: savedNpcRef,
        kiPower: kiPower,
        kiMin: kiMin,
        kiMax: kiMax
    };
}

// ============================================================================
// 1) IRON'S SPELLBOOKS BRIDGE
// ============================================================================

function runIronsBridge(entity, data, level) {
    if (AttributeRegistry == null) return;
    if (!isInstance(LivingEntity, entity)) return;
    if (dataHas(data, "sdd_irons_bridge_complete") &&
        dataGetBoolean(data, "sdd_irons_bridge_complete")) {
        return;
    }

    var spawner = getSpawnerConfig(data, level);
    if (spawner == null) return;

    var boss = isBossEntity(data);
    var roll = rollSpawnerKi(spawner.config, boss);
    var kiDamage = 0;
    var kiSource = "Spawner";
    var hasSavedNpc = roll.savedNpcRef.trim().length > 0;

    if (!hasSavedNpc && isInstance(DBSagasEntity, entity)) {
        try {
            var actual = Number(entity.getKiBlastDamage());
            if (!isNaN(actual) && actual > 0) {
                kiDamage = actual;
                kiSource = "Actual DMZ Fighter";
            }
        } catch (eKi) {}
    }

    if (kiDamage <= 0) {
        kiDamage = roll.damage;
        if (roll.source === "KiPower") kiSource = "Spawner KiPower";
        else if (roll.source === "Fixed Ki Range")
            kiSource = "Spawner Fixed Ki Range";
        else kiSource = "Spawner Ki Range";
    }

    if (isNaN(kiDamage) || kiDamage < 0) kiDamage = 0;
    data.putDouble("sdd_irons_ki_damage", kiDamage);

    var maxManaAttr = AttributeRegistry.MAX_MANA.get();
    var maxManaInst = getAttribute(entity, maxManaAttr);
    var oldMaxMana = 0;
    var appliedMaxMana = 0;
    if (maxManaInst != null) {
        oldMaxMana = attrBase(maxManaInst);
        attrSetBase(maxManaInst, MAX_MANA);
        appliedMaxMana = attrValue(maxManaInst);
    }

    try {
        if (MagicData != null) {
            var magic = MagicData.getPlayerMagicData(entity);
            if (magic != null) magic.setMana(Number(MAX_MANA));
        }
    } catch (manaErr) {
        if (DEBUG_IRON) {
            console.info(
                "[Shurui Iron Bridge] Mana fill warning for " +
                    entityName(entity) +
                    ": " +
                    manaErr
            );
        }
    }

    var calculatedSpellPower = kiDamage / SPELL_POWER_DIVISOR;
    if (calculatedSpellPower < MINIMUM_SPELL_POWER)
        calculatedSpellPower = MINIMUM_SPELL_POWER;
    if (MAXIMUM_SPELL_POWER > 0 && calculatedSpellPower > MAXIMUM_SPELL_POWER)
        calculatedSpellPower = MAXIMUM_SPELL_POWER;

    var spellAttr = AttributeRegistry.SPELL_POWER.get();
    var spellInst = getAttribute(entity, spellAttr);
    var oldSpellPower = 0;
    var requestedSpellPower = calculatedSpellPower;
    var appliedSpellPower = 0;
    if (spellInst != null) {
        oldSpellPower = attrBase(spellInst);
        if (PRESERVE_HIGHER_NATIVE_VALUES && oldSpellPower > requestedSpellPower)
            requestedSpellPower = oldSpellPower;
        attrSetBase(spellInst, requestedSpellPower);
        appliedSpellPower = attrValue(spellInst);
    }

    var calculatedSummon =
        calculatedSpellPower * SUMMON_DAMAGE_MULTIPLIER;
    if (calculatedSummon < MINIMUM_SUMMON_DAMAGE)
        calculatedSummon = MINIMUM_SUMMON_DAMAGE;
    if (MAXIMUM_SUMMON_DAMAGE > 0 && calculatedSummon > MAXIMUM_SUMMON_DAMAGE)
        calculatedSummon = MAXIMUM_SUMMON_DAMAGE;

    var oldSummonDamage = 0;
    var requestedSummon = calculatedSummon;
    var appliedSummon = 0;
    if (SCALE_SUMMON_DAMAGE) {
        var summonAttr = AttributeRegistry.SUMMON_DAMAGE.get();
        var summonInst = getAttribute(entity, summonAttr);
        if (summonInst != null) {
            oldSummonDamage = attrBase(summonInst);
            if (
                PRESERVE_HIGHER_NATIVE_VALUES &&
                oldSummonDamage > requestedSummon
            ) {
                requestedSummon = oldSummonDamage;
            }
            attrSetBase(summonInst, requestedSummon);
            appliedSummon = attrValue(summonInst);
        }
    }

    data.putDouble("sdd_irons_spell_power", appliedSpellPower);
    data.putDouble("sdd_irons_summon_damage", appliedSummon);
    data.putDouble("sdd_irons_max_mana", appliedMaxMana);
    data.putBoolean("sdd_irons_bridge_complete", true);

    if (DEBUG_IRON) {
        console.info(
            "[Shurui Iron Bridge] INITIALIZED ONCE" +
                " | Mob=" +
                entityName(entity) +
                " | Type=" +
                entityTypeName(entity) +
                " | Boss=" +
                boss +
                " | Spawner=" +
                spawner.pos.getX() +
                "," +
                spawner.pos.getY() +
                "," +
                spawner.pos.getZ() +
                " | KiSource=" +
                kiSource +
                " | KiDamage=" +
                kiDamage +
                " | OldSpellPower=" +
                oldSpellPower +
                " | SpellPower=" +
                appliedSpellPower +
                " | OldSummonDamage=" +
                oldSummonDamage +
                " | SummonDamage=" +
                appliedSummon +
                " | OldMaxMana=" +
                oldMaxMana +
                " | MaxMana=" +
                appliedMaxMana
        );
    }
}

// ============================================================================
// 2) NATIVE DMZ KI BRIDGE
// ============================================================================

function runNativeDmzKiBridge(event, entity, data, level) {
    if (DBSagasEntity == null) return;
    if (loadedFromDisk(event)) return;
    if (!isInstance(DBSagasEntity, entity)) return;
    if (isInstance(SduDmzFighter, entity)) return;
    if (
        dataHas(data, "sdd_native_dmz_ki_bridge_complete") &&
        dataGetBoolean(data, "sdd_native_dmz_ki_bridge_complete")
    ) {
        return;
    }

    var spawner = getSpawnerConfig(data, level);
    if (spawner == null) return;

    var boss = isBossEntity(data);
    var roll = rollSpawnerKi(spawner.config, boss);
    var oldKi = 0;
    try {
        oldKi = Number(entity.getKiBlastDamage());
        if (isNaN(oldKi)) oldKi = 0;
    } catch (eOld) {}

    var requested = roll.damage * KI_DAMAGE_MULTIPLIER;
    if (isNaN(requested) || requested < 0) requested = 0;
    if (requested > DMZ_MAX_KI_DAMAGE) requested = DMZ_MAX_KI_DAMAGE;

    if (requested > 0) {
        entity.setKiBlastDamage(Number(requested));
    } else if (!KEEP_NATIVE_KI_IF_SPAWNER_ZERO) {
        entity.setKiBlastDamage(0);
    }

    var finalKi = 0;
    try {
        finalKi = Number(entity.getKiBlastDamage());
        if (isNaN(finalKi)) finalKi = 0;
    } catch (eFinal) {}

    data.putFloat("sdd_native_dmz_spawner_ki", Number(roll.damage));
    data.putFloat("sdd_native_dmz_applied_ki", Number(finalKi));
    data.putBoolean("sdd_native_dmz_ki_bridge_complete", true);

    if (DEBUG_NATIVE_KI) {
        console.info(
            "[Native DMZ Ki Bridge] INITIALIZED ON SPAWN" +
                " | Mob=" +
                entityName(entity) +
                " | Class=" +
                entity.getClass().getName() +
                " | Boss=" +
                boss +
                " | Spawner=" +
                spawner.pos.getX() +
                "," +
                spawner.pos.getY() +
                "," +
                spawner.pos.getZ() +
                " | Source=" +
                roll.source +
                " | SpawnerKi=" +
                roll.damage +
                " | OldKi=" +
                oldKi +
                " | AppliedKi=" +
                finalKi
        );
    }
}

// ============================================================================
// 3) DIRECT ARROW DAMAGE BRIDGE
// ============================================================================

function runArrowBridge(entity) {
    if (!isInstance(AbstractArrow, entity)) return;

    var shooter = arrowOwner(entity);
    if (!isInstance(LivingEntity, shooter)) return;

    var shooterData = persistentData(shooter);
    if (shooterData == null || !dataHas(shooterData, "sdd_spawner")) return;

    var validWeapon = false;
    var weaponName = "unknown";

    var main = getMainHandItem(shooter);
    if (!stackEmpty(main)) {
        var mainItem = stackItem(main);
        if (isInstance(ProjectileWeaponItem, mainItem)) {
            validWeapon = true;
            weaponName = stackName(main);
        }
    }

    if (!validWeapon) {
        var off = getOffhandItem(shooter);
        if (!stackEmpty(off)) {
            var offItem = stackItem(off);
            if (isInstance(ProjectileWeaponItem, offItem)) {
                validWeapon = true;
                weaponName = stackName(off);
            }
        }
    }

    if (!validWeapon) return;

    var atkAttr = attackDamageAttribute();
    if (atkAttr == null) return;
    var atkInst = getAttribute(shooter, atkAttr);
    if (atkInst == null) return;

    var physicalPower = attrValue(atkInst);
    if (isNaN(physicalPower) || physicalPower <= 0) return;

    var oldBase = arrowGetBase(entity);
    if (isNaN(oldBase) || oldBase <= 0) return;

    var scale =
        (physicalPower / REFERENCE_PHYSICAL_DAMAGE) *
        PROJECTILE_DAMAGE_MULTIPLIER;
    if (isNaN(scale) || scale < 0) scale = 0;

    var newBase = oldBase * scale;
    if (ONLY_INCREASE_DAMAGE && newBase < oldBase) newBase = oldBase;
    if (MAX_ARROW_BASE_DAMAGE > 0 && newBase > MAX_ARROW_BASE_DAMAGE)
        newBase = MAX_ARROW_BASE_DAMAGE;

    arrowSetBase(entity, newBase);
    var applied = arrowGetBase(entity);

    if (DEBUG_ARROW) {
        console.info(
            "[Shurui Direct Ranged Bridge]" +
                " Shooter=" +
                entityName(shooter) +
                " | Weapon=" +
                weaponName +
                " | PhysicalPower=" +
                physicalPower +
                " | OriginalArrowBase=" +
                oldBase +
                " | Scale=" +
                scale +
                " | NewArrowBase=" +
                applied
        );
    }
}

// ============================================================================
// REGISTER
// ============================================================================

ForgeEvents.onEvent(
    "net.minecraftforge.event.entity.EntityJoinLevelEvent",
    function (event) {
        try {
            if (!loadOptionalJava()) return;

            var entity = getEntity(event);
            if (entity == null) return;

            var level = getLevel(event, entity);
            if (isClientLevel(level)) return;

            // Arrow bridge first (projectiles are not LivingEntity / no sdd_spawner on self)
            try {
                runArrowBridge(entity);
            } catch (arrowErr) {
                console.error(
                    "[Shurui Direct Ranged Bridge ERROR] " + arrowErr
                );
            }

            var data = persistentData(entity);
            if (data == null || !dataHas(data, "sdd_spawner")) return;

            try {
                runIronsBridge(entity, data, level);
            } catch (ironErr) {
                console.error("[Shurui Iron Bridge ERROR] " + ironErr);
            }

            try {
                runNativeDmzKiBridge(event, entity, data, level);
            } catch (kiErr) {
                console.error("[Native DMZ Ki Bridge ERROR] " + kiErr);
            }
        } catch (err) {
            console.error("[Shurui Bridges ERROR] " + err);
        }
    }
);

console.info("[Shurui Bridges] EntityJoinLevelEvent registered.");
