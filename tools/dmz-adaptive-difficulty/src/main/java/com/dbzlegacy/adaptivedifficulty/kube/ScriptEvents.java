package com.dbzlegacy.adaptivedifficulty.kube;

import com.dbzlegacy.adaptivedifficulty.kube.hotpath.CapsuleBan;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.HealEventCap;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.HeartstopCast;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.HotLog;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.RecipeStrip;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.SilkyBan;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.SpawnerVanillaize;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.WeaponBaseDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The non-tick half of the old KubeJS hot-path scripts: recipes, item use,
 * spawner comparator and chunk load, base weapon damage, and heal-event caps.
 */
public final class ScriptEvents {
    public static final ScriptEvents INSTANCE = new ScriptEvents();

    private ScriptEvents() {
        HeartstopCast.hook();
    }

    @SubscribeEvent
    public void onTags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        RecipeStrip.strip();
    }

    @SubscribeEvent
    public void onAttributes(ItemAttributeModifierEvent event) {
        WeaponBaseDamage.onAttributes(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHeal(LivingHealEvent event) {
        HealEventCap.onHeal(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        HealEventCap.onHurt(event);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getChunk() == null) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        SpawnerVanillaize.vanillaizeChunk(level, chunk.m_7697_().f_45578_, chunk.m_7697_().f_45579_);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!isSpawner(event.getPlacedBlock())) {
            return;
        }
        SpawnerVanillaize.vanillaizeAt(level, event.getPos());
    }

    @SubscribeEvent
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().m_5776_() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!isSpawner(level.m_8055_(pos))) {
            return;
        }
        ItemStack used = event.getItemStack();
        if (!"minecraft:comparator".equals(itemId(used))) {
            return;
        }
        boolean enable = true;
        ItemStack other = event.getHand() == InteractionHand.OFF_HAND ? player.m_21205_() : player.m_21206_();
        if ("minecraft:quartz".equals(itemId(other))) {
            enable = false;
        }
        BlockEntity be = level.m_7702_(pos);
        if (be != null && SpawnerVanillaize.redstoneEnabled(be) == enable) {
            HotLog.tell(
                    player,
                    enable
                            ? "This spawner already has redstone control. Power it to spawn."
                            : "This spawner already has no redstone control.");
            event.setCanceled(true);
            return;
        }
        if (!SpawnerVanillaize.setRedstone(be, enable)) {
            return;
        }
        if (!player.m_7500_()) {
            used.m_41774_(1);
        }
        HotLog.tell(
                player,
                enable ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY,
                enable
                        ? "Spawner redstone control enabled. Power it to spawn."
                        : "Spawner redstone control removed.");
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().m_5776_() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (CapsuleBan.onUse(event, player)) {
            return;
        }
        SilkyBan.onUse(event, player);
    }

    private static boolean isSpawner(BlockState state) {
        if (state == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.m_60734_());
        return id != null && "minecraft:spawner".equals(id.toString());
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return id == null ? "" : id.toString();
    }
}
