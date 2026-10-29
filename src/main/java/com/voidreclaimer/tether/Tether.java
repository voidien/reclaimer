package com.voidreclaimer.tether;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of the Tether Anchor.
 *
 * <ul>
 *   <li>Clip: right-click an anchor to leash to it (it also becomes the recall point); again to unclip. The leash
 *       itself is physics, run by the client in {@link com.voidreclaimer.flight.ThrusterFlight}.</li>
 *   <li>Recall (hold the recall key): within {@link Config#RECALL_PULL_RANGE} a hold of {@link #PULL_HOLD_TICKS}
 *       starts the winch, a fast pull toward the anchor that lasts while the key is held; farther away a hold of
 *       {@link #TELEPORT_HOLD_TICKS} teleports to the anchor (energy cost and cooldown). Same dimension only.</li>
 * </ul>
 * Logic lives here rather than in the block so Phase 5a can drive it from a body module.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Tether {
    public static final int PULL_HOLD_TICKS = 20;
    public static final int TELEPORT_HOLD_TICKS = 60;
    /** Within this distance of the anchor the winch stops. */
    public static final double ARRIVE_DISTANCE = 2.5;
    private static final int VALIDATE_TICKS = 20;

    private Tether() {}

    public static TetherData data(Player player) {
        return player.getData(ModAttachments.TETHER);
    }

    private static void store(ServerPlayer player, TetherData data) {
        if (!data.equals(data(player))) {
            player.setData(ModAttachments.TETHER, data);
            player.syncData(ModAttachments.TETHER);
        }
    }

    public static void toggleClip(ServerPlayer player, BlockPos pos) {
        GlobalPos anchor = GlobalPos.of(player.level().dimension(), pos.immutable());
        TetherData data = data(player);
        if (data.clipped() && data.isAnchoredAt(anchor)) {
            store(player, data.withClipped(false));
            message(player, "message.void_reclaimer.tether.unclipped", ChatFormatting.GRAY);
        } else {
            store(player, data.withClip(anchor));
            message(player, "message.void_reclaimer.tether.clipped", ChatFormatting.AQUA);
        }
    }

    /** Frees everyone tethered to an anchor that was just removed and forgets it as their recall point. */
    public static void onAnchorRemoved(ServerLevel level, BlockPos pos) {
        GlobalPos anchor = GlobalPos.of(level.dimension(), pos);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (data(player).isAnchoredAt(anchor)) {
                store(player, TetherData.NONE.withTeleportReadyAt(data(player).teleportReadyAt()));
                message(player, "message.void_reclaimer.tether.lost", ChatFormatting.GOLD);
            }
        }
    }

    /** The anchor's center if it is in the player's current dimension. */
    @Nullable
    public static Vec3 anchorPoint(Player player) {
        Optional<GlobalPos> anchor = data(player).anchor();
        if (anchor.isEmpty() || anchor.get().dimension() != player.level().dimension()) {
            return null;
        }
        return Vec3.atCenterOf(anchor.get().pos());
    }

    public static boolean isRecalling(Player player) {
        return data(player).recalling() && anchorPoint(player) != null;
    }

    /** Recall winch top speed for this player, in blocks/tick. */
    public static double recallSpeed(Player player) {
        return FlightStats.of(player).maxSpeed() * Config.RECALL_SPEED_FACTOR.get();
    }

    /** Called from the recall key payload. */
    public static void setRecallHeld(ServerPlayer player, boolean held) {
        RecallState state = player.getData(ModAttachments.RECALL_STATE);
        state.holding = held;
        if (!held) {
            state.holdTicks = 0;
            store(player, data(player).withRecalling(false));
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % VALIDATE_TICKS == 0) {
            validate(player);
        }
        RecallState state = player.getData(ModAttachments.RECALL_STATE);
        if (state.holding) {
            state.holdTicks++;
            tickRecall(player, state);
        }
    }

    private static void tickRecall(ServerPlayer player, RecallState state) {
        TetherData data = data(player);
        Vec3 anchor = anchorPoint(player);
        if (anchor == null) {
            if (state.holdTicks == 1) {
                message(player, data.anchor().isEmpty() ? "message.void_reclaimer.tether.no_anchor"
                        : "message.void_reclaimer.tether.other_dimension", ChatFormatting.GRAY);
            }
            return;
        }
        if (!Energy.canPowerSystems(player)) {
            if (state.holdTicks == 1 || data.recalling()) {
                message(player, "message.void_reclaimer.tether.no_power", ChatFormatting.RED);
            }
            store(player, data.withRecalling(false));
            return;
        }
        double distance = player.getBoundingBox().getCenter().distanceTo(anchor);
        if (data.recalling()) {
            if (distance <= ARRIVE_DISTANCE) {
                store(player, data.withRecalling(false));
                state.holding = false;
                state.holdTicks = 0;
                return;
            }
            Energy.consume(player, (float) (Config.RECALL_PULL_ENERGY.get() / FlightStats.TICKS_PER_SECOND));
            return;
        }
        if (!TetherPhysics.usesTeleport(distance, Config.RECALL_PULL_RANGE.get())) {
            if (state.holdTicks >= PULL_HOLD_TICKS && distance > ARRIVE_DISTANCE) {
                store(player, data.withRecalling(true));
            }
        } else if (state.holdTicks == TELEPORT_HOLD_TICKS) {
            teleport(player, data);
        }
    }

    private static void teleport(ServerPlayer player, TetherData data) {
        long now = player.level().getGameTime();
        if (now < data.teleportReadyAt() && !Energy.isExempt(player)) {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.tether.cooldown",
                    (data.teleportReadyAt() - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        float cost = Config.RECALL_TELEPORT_ENERGY.get().floatValue();
        EnergyData energy = player.getData(ModAttachments.ENERGY);
        if (!Energy.isExempt(player) && energy.energy() < cost) {
            message(player, "message.void_reclaimer.tether.no_power", ChatFormatting.RED);
            return;
        }
        BlockPos anchorPos = data.anchor().orElseThrow().pos();
        // validate() skips unloaded chunks, so the anchor may have been removed while its owner was away.
        ServerLevel anchorLevel = player.server.getLevel(data.anchor().get().dimension());
        if (anchorLevel == null || !anchorLevel.getBlockState(anchorPos).is(ModBlocks.TETHER_ANCHOR.get())) {
            store(player, TetherData.NONE.withTeleportReadyAt(data.teleportReadyAt()));
            message(player, "message.void_reclaimer.tether.lost", ChatFormatting.GOLD);
            return;
        }
        BlockPos spot = freeSpotNear(player.level(), anchorPos);
        if (spot == null) {
            message(player, "message.void_reclaimer.tether.blocked", ChatFormatting.GRAY);
            return;
        }
        Energy.consume(player, cost);
        player.teleportTo(player.serverLevel(), spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.getData(ModAttachments.FLIGHT_TRACKER).clear();
        store(player, data.withTeleportReadyAt(now + Config.RECALL_TELEPORT_COOLDOWN.get() * 20L));
        message(player, "message.void_reclaimer.tether.teleported", ChatFormatting.AQUA);
    }

    /** A two-block-tall free spot next to {@code pos}: above first, then the sides, then below. */
    @Nullable
    public static BlockPos freeSpotNear(Level level, BlockPos pos) {
        Direction[] order = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
        for (Direction direction : order) {
            BlockPos feet = direction == Direction.DOWN ? pos.below(2) : pos.relative(direction);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }

    /** Drops a stale anchor (removed while nobody was around) and unclips across dimensions. */
    private static void validate(ServerPlayer player) {
        TetherData data = data(player);
        if (data.anchor().isEmpty()) {
            return;
        }
        GlobalPos anchor = data.anchor().get();
        ServerLevel level = player.server.getLevel(anchor.dimension());
        if (level == null || (level.isLoaded(anchor.pos()) && !level.getBlockState(anchor.pos()).is(ModBlocks.TETHER_ANCHOR.get()))) {
            store(player, TetherData.NONE.withTeleportReadyAt(data.teleportReadyAt()));
            return;
        }
        if (data.clipped() && anchor.dimension() != player.level().dimension()) {
            store(player, data.withClipped(false).withRecalling(false));
        }
    }

    /** A fresh body isn't clipped to anything, and nothing is mid-recall after a death or a relog. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            store(player, data(player).withClipped(false).withRecalling(false));
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            store(player, data(player).withRecalling(false));
        }
    }

    private static void message(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
