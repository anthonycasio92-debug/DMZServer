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

/**
 * AdaptiveDifficulty — Forge entrypoint.
 *
 * <p><b>ABI:</b> Java package {@code com.dbzlegacy.adaptivedifficulty} and class
 * {@code AdaptiveDifficultyMod} must stay stable — AdaptiveDifficultyGUI reflects on them.
 * Player NBT root {@code dmz_adaptive_difficulty} and mob tags {@code dmz_ad_*} are unchanged.
 */
@Mod(AdaptiveDifficultyMod.MOD_ID)
public final class AdaptiveDifficultyMod {
    /** Forge modId — lowercase; not used for NBT / GUI class lookup. */
    public static final String MOD_ID = "adaptivedifficulty";
    /** Product line version (was DMZ Adaptive Difficulty 3.3.x). */
    public static final String VERSION = "1.0.41";
    public static final String DISPLAY_NAME = "AdaptiveDifficulty";
    public static final Logger LOGGER = LogManager.getLogger(DISPLAY_NAME);

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
