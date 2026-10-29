package com.voidreclaimer.client.input;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.nanite.NaniteTargeting;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.network.BodyActionPayload;
import com.voidreclaimer.network.NaniteRangePayload;
import com.voidreclaimer.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Nanite Swarm module on the client, active when it is the selected tool and the main hand is empty:
 * <ul>
 *   <li>right-click in empty space (or sneak + right-click on a block) asks the server to build;</li>
 *   <li>a plain white outline shows where the block will go;</li>
 *   <li>sneak + scroll changes the build distance.</li>
 * </ul>
 * The outline is drawn through a NeoForge render stage (no chunk rendering changes, so Sodium is unaffected).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientNaniteController {
    private ClientNaniteController() {}

    private static boolean naniteActive(LocalPlayer player) {
        return Modules.isActive(player, ModItems.NANITE_SWARM.get());
    }

    /** Sneaking, read from the key itself: thruster flight clears the vanilla sneak flag (Shift is "thrust down"). */
    private static boolean sneakHeld(Minecraft mc) {
        return mc.options.keyShift.isDown();
    }

    /** Where the swarm would build right now, or null when it wouldn't. */
    @Nullable
    static BlockPos ghostTarget(Minecraft mc, LocalPlayer player) {
        if (!naniteActive(player) || mc.screen != null || Nanites.source(player) == null) {
            return null;
        }
        HitResult hit = mc.hitResult;
        BlockPos pos;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (!sneakHeld(mc)) {
                return null;
            }
            pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            pos = Nanites.target(player, Modules.naniteRange(player));
        }
        return player.level().getBlockState(pos).canBeReplaced() ? pos : null;
    }

    @SubscribeEvent
    static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || player == null || !naniteActive(player)) {
            return;
        }
        boolean onBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        if (onBlock && !sneakHeld(mc)) {
            return; // a plain right-click on a block uses the block
        }
        event.setCanceled(true);
        if (!player.getCooldowns().isOnCooldown(ModItems.NANITE_SWARM.get())) {
            event.setSwingHand(true);
            PacketDistributor.sendToServer(BodyActionPayload.useTool());
        } else {
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        BlockPos target = ghostTarget(mc, mc.player);
        if (target == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        AABB box = new AABB(target).move(-cam.x, -cam.y, -cam.z).inflate(0.002);
        LevelRenderer.renderLineBox(poseStack, lines, box, 1f, 1f, 1f, 0.8f);
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !sneakHeld(mc) || !naniteActive(player)) {
            return;
        }
        int current = Modules.naniteRange(player);
        int next = NaniteTargeting.adjustRange(current, event.getScrollDeltaY());
        event.setCanceled(true);
        if (next != current) {
            PacketDistributor.sendToServer(new NaniteRangePayload(next));
            player.displayClientMessage(Component.translatable("message.void_reclaimer.nanite.range", next), true);
        }
    }
}
