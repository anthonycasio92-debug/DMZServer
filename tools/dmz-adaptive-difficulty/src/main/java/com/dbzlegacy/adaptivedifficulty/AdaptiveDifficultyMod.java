package com.dbzlegacy.adaptivedifficulty;

import com.dbzlegacy.adaptivedifficulty.command.DifficultyCommands;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.event.DifficultyEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AdaptiveDifficultyMod.MOD_ID)
public final class AdaptiveDifficultyMod {
    public static final String MOD_ID = "dmz_adaptive_difficulty";
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AdaptiveDifficultyMod() {
        DifficultyConfig.load();
        MinecraftForge.EVENT_BUS.register(new DifficultyEvents());
        DifficultyCommands.register();
        LOGGER.info(
                "[{}] v{} concept-complete: Lightman's={}, GUI, evolution/elites/mutations/AI/boss/rewards",
                MOD_ID,
                VERSION,
                CurrencyBridge.lightmansAvailable()
        );
    }
}
