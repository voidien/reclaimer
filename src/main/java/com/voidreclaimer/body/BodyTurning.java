package com.voidreclaimer.body;

import com.voidreclaimer.Config;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
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

    /** Where a heavy body points this tick and last, and where it was. Kept only while it flies, so it isn't saved. */
    public record State(@Nullable BodyHeading now, @Nullable BodyHeading before, int tick, @Nullable ResourceKey<Level> level, @Nullable Chassis chassis,
                        @Nullable Vec3 at) {
        public static final State NONE = new State(null, null, 0, null, null, null);
    }

    private BodyTurning() {}

    /** Turned one tick ago or just now: anything older is a body that stopped flying, and it picks up at its view. */
    private static boolean live(Player player, State state) {
        return state.now() != null && player.tickCount - state.tick() <= 1;
    }

    /** Where {@code player}'s body points. */
    public static BodyHeading of(Player player) {
        State state = player.getData(ModAttachments.BODY_TURN);
        return live(player, state) ? state.now() : new BodyHeading(player.getYRot(), player.getXRot());
    }

    /** Where a heavy body points between ticks, for drawing it; null for a body that points at its view. */
    @Nullable
    public static BodyHeading at(Player player, float partialTick) {
        State state = player.getData(ModAttachments.BODY_TURN);
        if (!live(player, state)) {
            return null;
        }
        return new BodyHeading(Mth.rotLerp(partialTick, state.before().yaw(), state.now().yaw()), Mth.lerp(partialTick, state.before().pitch(), state.now().pitch()));
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
        double rate = BodyHeading.degreesPerTick(body.stats(), Config.BODY_TURN_FACTOR.get(), Config.BODY_TURN_SLOWEST.get(), Config.BODY_TURN_FASTEST.get());
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
        return BoxCollision.blocksNear(player.level(), both, Vec3.ZERO, BodyBoxes.MOST_PUSH);
    }
}
