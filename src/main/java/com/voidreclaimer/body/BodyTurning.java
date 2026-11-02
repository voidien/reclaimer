package com.voidreclaimer.body;

import com.voidreclaimer.Config;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A big body's turn: the view moves at once and the body follows, as fast as its weight lets it, pushed off anything in its way. It runs
 * in flight, so a body that isn't flying simply points where it looks. A small body has no state here and always points at its view.
 */
public final class BodyTurning {
    /** Further in a tick than any body flies: it was moved, by a teleport or a link, and starts again at its view. */
    private static final double JUMP = 16.0;
    /** How much faster than the body's rate the server lets a heading arrive, and over how many ticks at most. */
    private static final double TURN_ROOM = 1.5;
    private static final int MOST_TICKS_HEARD = 20;

    /** Where a heavy body points this tick and last, and where it was. Kept only while it flies, so it isn't saved. */
    public record State(@Nullable BodyHeading now, @Nullable BodyHeading before, int tick, @Nullable ResourceKey<Level> level, @Nullable Chassis chassis,
                        @Nullable Vec3 at) {
        public static final State NONE = new State(null, null, 0, null, null, null);
    }

    /**
     * What the server has heard of a body that isn't pointing at its view, for itself and for everyone who can see it.
     *
     * @param lagging whether the body is behind its view at all
     * @param tick    the server's tick count when it heard
     */
    public record Heard(boolean lagging, float yaw, float pitch, int tick) {
        public static final Heard NONE = new Heard(false, 0f, 0f, 0);
        public static final StreamCodec<ByteBuf, Heard> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Heard::lagging,
                ByteBufCodecs.FLOAT, Heard::yaw,
                ByteBufCodecs.FLOAT, Heard::pitch,
                ByteBufCodecs.VAR_INT, Heard::tick,
                Heard::new);

        BodyHeading heading() {
            return new BodyHeading(yaw, pitch);
        }
    }

    private BodyTurning() {}

    /** Turned one tick ago or just now: anything older is a body that stopped flying, and it picks up at its view. */
    private static boolean live(Player player, State state) {
        return state.now() != null && player.tickCount - state.tick() <= 1;
    }

    /** Where {@code player}'s body points: its own turn on the client that flies it, what the server heard anywhere else. */
    public static BodyHeading of(Player player) {
        State state = player.getData(ModAttachments.BODY_TURN);
        if (live(player, state)) {
            return state.now();
        }
        Heard heard = player.getData(ModAttachments.BODY_HEARD);
        return heard.lagging() ? heard.heading() : new BodyHeading(player.getYRot(), player.getXRot());
    }

    /** Where any body points: a player's as {@link #of(Player)}, anything else's along its view. */
    public static BodyHeading of(LivingEntity entity) {
        return entity instanceof Player player ? of(player) : new BodyHeading(entity.getYRot(), entity.getXRot());
    }

    /** Where a heavy body points between ticks, for drawing it; null for a body that points at its view. */
    @Nullable
    public static BodyHeading at(Player player, float partialTick) {
        State state = player.getData(ModAttachments.BODY_TURN);
        if (!live(player, state)) {
            Heard heard = player.getData(ModAttachments.BODY_HEARD);
            return heard.lagging() ? heard.heading() : null;
        }
        return new BodyHeading(Mth.rotLerp(partialTick, state.before().yaw(), state.now().yaw()), Mth.lerp(partialTick, state.before().pitch(), state.now().pitch()));
    }

    /**
     * The flying client says where the body points, or that it has caught up. The client is trusted no further than the body could
     * turn since the last word, with some room for packets arriving in bunches: a body that says more is held to that.
     */
    public static void heard(ServerPlayer player, boolean lagging, float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return;
        }
        double rate = rate(Bodies.view(player).stats());
        if (!lagging || Double.isInfinite(rate)) {
            player.setData(ModAttachments.BODY_HEARD, Heard.NONE);
            return;
        }
        Heard last = player.getData(ModAttachments.BODY_HEARD);
        // Not the view now: the packet that moved it lands first, so it is where the body is turning to. The body starts from where it pointed.
        BodyHeading from = last.lagging() ? last.heading() : player.getData(ModAttachments.FLIGHT_TRACKER).settled(new BodyHeading(player.getYRot(), player.getXRot()));
        int tick = player.getServer().getTickCount();
        int ticks = last.lagging() ? Mth.clamp(tick - last.tick(), 0, MOST_TICKS_HEARD) : 1;
        BodyHeading now = from.atMost(new BodyHeading(yaw, Mth.clamp(pitch, -90f, 90f)), rate * TURN_ROOM * ticks);
        player.setData(ModAttachments.BODY_HEARD, new Heard(true, now.yaw(), now.pitch(), tick));
    }

    /** The most {@code stats} can turn in a tick, in degrees, by the config. */
    private static double rate(BodyStats stats) {
        return BodyHeading.degreesPerTick(stats, Config.BODY_TURN_FACTOR.get(), Config.BODY_TURN_SLOWEST.get(), Config.BODY_TURN_FASTEST.get());
    }

    /** The boxes of {@code view}'s fitted sections at {@code heading}. */
    static List<BodyBoxes.Box> place(Player player, BodyView view, BodyHeading heading) {
        BodyData data = view.data();
        return BodyBoxes.place(view.chassis(), socket -> !data.section(socket).isEmpty(), player.position(), heading.yaw(), heading.pitch(), view.stats().scale());
    }

    /** Turns the body one tick toward the view. Returns how far that pushed it: a move to make, not a speed. */
    public static Vec3 turn(Player player) {
        BodyHeading view = new BodyHeading(player.getYRot(), player.getXRot());
        BodyView body = Bodies.view(player);
        double rate = rate(body.stats());
        if (Double.isInfinite(rate)) {
            player.setData(ModAttachments.BODY_TURN, State.NONE);
            return Vec3.ZERO;
        }
        State state = player.getData(ModAttachments.BODY_TURN);
        ResourceKey<Level> level = player.level().dimension();
        Vec3 at = player.position();
        if (!live(player, state) || !level.equals(state.level()) || !body.chassis().equals(state.chassis()) || state.at().distanceToSqr(at) > JUMP * JUMP) {
            player.setData(ModAttachments.BODY_TURN, new State(view, view, player.tickCount, level, body.chassis(), at));
            return Vec3.ZERO;
        }
        BodyHeading now = state.now();
        BodyHeading next = now.toward(view.yaw(), view.pitch(), rate);
        BodyHeading.Turn turn = next.equals(now) ? new BodyHeading.Turn(now, Vec3.ZERO) : now.turn(next, heading -> place(player, body, heading), blocksFor(player, body, now, next));
        player.setData(ModAttachments.BODY_TURN, new State(turn.heading(), now, player.tickCount, level, body.chassis(), at));
        return turn.push();
    }

    /** Everything near the body where it is and where it would be: a block missing from where it is reads as a body less buried than it is. */
    private static List<BodyBoxes.Box> blocksFor(Player player, BodyView body, BodyHeading now, BodyHeading next) {
        List<BodyBoxes.Box> both = new ArrayList<>(place(player, body, now));
        both.addAll(place(player, body, next));
        return BoxCollision.solidsNear(player, both, Vec3.ZERO, BodyBoxes.MOST_PUSH);
    }
}
