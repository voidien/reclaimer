package com.voidreclaimer.space;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Beds and respawn anchors don't work in deep space ({@code bed_works}/{@code respawn_anchor_works} are false), and
 * vanilla would make them explode. Using them is denied instead, with a lore message. Item use (e.g. placing a
 * block against one) still goes through.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class SpawnDeviceBlocker {
    private SpawnDeviceBlocker() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!ModDimensions.isDeepSpace(level)) {
            return;
        }
        Block block = level.getBlockState(event.getPos()).getBlock();
        if (!(block instanceof BedBlock) && !(block instanceof RespawnAnchorBlock)) {
            return;
        }
        event.setUseBlock(TriState.FALSE);
        boolean wouldUseBlock = !event.getEntity().isSecondaryUseActive() || event.getItemStack().isEmpty();
        if (!level.isClientSide() && wouldUseBlock) {
            event.getEntity().displayClientMessage(Component.translatable("message.void_reclaimer.no_planetary_anchor"), true);
        }
    }
}
