package com.dbzlegacy.adaptivedifficulty.network;

import com.dbzlegacy.adaptivedifficulty.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** S2C: open / refresh the Adaptive Difficulty screen. */
public final class OpenDifficultyScreenPacket {
    public final String page;
    public final long active;
    public final long calculated;
    public final long purchased;
    public final long personalMax;
    public final long availableMax;
    public final long teamThresholdBonus;
    public final long teamContribution;
    public final int dmzLevel;
    public final int prestige;
    public final String teamMode;
    public final String teamName;
    public final int teammateCount;
    public final String teamSource;
    public final String enemyTier;
    public final String currencyLabel;
    public final String balanceText;
    public final String cost100Text;
    public final String cost1kText;
    public final String cost10kText;
    public final long cost100;
    public final long cost1k;
    public final long cost10k;
    public final String stateColor;

    public OpenDifficultyScreenPacket(
            String page,
            long active,
            long calculated,
            long purchased,
            long personalMax,
            long availableMax,
            long teamThresholdBonus,
            long teamContribution,
            int dmzLevel,
            int prestige,
            String teamMode,
            String teamName,
            int teammateCount,
            String teamSource,
            String enemyTier,
            String currencyLabel,
            String balanceText,
            String cost100Text,
            String cost1kText,
            String cost10kText,
            long cost100,
            long cost1k,
            long cost10k,
            String stateColor
    ) {
        this.page = page;
        this.active = active;
        this.calculated = calculated;
        this.purchased = purchased;
        this.personalMax = personalMax;
        this.availableMax = availableMax;
        this.teamThresholdBonus = teamThresholdBonus;
        this.teamContribution = teamContribution;
        this.dmzLevel = dmzLevel;
        this.prestige = prestige;
        this.teamMode = teamMode;
        this.teamName = teamName;
        this.teammateCount = teammateCount;
        this.teamSource = teamSource;
        this.enemyTier = enemyTier;
        this.currencyLabel = currencyLabel;
        this.balanceText = balanceText;
        this.cost100Text = cost100Text;
        this.cost1kText = cost1kText;
        this.cost10kText = cost10kText;
        this.cost100 = cost100;
        this.cost1k = cost1k;
        this.cost10k = cost10k;
        this.stateColor = stateColor;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.m_130070_(page);
        buf.writeLong(active);
        buf.writeLong(calculated);
        buf.writeLong(purchased);
        buf.writeLong(personalMax);
        buf.writeLong(availableMax);
        buf.writeLong(teamThresholdBonus);
        buf.writeLong(teamContribution);
        buf.writeInt(dmzLevel);
        buf.writeInt(prestige);
        buf.m_130070_(nullSafe(teamMode));
        buf.m_130070_(nullSafe(teamName));
        buf.writeInt(teammateCount);
        buf.m_130070_(nullSafe(teamSource));
        buf.m_130070_(nullSafe(enemyTier));
        buf.m_130070_(nullSafe(currencyLabel));
        buf.m_130070_(nullSafe(balanceText));
        buf.m_130070_(nullSafe(cost100Text));
        buf.m_130070_(nullSafe(cost1kText));
        buf.m_130070_(nullSafe(cost10kText));
        buf.writeLong(cost100);
        buf.writeLong(cost1k);
        buf.writeLong(cost10k);
        buf.m_130070_(nullSafe(stateColor));
    }

    public static OpenDifficultyScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenDifficultyScreenPacket(
                buf.m_130277_(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.readInt(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.m_130277_(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.m_130277_()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openDifficulty(this)));
        context.setPacketHandled(true);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
