package com.dbzlegacy.adaptivedifficulty;

import com.dbzlegacy.adaptivedifficulty.command.DifficultyCommands;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.event.DifficultyEvents;
import com.dbzlegacy.adaptivedifficulty.network.DifficultyNet;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AdaptiveDifficultyMod.MOD_ID)
public final class AdaptiveDifficultyMod {
    public static final String MOD_ID = "dmz_adaptive_difficulty";
    public static final String VERSION = "1.3.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AdaptiveDifficultyMod() {
        DifficultyConfig.load();
        DifficultyNet.register();
        MinecraftForge.EVENT_BUS.register(new DifficultyEvents());
        DifficultyCommands.register();
        LOGGER.info(
                "[{}] v{} GUI+FTB+Lightmans: Lightman's={}, FTB Teams={}",
                MOD_ID,
                VERSION,
                CurrencyBridge.lightmansAvailable(),
                TeamScaling.ftbAvailable()
        );
    }
}
