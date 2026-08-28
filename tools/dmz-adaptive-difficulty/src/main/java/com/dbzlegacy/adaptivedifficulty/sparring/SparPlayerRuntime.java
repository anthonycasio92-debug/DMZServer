package com.dbzlegacy.adaptivedifficulty.sparring;

import java.util.UUID;

/** Per-player in-memory sparring session runtime. */
public final class SparPlayerRuntime {
    public UUID partner;
    public boolean active;
    public long startAt;
    public long graceUntil;
    public String graceReason = "";
    public boolean graceWarned;
    public long restartCooldownUntil;

    public String lastOutPartner = "";
    public long lastOutAt;
    public String lastOutKind = "melee";
    public String lastInPartner = "";
    public long lastInAt;
    public long lastKiOutAt;
    public long lastLaserOutAt;
    public long clashUntil;

    public float pendingSampleHp;
    public UUID pendingAttacker;
    public boolean pendingKi;
    public String pendingKiKind = "";
    public long pendingUntil;

    public double moveX;
    public double moveY;
    public double moveZ;
    public long moveValidUntil;
    public long heavyMotionUntil;

    public int combo;
    public long comboUntil;
    public int momentumTier;
    public long momentumUntil;

    public double sessionTp;
    public double sessionMelee;
    public double sessionKi;
    public double sessionDmg;
    public double sessionTaken;
    public int sessionBlocks;
    public long sessionClashMs;
    public int sessionKb;
    public int sessionMaxCombo;
    public int sessionMaxMom;
    public boolean sessionPerfect;

    public double styleMelee;
    public double styleKi;
    public double styleBeam;
    public double styleBlock;
    public double styleMove;

    public double tpPending;
    public double tpPendingMelee;
    public double tpPendingKi;
    public double tpPendingClash;
    public long tpMsgNext;
    public long clashNext;
    public long releaseCtrlNext;
    public long messageNext;

    public void resetSession() {
        partner = null;
        active = false;
        startAt = 0L;
        graceUntil = 0L;
        graceReason = "";
        graceWarned = false;
        combo = 0;
        comboUntil = 0L;
        momentumTier = 0;
        momentumUntil = 0L;
        sessionTp = 0;
        sessionMelee = 0;
        sessionKi = 0;
        sessionDmg = 0;
        sessionTaken = 0;
        sessionBlocks = 0;
        sessionClashMs = 0;
        sessionKb = 0;
        sessionMaxCombo = 0;
        sessionMaxMom = 0;
        sessionPerfect = false;
        styleMelee = 0;
        styleKi = 0;
        styleBeam = 0;
        styleBlock = 0;
        styleMove = 0;
        moveX = 0;
        moveY = 0;
        moveZ = 0;
        moveValidUntil = 0L;
        heavyMotionUntil = 0L;
        tpPending = 0;
        tpPendingMelee = 0;
        tpPendingKi = 0;
        tpPendingClash = 0;
        clashUntil = 0L;
        pendingUntil = 0L;
        pendingAttacker = null;
    }
}
