package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.character.Character;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Builds a non-spawned CNPC Gecko clone for menu preview (same pipeline SDU uses for saga / form previews).
 */
final class CnpcGeckoPreviewBridge {
    private static final String GECKO_MOD = "cnpcgeckoaddon";
    private static final String SDU_BRIDGE = "net.shurui.dev.sdu.compat.cnpc.CnpcGeckoBridge";
    private static final String SDU_HAIR = "net.shurui.dev.sdu.compat.cnpc.SduHairHolder";
    private static final ResourceLocation GECKO_ENTITY =
            new ResourceLocation("cnpcgeckoaddon", "custommodelentity");

    private CnpcGeckoPreviewBridge() {}

    static boolean geckoAvailable() {
        return ModList.get().isLoaded(GECKO_MOD);
    }

    static IEntity previewEntity(ServerPlayer player) {
        if (player == null || !NpcAPI.IsAvailable() || !geckoAvailable()) {
            return null;
        }
        try {
            ServerLevel level = (ServerLevel) player.m_9236_();
            ICustomNpc npcApi = NpcAPI.Instance().createNPC(level);
            if (!(npcApi.getMCEntity() instanceof EntityCustomNpc npc)) {
                return null;
            }
            Character ch = DmzProgression.character(player);
            if (ch != null) {
                try {
                    var stats = DmzProgression.stats(player);
                    if (stats != null && stats.getCharacter() != null) {
                        ch = stats.getCharacter();
                    }
                } catch (Throwable ignored) {
                }
            }
            String geo = DmzPreviewGeo.resolveModelGeo(ch);
            applyDmzModel(npc, geo, DmzPreviewGeo.SAGA_BASE_ANIM, "idle", "walk", "attack1_1", "hurt", null);
            applyPlayerSkin(npc, player.m_36316_().getName());
            applyHair(npc, ch);
            npc.display.setShowName(0);
            try {
                npc.ais.orientation = 2;
            } catch (Throwable ignored) {
            }
            npc.m_146922_(180.0f);
            npc.m_146926_(0.0f);
            npc.updateClient();
            return NpcAPI.Instance().getIEntity(npc);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] Gecko menu preview failed: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
            return null;
        }
    }

    private static void applyDmzModel(
            EntityCustomNpc npc,
            String geo,
            String animFile,
            String idle,
            String walk,
            String attack,
            String hurt,
            String texture) throws ReflectiveOperationException {
        if (ModList.get().isLoaded("sdu")) {
            Class<?> bridge = Class.forName(SDU_BRIDGE);
            Method m = bridge.getMethod(
                    "applyDmzModel",
                    EntityCustomNpc.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    String.class);
            m.invoke(null, npc, geo, animFile, idle, walk, attack, hurt, texture);
            return;
        }
        npc.modelData.setEntity(GECKO_ENTITY);
        Object display = npc.display;
        Class<?> idata = Class.forName("com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay");
        if (!idata.isInstance(display)) {
            throw new IllegalStateException("CNPC display is not Gecko IDataDisplay");
        }
        Method getCmd = idata.getMethod("getCustomModelData");
        Object cmd = getCmd.invoke(display);
        Class<?> cmdClass = Class.forName("com.goodbird.cnpcgeckoaddon.data.CustomModelData");
        cmdClass.getMethod("setModel", String.class).invoke(cmd, geo);
        cmdClass.getMethod("setAnimFile", String.class).invoke(cmd, animFile);
        cmdClass.getMethod("setIdleAnim", String.class).invoke(cmd, idle);
        cmdClass.getMethod("setWalkAnim", String.class).invoke(cmd, walk);
        cmdClass.getMethod("setAttackAnim", String.class).invoke(cmd, attack);
        cmdClass.getMethod("setHurtAnim", String.class).invoke(cmd, hurt);
    }

    private static void applyPlayerSkin(EntityCustomNpc npc, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        try {
            if (ModList.get().isLoaded("sdu")) {
                Class<?> bridge = Class.forName(SDU_BRIDGE);
                Method m = bridge.getMethod("applyPlayerSkin", EntityCustomNpc.class, String.class);
                m.invoke(null, npc, playerName);
                return;
            }
            npc.display.skinType = 1;
            npc.display.setSkinPlayer(playerName);
            npc.display.loadProfile();
            npc.textureLocation = null;
        } catch (Throwable ignored) {
        }
    }

    private static void applyHair(EntityCustomNpc npc, Character ch) {
        if (ch == null) {
            return;
        }
        try {
            Class<?> holder = Class.forName(SDU_HAIR);
            if (!holder.isInstance(npc.display)) {
                return;
            }
            var hair = HairManager.getEffectiveHair(ch);
            String code = hair == null ? "" : HairManager.toCode(hair);
            String color = ch.getHairColor();
            if (color == null) {
                color = "";
            }
            holder.getMethod("sdu$setHairCode", String.class).invoke(npc.display, code);
            holder.getMethod("sdu$setHairColor", String.class).invoke(npc.display, color);
        } catch (Throwable ignored) {
        }
    }
}
