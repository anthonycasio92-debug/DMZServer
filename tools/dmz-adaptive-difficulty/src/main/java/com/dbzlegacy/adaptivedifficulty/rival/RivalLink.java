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

    public RivalStatus status() {
        if (mutual) {
            if (deathLosses >= RivalConstants.NEMESIS_DEATH_LOSSES) {
                isNemesis = true;
                return RivalStatus.NEMESIS;
            }
            isNemesis = false;
            return RivalStatus.MUTUAL;
        }
        if (inviteSent || inviteReceived) {
            return RivalStatus.PENDING;
        }
        if (declaredByMe && declaredByThem) {
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
