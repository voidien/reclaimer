package com.voidreclaimer.ship;

import com.voidreclaimer.flight.FlightSettings;
import com.voidreclaimer.network.HelmReadoutPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.PacketDistributor;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlockEntities;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Helm's brain. Every tick with a pilot in the chair it looks at the ship, lists its thrusters and sets their throttles
 * from the pilot's keys ({@link PilotInput}, synced from the client) through {@link ThrustAllocator}. The ship's forward is
 * the way the chair faces; right and up follow. Without a ship (the Helm stands in the world) it does nothing.
 */
public class HelmBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String CONTROLLER = "helm";
    /** Below this speed (blocks per second) or spin (radians per second) a hold treats the ship as stopped. */
    static final double HOLD_DEADBAND = 0.05;
    /** The speed or spin at which a hold reaches full throttle; below it the hold eases off in proportion. */
    static final double HOLD_RAMP = 1.0;
    public static final String SIT_ANIMATION = "turn_180";
    private static final RawAnimation SIT = RawAnimation.begin().thenPlay("animation.void_reclaimer.helm." + SIT_ANIMATION);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** What the Helm found last tick, for the HUD and the peripheral. */
    private volatile Readout readout = Readout.EMPTY;
    /**
     * The seat this Helm spawned (server only, not saved). Kept here because an entity added to a chunk that is not yet
     * entity-ticking is invisible to entity lookups until the chunk is, which would lose the pilot for those ticks;
     * {@link HelmSeats} keeps such a seat moving.
     */
    @Nullable
    private HelmSeat seat;

    /** What the Helm saw last tick: whether it is on a ship, how many thrusters, and what the ship can do. */
    /**
     * What the Helm sees of its ship, for the HUD and the peripheral. {@code move} and {@code turn} are the six-way
     * coverage of the thrusters (see {@link ThrustAllocator#translationCoverage}); {@code velocity} is the ship's speed
     * along the Helm's forward, right and up axes in blocks per second; {@code spin} is its turning rate as the pilot
     * reads it, roll right, nose up and yaw right, in degrees per second.
     */
    public record Readout(boolean onShip, int thrusters, double[] move, double[] turn, double fuel, double fuelCapacity,
                          int energy, int energyCapacity, double[] velocity, double[] spin, Attitude attitude) {
        public static final Readout EMPTY = new Readout(false, 0, new double[6], new double[6], 0, 0, 0, 0, new double[3], new double[3], Attitude.LEVEL);
        public static final StreamCodec<ByteBuf, Readout> STREAM_CODEC = StreamCodec.of(Readout::write, Readout::read);

        public double speed() {
            return Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
        }

        private static void write(ByteBuf buf, Readout r) {
            buf.writeBoolean(r.onShip);
            buf.writeInt(r.thrusters);
            floats(buf, r.move);
            floats(buf, r.turn);
            buf.writeFloat((float) r.fuel);
            buf.writeFloat((float) r.fuelCapacity);
            buf.writeInt(r.energy);
            buf.writeInt(r.energyCapacity);
            floats(buf, r.velocity);
            floats(buf, r.spin);
            Attitude.STREAM_CODEC.encode(buf, r.attitude);
        }

        private static Readout read(ByteBuf buf) {
            return new Readout(buf.readBoolean(), buf.readInt(), floats(buf, 6), floats(buf, 6), buf.readFloat(), buf.readFloat(),
                    buf.readInt(), buf.readInt(), floats(buf, 3), floats(buf, 3), Attitude.STREAM_CODEC.decode(buf));
        }

        private static void floats(ByteBuf buf, double[] values) {
            for (double v : values) {
                buf.writeFloat((float) v);
            }
        }

        private static double[] floats(ByteBuf buf, int count) {
            double[] out = new double[count];
            for (int i = 0; i < count; i++) {
                out[i] = buf.readFloat();
            }
            return out;
        }
    }

    public HelmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HELM.get(), pos, state);
    }

    public Readout readout() {
        return readout;
    }

    /** Puts {@code player} in the chair, unless someone is already sitting. True when they are now riding the seat. */
    public boolean seat(Player player) {
        if (!(level instanceof ServerLevel server) || pilot() != null) {
            return false;
        }
        // On a ship the Helm's block position is in the ship's plot; the seat floats where the Helm is in the world and
        // follows it from there (see HelmSeat.tick).
        Vec3 where = HelmSeat.worldSpot(server, worldPosition);
        if (where == null) {
            return false;
        }
        HelmSeat seat = HelmSeat.at(server, worldPosition, where);
        if (!server.addFreshEntity(seat) || !player.startRiding(seat, true)) {
            seat.discard();
            return false;
        }
        this.seat = seat;
        triggerAnim(CONTROLLER, SIT_ANIMATION);
        return true;
    }

    /** Who sits in the chair, or null. */
    @Nullable
    public Player pilot() {
        if (level == null) {
            return null;
        }
        if (seat != null && (seat.isRemoved() || !seat.helmPos().equals(worldPosition))) {
            seat = null;
        }
        if (seat != null) {
            return seat.getFirstPassenger() instanceof Player player ? player : null;
        }
        // A seat restored from a save was not spawned here, and an entity lookup may not see it yet: when its pilot logs back
        // in, its chunk has only just loaded. HelmSeats knows every live seat either way.
        seat = HelmSeats.of(level, worldPosition);
        return seat != null && seat.getFirstPassenger() instanceof Player player ? player : null;
    }

    /** The ship's axes in its own coordinates: forward is the chair's facing, up is up, right follows (pure, see {@link #axes}). */
    public ThrustAllocator.Frame frame(@Nullable ServerSubLevel ship) {
        Direction facing = getBlockState().getValue(HelmBlock.FACING);
        Vector3d[] axes = axes(facing);
        Vector3d forward = axes[0], right = axes[1], up = axes[2];
        Vector3d centre = ship != null && ship.getMassTracker() != null ? new Vector3d(ship.getMassTracker().getCenterOfMass())
                : new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        return new ThrustAllocator.Frame(forward, right, up, centre);
    }

    /**
     * {forward, right, up} for a chair facing {@code facing}. Minecraft's world is left-handed (+X east, +Z south, +Y up),
     * so the pilot's right is {@code forward x up}: north gives east, east gives south.
     */
    public static Vector3d[] axes(Direction facing) {
        Vector3d forward = Ships.unit(facing);
        Vector3d up = new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(forward).cross(up);
        return new Vector3d[] {forward, right, up};
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HelmBlockEntity helm) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Player pilot = helm.pilot();
        ServerSubLevel ship = Ships.shipAt(server, pos);
        if (pilot == null || ship == null) {
            helm.readout = ship == null ? Readout.EMPTY : helm.look(server, ship, List.of(), new Vector3d(), new Vector3d());
            helm.tell(pilot);
            return;
        }
        List<ThrusterLike> thrusters = Ships.thrustersOf(server, ship);
        PilotInput input = pilot instanceof ServerPlayer p ? p.getData(ModAttachments.PILOT_INPUT) : PilotInput.NONE;
        List<ThrustAllocator.Thruster> model = new ArrayList<>(thrusters.size());
        for (ThrusterLike t : thrusters) {
            model.add(new ThrustAllocator.Thruster(new Vector3d(t.getBlockPos().getX() + 0.5, t.getBlockPos().getY() + 0.5, t.getBlockPos().getZ() + 0.5),
                    Ships.unit(t.pushDirection()), t.maxForce()));
        }
        ThrustAllocator.Frame frame = helm.frame(ship);
        double[] throttles = ThrustAllocator.throttles(model, frame, input);
        // The ship's velocity and spin are in the world; the frame is the ship's own, so they are turned into it first.
        Vector3d velocity = new Vector3d();
        Vector3d spin = new Vector3d();
        RigidBodyHandle body = RigidBodyHandle.of(ship);
        if (body != null && body.isValid()) {
            Quaterniond toShip = new Quaterniond(Ships.orientation(ship)).conjugate();
            toShip.transform(body.getLinearVelocity(velocity));
            toShip.transform(body.getAngularVelocity(spin));
        }
        // The holds: with the dampener on, brake to a stop; with the RCS lock on, stop spinning. Each only on the axes
        // the pilot is not using, so a key always wins.
        FlightSettings settings = pilot instanceof ServerPlayer p ? p.getData(ModAttachments.FLIGHT_SETTINGS) : FlightSettings.DEFAULT;
        if (settings.dampener() || settings.rcsLock()) {
            Vector3d[] hold = ThrustAllocator.holdRequest(frame, input, velocity, spin, settings.dampener(), settings.rcsLock(), HOLD_DEADBAND, HOLD_RAMP);
            double[] holding = ThrustAllocator.throttles(model, frame, hold[0], hold[1]);
            for (int i = 0; i < throttles.length; i++) {
                throttles[i] = Math.max(throttles[i], holding[i]);
            }
        }
        for (int i = 0; i < thrusters.size(); i++) {
            thrusters.get(i).setCommandedThrottle(throttles[i]);
        }
        helm.readout = helm.look(server, ship, thrusters, velocity, spin);
        helm.tell(pilot);
    }

    /** Sends the readout to the pilot for the HUD. */
    private void tell(@Nullable Player pilot) {
        if (pilot instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new HelmReadoutPayload(readout));
        }
    }

    /** {@code velocityShipFrame} and {@code spinShipFrame} are the ship's motion turned into its own frame (zero if unknown). */
    private Readout look(ServerLevel server, ServerSubLevel ship, List<ThrusterLike> thrusters, Vector3d velocityShipFrame, Vector3d spinShipFrame) {
        List<ThrustAllocator.Thruster> model = new ArrayList<>();
        double fuel = 0, fuelCapacity = 0;
        int energy = 0, energyCapacity = 0;
        for (ThrusterLike t : thrusters) {
            model.add(new ThrustAllocator.Thruster(new Vector3d(t.getBlockPos().getX() + 0.5, t.getBlockPos().getY() + 0.5, t.getBlockPos().getZ() + 0.5),
                    Ships.unit(t.pushDirection()), t.maxForce()));
            if (t instanceof ChemicalThrusterBlockEntity chemical) {
                fuel += chemical.tank().getFluidAmount();
                fuelCapacity += chemical.tank().getCapacity();
            } else if (t instanceof ElectricThrusterBlockEntity electric) {
                energy += electric.storedEnergy();
                energyCapacity += electric.energy().getMaxEnergyStored();
            }
        }
        ThrustAllocator.Frame frame = frame(ship);
        double[] velocity = {velocityShipFrame.dot(frame.forward()), velocityShipFrame.dot(frame.right()), velocityShipFrame.dot(frame.up())};
        // As the pilot names them: roll right, nose up, yaw right. Spin about +up is a turn left, so that one is the other way round.
        double[] spin = {Math.toDegrees(spinShipFrame.dot(frame.forward())), Math.toDegrees(spinShipFrame.dot(frame.right())),
                -Math.toDegrees(spinShipFrame.dot(frame.up()))};
        return new Readout(true, thrusters.size(), ThrustAllocator.translationCoverage(model, frame),
                ThrustAllocator.rotationCoverage(model, frame), fuel, fuelCapacity, energy, energyCapacity, velocity, spin,
                Attitude.of(Ships.orientation(ship), frame.forward(), frame.right(), frame.up()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(SIT_ANIMATION, SIT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
