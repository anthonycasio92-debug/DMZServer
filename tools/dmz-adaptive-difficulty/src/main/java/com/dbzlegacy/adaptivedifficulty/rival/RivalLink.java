package com.dbzlegacy.adaptivedifficulty.rival;

/**
 * One directed rivalry edge from owner → other player.
 * Status is derived from flags (mutual / invites / death losses).
 */
public final class RivalLink {
    public String uuid = "";
    public String name = "";
    public boolean mutual;
    public boolean declaredByMe;
    public boolean declaredByThem;
    public boolean inviteSent;
    public boolean inviteReceived;
    /**
     * Set when this owner used a visible Declare (not Silent). Survives decline/expire
     * so the declarer keeps a Declared entry on their rival list.
     */
    public boolean visibleDeclare;
    public double points;
    public int wins;
    public int losses;
    public int draws;
    public int deathLosses;
    public int deathWins;
    public boolean isNemesis;
    public long presenceMs;
    public long createdAt;
    public long firstMetAt;
    public long mutualSince;
    public long lastBattleAt;
    public long lastInteractAt;
    public long lastSeenTogetherAt;
    public long lastSurpassAt;
    public long pendingExpireAt;
    public boolean surpassWasBelow;
    /** Proving Grounds claim shared across the mutual pair. */
    public ProvingGrounds.Grounds provingGrounds;

    public RivalStatus status() {
        if (mutual) {
            if (deathLosses >= RivalConstants.NEMESIS_DEATH_LOSSES) {
                isNemesis = true;
                return RivalStatus.NEMESIS;
            }
            isNemesis = false;
            return RivalStatus.MUTUAL;
        }
        // Incoming visible declare — Pending Invites (accept/decline).
        if (inviteReceived) {
            return RivalStatus.PENDING;
        }
        // Dual Silent, or you visibly Declared them (even one-way / after decline).
        if ((declaredByMe && declaredByThem) || (declaredByMe && (visibleDeclare || inviteSent))) {
            return RivalStatus.DECLARED;
        }
        if (declaredByMe || declaredByThem) {
            return RivalStatus.UNKNOWN;
        }
        return RivalStatus.NONE;
    }

    public void touch(long now) {
        lastInteractAt = now;
        if (createdAt <= 0L) {
            createdAt = now;
        }
        if (firstMetAt <= 0L) {
            firstMetAt = now;
        }
    }
}
