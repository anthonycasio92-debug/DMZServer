// kubejs/startup_scripts/shadow_dummy_protect_hook.js
//
// Port of CustomNPCs ShadowDummyForgeProtect.js
// Same-tick spawn protection for player shadow dummies.
//
// MUST be startup_scripts — ForgeEvents.onEvent only registers on first load.
// Requires full restart once after adding/changing this file.
//
// Disable the CNPC Global Forge tab for ShadowDummyForgeProtect to avoid
// double-handling. Keep ShadowDummyLimiter.js on the Player tab.

console.info("[ShadowDummyProtect] registering Forge hooks...");

var SPAWN_PROTECT_MS = 3000;

var TAG_PLAYER_SHADOW = "dmz_player_shadow";
var TAG_SPAWN_PROTECT = "dmz_minigame_spawn_protect";
var TAG_SPAWN_PROTECT_UNTIL = "dmz_minigame_spawn_protect_until";
var NBT_LIMITER_LOCKED = "dmz_shadow_limiter_locked";

var ShadowDummyEntityCls = null;
var SystemCls = null;

function loadJava() {
    if (ShadowDummyEntityCls != null) return true;
    try {
        ShadowDummyEntityCls = Java.loadClass(
            "com.dragonminez.common.init.entities.ShadowDummyEntity"
        );
        SystemCls = Java.loadClass("java.lang.System");
        return ShadowDummyEntityCls != null;
    } catch (err) {
        console.error("[ShadowDummyProtect] Java load failed: " + err);
        return false;
    }
}

function nowMs() {
    try {
        return Number(SystemCls.currentTimeMillis());
    } catch (e) {
        return Date.now();
    }
}

function eventEntity(event) {
    if (event == null) return null;
    try {
        return event.getEntity();
    } catch (e1) {}
    try {
        return event.entity;
    } catch (e2) {}
    return null;
}

function isShadowDummyEntity(entity) {
    if (entity == null || !loadJava()) return false;
    // KubeJS loadClass returns java.lang.Class — use Class.isInstance
    try {
        if (ShadowDummyEntityCls.isInstance(entity)) return true;
    } catch (e0) {}
    try {
        if (entity instanceof ShadowDummyEntityCls) return true;
    } catch (e1) {}
    try {
        var cn = String(entity.getClass().getName());
        if (cn.indexOf("ShadowDummyEntity") >= 0) return true;
    } catch (e2) {}
    return false;
}

function nbtGetBoolean(tag, key) {
    try {
        return tag.getBoolean(key) === true;
    } catch (e0) {}
    try {
        return tag.m_128471_(key) === true;
    } catch (e1) {}
    return false;
}

function nbtPutBoolean(tag, key, value) {
    try {
        tag.putBoolean(key, value === true);
        return;
    } catch (e0) {}
    try {
        tag.m_128379_(key, value === true);
    } catch (e1) {}
}

function nbtGetLong(tag, key) {
    try {
        return Number(tag.getLong(key));
    } catch (e0) {}
    try {
        return Number(tag.m_128454_(key));
    } catch (e1) {}
    return NaN;
}

function nbtPutLong(tag, key, value) {
    try {
        tag.putLong(key, value);
        return;
    } catch (e0) {}
    try {
        tag.m_128356_(key, value);
    } catch (e1) {}
}

function isPlayerShadowDummy(entity) {
    if (!isShadowDummyEntity(entity)) return false;
    try {
        return nbtGetBoolean(entity.getPersistentData(), TAG_PLAYER_SHADOW);
    } catch (err) {
        return false;
    }
}

function setDummyInvulnerable(dummy, enabled) {
    if (dummy == null) return;
    try {
        dummy.setInvulnerable(enabled === true);
        return;
    } catch (e0) {}
    try {
        dummy.m_20331_(enabled === true);
    } catch (e1) {}
}

function bumpDummyHurtInvuln(dummy) {
    if (dummy == null) return;
    try {
        dummy.invulnerableTime = 40;
        return;
    } catch (e0) {}
    try {
        dummy.f_19802_ = 40;
    } catch (e1) {}
}

function setDummyNoAi(dummy, enabled) {
    if (dummy == null) return;
    try {
        dummy.setNoAi(enabled === true);
        return;
    } catch (e0) {}
    try {
        dummy.m_21557_(enabled === true);
    } catch (e1) {}
}

function clearDummyTarget(dummy) {
    if (dummy == null) return;
    try {
        dummy.setTarget(null);
        return;
    } catch (e0) {}
    try {
        dummy.m_6710_(null);
    } catch (e1) {}
}

function fullyHealDummy(dummy) {
    if (dummy == null) return;
    try {
        dummy.setHealth(dummy.getMaxHealth());
        return;
    } catch (e0) {}
    try {
        dummy.m_21153_(dummy.m_21233_());
    } catch (e1) {}
}

function grantSpawnProtection(dummy, untilMs) {
    if (dummy == null) return;

    var until = Number(untilMs);
    if (isNaN(until) || !isFinite(until)) {
        until = nowMs() + SPAWN_PROTECT_MS;
    }

    setDummyInvulnerable(dummy, true);
    bumpDummyHurtInvuln(dummy);
    setDummyNoAi(dummy, true);
    clearDummyTarget(dummy);
    fullyHealDummy(dummy);

    try {
        var persistent = dummy.getPersistentData();
        nbtPutBoolean(persistent, TAG_SPAWN_PROTECT, true);
        nbtPutLong(persistent, TAG_SPAWN_PROTECT_UNTIL, until);
        nbtPutBoolean(persistent, NBT_LIMITER_LOCKED, true);
    } catch (tagErr) {}
}

function isDummySpawnProtected(dummy, now) {
    if (dummy == null) return false;
    try {
        var persistent = dummy.getPersistentData();
        if (nbtGetBoolean(persistent, NBT_LIMITER_LOCKED)) return true;
        if (!nbtGetBoolean(persistent, TAG_SPAWN_PROTECT)) return false;
        var until = nbtGetLong(persistent, TAG_SPAWN_PROTECT_UNTIL);
        if (isNaN(until) || !isFinite(until)) return false;
        return Number(now) < until;
    } catch (err) {
        return false;
    }
}

function protectJoinedShadowDummy(dummy) {
    if (!isPlayerShadowDummy(dummy)) return false;
    grantSpawnProtection(dummy, nowMs() + SPAWN_PROTECT_MS);
    return true;
}

function cancelDamageOnProtectedDummy(dummy, forgeEvent) {
    if (!isPlayerShadowDummy(dummy)) return false;

    var now = nowMs();
    if (!isDummySpawnProtected(dummy, now)) return false;

    try {
        var until = nbtGetLong(
            dummy.getPersistentData(),
            TAG_SPAWN_PROTECT_UNTIL
        );
        grantSpawnProtection(
            dummy,
            isNaN(until) ? now + SPAWN_PROTECT_MS : until
        );
    } catch (tagErr) {
        grantSpawnProtection(dummy, now + SPAWN_PROTECT_MS);
    }

    try {
        forgeEvent.setAmount(0);
    } catch (amtErr) {}
    try {
        forgeEvent.setCanceled(true);
    } catch (cancelErr) {}

    return true;
}

ForgeEvents.onEvent(
    "net.minecraftforge.event.entity.EntityJoinLevelEvent",
    function (event) {
        try {
            protectJoinedShadowDummy(eventEntity(event));
        } catch (err) {
            console.error("[ShadowDummyProtect] join error: " + err);
        }
    }
);

ForgeEvents.onEvent(
    "net.minecraftforge.event.entity.living.LivingHurtEvent",
    function (event) {
        try {
            cancelDamageOnProtectedDummy(eventEntity(event), event);
        } catch (err) {
            console.error("[ShadowDummyProtect] hurt error: " + err);
        }
    }
);

ForgeEvents.onEvent(
    "net.minecraftforge.event.entity.living.LivingDamageEvent",
    function (event) {
        try {
            cancelDamageOnProtectedDummy(eventEntity(event), event);
        } catch (err) {
            console.error("[ShadowDummyProtect] damage error: " + err);
        }
    }
);

console.info(
    "[ShadowDummyProtect] EntityJoinLevel + LivingHurt/Damage registered (3s spawn protect)"
);
