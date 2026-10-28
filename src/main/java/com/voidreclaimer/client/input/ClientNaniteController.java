package com.voidreclaimer.client.input;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.nanite.NaniteSwarmItem;
import com.voidreclaimer.nanite.NaniteTargeting;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.network.NaniteRangePayload;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
 * Nanite Swarm on the client: a plain white outline where the next block will go, and sneak + scroll to change
 * the build distance. Drawn through a NeoForge render stage (no chunk rendering changes, so Sodium is unaffected).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientNaniteController {
    private ClientNaniteController() {}

    /** Where the swarm would build right now, or null when it can't. */
    @Nullable
    static BlockPos ghostTarget(Minecraft mc, LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NaniteSwarmItem) || mc.screen != null || Nanites.source(player) == null) {
            return null;
        }
        HitResult hit = mc.hitResult;
        BlockPos pos = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                ? blockHit.getBlockPos().relative(blockHit.getDirection())
                : Nanites.target(player, Nanites.range(stack));
        return player.level().getBlockState(pos).canBeReplaced() ? pos : null;
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
        // Read the key itself: thruster flight clears the vanilla sneak flag (Shift is "thrust down").
        if (player == null || mc.screen != null || !mc.options.keyShift.isDown()
                || !(player.getMainHandItem().getItem() instanceof NaniteSwarmItem)) {
            return;
        }
        int current = Nanites.range(player.getMainHandItem());
        int next = NaniteTargeting.adjustRange(current, event.getScrollDeltaY());
        event.setCanceled(true);
        if (next != current) {
            PacketDistributor.sendToServer(new NaniteRangePayload(next));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.void_reclaimer.nanite.range", next), true);
        }
    }
}
