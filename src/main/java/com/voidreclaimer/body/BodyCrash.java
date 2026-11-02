package com.voidreclaimer.body;

import com.voidreclaimer.Config;
import com.voidreclaimer.api.event.BodyCrashEvent;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.flight.ServerFlightHandler;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.tether.Tether;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A body flying into something. Flight runs on the client, so the flying client says how much speed it lost, and the server takes that as
 * far as the body could have been going. Under the safe speed nothing happens. Above it the body is hurt, and a big body breaks the world's
 * blocks it hit, the weak ones first. A ship's blocks are left alone here: they go with a ship's own crashes.
 */
public final class BodyCrash {
    /** How far ahead of a body a hit reaches, in blocks: what it ploughs into, beyond what it touched. */
    private static final double REACH = 1.0;
    /** A client's word goes this far over top speed, for the wind and a body still slowing from a boost. */
    private static final double SPEED_ROOM = 1.5;

    private BodyCrash() {}

    /** {@code lost} is the speed that went into what it hit, in blocks a tick. */
    public static void heard(ServerPlayer player, Vec3 lost) {
        // The winch stops on the anchor's middle, and a long body's nose gets there first: that is the winch's doing, not the pilot's.
        if (!Double.isFinite(lost.lengthSqr()) || !player.isAlive() || player.isCreative() || player.isSpectator() || Tether.data(player).recalling()) {
            return;
        }
        double perSecond = FlightStats.TICKS_PER_SECOND;
        double top = FlightStats.of(player).maxSpeed() * perSecond;
        double speed = Math.min(onlyTheHit(player, lost).length() * perSecond, Math.max(top, ServerFlightHandler.MOST_WIND) * SPEED_ROOM);
        double safe = Config.CRASH_SAFE_SPEED.get();
        if (speed <= safe) {
            return;
        }
        float damage = (float) (CrashRules.healthShare(speed, top, safe, Config.CRASH_AT_TOP_SPEED.get()) * player.getMaxHealth());
        boolean big = Bodies.view(player).chassis().sockets().stream().anyMatch(socket -> !socket.boxes().isEmpty());
        BodyCrashEvent event = new BodyCrashEvent(player, speed, damage, big && Config.CRASH_BREAKS_BLOCKS.get());
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }
        if (event.breaksBlocks()) {
            breakBlocks(player, lost, speed, safe);
        }
        if (event.getDamage() > 0f) {
            player.hurt(player.serverLevel().damageSources().flyIntoWall(), event.getDamage());
        }
    }

    /** Something flew into a still body at {@code speed} blocks a second, taking {@code share} of its health: a part can soften this as it can a crash. */
    public static void struck(ServerPlayer player, double speed, double share) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        BodyCrashEvent event = new BodyCrashEvent(player, speed, (float) (share * player.getMaxHealth()), false);
        if (!NeoForge.EVENT_BUS.post(event).isCanceled() && event.getDamage() > 0f) {
            player.hurt(player.serverLevel().damageSources().flyIntoWall(), event.getDamage());
        }
    }

    /** A fall vanilla will charge for is not charged again. */
    private static Vec3 onlyTheHit(ServerPlayer player, Vec3 lost) {
        return lost.y < 0.0 && ServerFlightHandler.fallHurts(player) ? new Vec3(lost.x, 0.0, lost.z) : lost;
    }

    /** The weakest first, and only what this hit can break: a Ship that clips a station takes the glass, and leaves the frame. */
    private static void breakBlocks(ServerPlayer player, Vec3 lost, double speed, double safe) {
        ServerLevel level = player.serverLevel();
        double mass = Bodies.stats(player).mass();
        double hardest = CrashRules.hardness(speed, mass, safe);
        int most = CrashRules.blocks(speed, mass, safe, Config.CRASH_MOST_BLOCKS.get());
        Vec3 ahead = lost.normalize().scale(REACH);
        List<BlockPos> hit = BoxCollision.blocksAhead(player, ahead).stream()
                .filter(pos -> breaks(level, pos, hardest))
                .sorted(Comparator.comparingDouble((BlockPos pos) -> level.getBlockState(pos).getDestroySpeed(level, pos))
                        .thenComparingDouble(pos -> pos.distToCenterSqr(player.position())))
                .limit(most).toList();
        hit.forEach(pos -> level.destroyBlock(pos, true, player));
    }

    private static boolean breaks(ServerLevel level, BlockPos pos, double hardest) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0f && hardness <= hardest && !state.is(ModBlocks.CRASH_PROOF);
    }

    /** Whether something hitting the block at {@code pos} as hard as {@code hardest} breaks it. */
    public static boolean canBreak(ServerLevel level, BlockPos pos, double hardest) {
        return !level.getBlockState(pos).isAir() && breaks(level, pos, hardest);
    }
}
