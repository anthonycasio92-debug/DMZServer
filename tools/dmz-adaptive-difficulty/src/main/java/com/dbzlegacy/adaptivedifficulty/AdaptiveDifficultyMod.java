package com.dbzlegacy.adaptivedifficulty;

import com.dbzlegacy.adaptivedifficulty.command.DifficultyCommands;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.event.DifficultyEvents;
import com.dbzlegacy.adaptivedifficulty.scaling.AttributeLimits;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AdaptiveDifficultyMod.MOD_ID)
public final class AdaptiveDifficultyMod {
    public static final String MOD_ID = "dmz_adaptive_difficulty";
    public static final String VERSION = "3.3.13";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AdaptiveDifficultyMod() {
        // Server-side only: clients may join without this mod installed.
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> true
                )
        );

        DifficultyConfig.load();
        // Vanilla max_health 1024 / armor 30 / attack-damage 2048 would silently hard-cap scaling.
        AttributeLimits.uncapOffenseAttributes();
        MinecraftForge.EVENT_BUS.register(new DifficultyEvents());
        DifficultyCommands.register();
        LOGGER.info(
                "[{}] v{} server-only: Lightman's={}, FTB Teams={}, CMI={}, ChestGUI={}",
                MOD_ID,
                VERSION,
                AncientCoinEconomy.realCoinsAvailable(),
                TeamScaling.ftbAvailable(),
                com.dbzlegacy.adaptivedifficulty.gui.CmiGuiBridge.available(),
                com.dbzlegacy.adaptivedifficulty.gui.BukkitGuiBridge.available()
        );
    }
}
