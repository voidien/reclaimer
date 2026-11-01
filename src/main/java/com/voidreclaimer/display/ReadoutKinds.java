package com.voidreclaimer.display;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.holo.HoloReadout;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.CoreCharging;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.machine.CloudSiphonBlockEntity;
import com.voidreclaimer.machine.ElectrolyzerBlockEntity;
import com.voidreclaimer.machine.SpinnerBlockEntity;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.ship.ChemicalThrusterBlockEntity;
import com.voidreclaimer.ship.ElectricThrusterBlockEntity;
import com.voidreclaimer.ship.HelmBlockEntity;
import com.voidreclaimer.ship.ThrusterLike;
import com.voidreclaimer.terraform.SpireBlockEntity;
import java.util.Comparator;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The built-in readout kinds: each one's sampler (reads the world on the server) with its {@link Readouts} formatter. A kind
 * about a machine reads the nearest one within {@code holo.sourceRange} blocks of the projector; with none in reach it shows
 * nothing. A projector on a ship finds what is aboard, because both stand in the ship's plot.
 */
public final class ReadoutKinds {
    public static final DeferredRegister<HoloReadout<?>> KINDS = DeferredRegister.create(VoidReclaimerRegistries.HOLO_READOUT, VoidReclaimer.MOD_ID);

    /** How far off a {@code player} readout sees a body, in blocks. */
    private static final double BODY_RANGE = 16;

    public static final Supplier<HoloReadout<Readouts.Clock>> CLOCK = KINDS.register("clock",
            () -> new HoloReadout<>((level, pos) -> new Readouts.Clock(level.getGameTime()), Readouts::clock));

    public static final Supplier<HoloReadout<Readouts.Helm>> HELM = KINDS.register("helm",
            () -> new HoloReadout<>((level, pos) -> {
                HelmBlockEntity helm = nearest(level, pos, HelmBlockEntity.class);
                if (helm == null) {
                    return null;
                }
                HelmBlockEntity.Readout r = helm.readout();
                return new Readouts.Helm(r.onShip(), r.thrusters(), r.speed(), r.attitude().heading(), r.attitude().pitch(), r.attitude().roll(),
                        r.fuel(), r.fuelCapacity(), r.energy(), r.energyCapacity());
            }, Readouts::helm));

    public static final Supplier<HoloReadout<Readouts.Thruster>> THRUSTER = KINDS.register("thruster",
            () -> new HoloReadout<>((level, pos) -> {
                BlockEntity entity = nearest(level, pos, BlockEntity.class, ThrusterLike.class::isInstance);
                if (entity instanceof ChemicalThrusterBlockEntity chemical) {
                    return new Readouts.Thruster(chemical.throttle(), chemical.force(),
                            "Fuel " + chemical.tank().getFluidAmount() + " / " + chemical.tank().getCapacity() + " mB");
                }
                if (entity instanceof ElectricThrusterBlockEntity electric) {
                    return new Readouts.Thruster(electric.throttle(), electric.force(),
                            "Energy " + electric.storedEnergy() + " / " + electric.energy().getMaxEnergyStored() + " FE");
                }
                return null;
            }, Readouts::thruster));

    public static final Supplier<HoloReadout<Readouts.Siphon>> SIPHON = KINDS.register("siphon",
            () -> new HoloReadout<>((level, pos) -> {
                CloudSiphonBlockEntity siphon = nearest(level, pos, CloudSiphonBlockEntity.class);
                return siphon == null ? null : new Readouts.Siphon(siphon.density(), siphon.power(), siphon.getSpeed(), count(siphon.inventory()));
            }, Readouts::siphon));

    public static final Supplier<HoloReadout<Readouts.Electrolyzer>> ELECTROLYZER = KINDS.register("electrolyzer",
            () -> new HoloReadout<>((level, pos) -> {
                ElectrolyzerBlockEntity machine = nearest(level, pos, ElectrolyzerBlockEntity.class);
                return machine == null ? null : new Readouts.Electrolyzer(machine.progress(), machine.getSpeed(), machine.waterTank().getFluidAmount(),
                        machine.oxygenTank().getFluidAmount(), machine.hydrogenTank().getFluidAmount(), machine.waterTank().getCapacity());
            }, Readouts::electrolyzer));

    public static final Supplier<HoloReadout<Readouts.Spinner>> SPINNER = KINDS.register("spinner",
            () -> new HoloReadout<>((level, pos) -> {
                SpinnerBlockEntity machine = nearest(level, pos, SpinnerBlockEntity.class);
                return machine == null ? null : new Readouts.Spinner(machine.progress(), machine.getSpeed(),
                        machine.inventory().getStackInSlot(0).getCount(), machine.inventory().getStackInSlot(1).getCount());
            }, Readouts::spinner));

    public static final Supplier<HoloReadout<Readouts.Launch>> LAUNCH = KINDS.register("launch_complex",
            () -> new HoloReadout<>((level, pos) -> {
                LaunchControllerBlockEntity controller = nearest(level, pos, LaunchControllerBlockEntity.class);
                return controller == null ? null : new Readouts.Launch(controller.assembled(), controller.progress(), controller.launches(),
                        controller.getSpeed(), controller.firstPayloadSlot() >= 0);
            }, Readouts::launch));

    public static final Supplier<HoloReadout<Readouts.Dyson>> DYSON = KINDS.register("dyson",
            () -> new HoloReadout<>((level, pos) -> {
                DysonSphereData sphere = DysonSphereData.of(level);
                return new Readouts.Dyson(sphere.satellites(), sphere.trusses(), sphere.shellTotal(), sphere.tier(), sphere.shellProgress());
            }, Readouts::dyson));

    public static final Supplier<HoloReadout<Readouts.Spire>> SPIRE = KINDS.register("spire",
            () -> new HoloReadout<>((level, pos) -> {
                SpireBlockEntity spire = nearest(level, pos, SpireBlockEntity.class);
                return spire == null ? null : new Readouts.Spire(spire.running(), spire.progress(), spire.pulses(), spire.radius());
            }, Readouts::spire));

    public static final Supplier<HoloReadout<Readouts.Core>> BACKUP_CORE = KINDS.register("backup_core",
            () -> new HoloReadout<>((level, pos) -> {
                BackupCoreBlockEntity core = nearest(level, pos, BackupCoreBlockEntity.class);
                return core == null ? null : new Readouts.Core(core.name(), core.charges(), CoreCharging.MAX_CHARGES, core.sunFraction());
            }, Readouts::core));

    public static final Supplier<HoloReadout<Readouts.Body>> PLAYER = KINDS.register("player",
            () -> new HoloReadout<>((level, pos) -> {
                Vec3 center = Vec3.atCenterOf(pos);
                Player player = level.players().stream()
                        .filter(candidate -> !candidate.isSpectator() && candidate.distanceToSqr(center) <= BODY_RANGE * BODY_RANGE)
                        .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))
                        .orElse(null);
                if (player == null) {
                    return null;
                }
                EnergyData energy = player.getData(ModAttachments.ENERGY);
                return new Readouts.Body(player.getGameProfile().getName(), player.getHealth(), player.getMaxHealth(),
                        energy.energy(), energy.capacity(), energy.lowPower());
            }, Readouts::body));

    public static final Supplier<HoloReadout<Readouts.Cloud>> CLOUD = KINDS.register("cloud",
            () -> new HoloReadout<>((level, pos) -> {
                DustField.Sample sample = DustField.of(level).sample(Vec3.atCenterOf(pos));
                return sample.inCloud() ? new Readouts.Cloud(sample.cloud().kind().getPath(), sample.density()) : new Readouts.Cloud("", 0);
            }, Readouts::cloud));

    public static final Supplier<HoloReadout<Readouts.Device>> DEVICE = KINDS.register("device",
            () -> new HoloReadout<>(ReadoutKinds::device, Readouts::device));

    private ReadoutKinds() {}

    /** The nearest block entity of a type in reach of a projector, or null. */
    @Nullable
    static <T> T nearest(Level level, BlockPos pos, Class<T> type) {
        return nearest(level, pos, type, entity -> true);
    }

    @Nullable
    static <T> T nearest(Level level, BlockPos pos, Class<T> type, java.util.function.Predicate<? super T> test) {
        int range = Config.HOLO_SOURCE_RANGE.get();
        long rangeSqr = (long) range * range;
        T best = null;
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;
        // Each loaded chunk's own list, not a cube of lookups: a projector does this twice a second.
        for (int cx = (pos.getX() - range) >> 4; cx <= (pos.getX() + range) >> 4; cx++) {
            for (int cz = (pos.getZ() - range) >> 4; cz <= (pos.getZ() + range) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    double distance = entity.getBlockPos().distSqr(pos);
                    if (distance > rangeSqr || !type.isInstance(entity)) {
                        continue;
                    }
                    T candidate = type.cast(entity);
                    if (test.test(candidate) && (distance < bestDistance
                            || distance == bestDistance && entity.getBlockPos().compareTo(bestPos) < 0)) {
                        best = candidate;
                        bestPos = entity.getBlockPos();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    /** The first block next to the projector, the one below it first, that stores energy or fluid. Works for any mod's block. */
    @Nullable
    private static Readouts.Device device(Level level, BlockPos pos) {
        for (Direction side : SIDES) {
            BlockPos at = pos.relative(side);
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, at, side.getOpposite());
            IFluidHandler fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, at, side.getOpposite());
            boolean tank = fluid != null && fluid.getTanks() > 0;
            if (energy == null && !tank) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            return new Readouts.Device(state.getBlock().getName().getString(),
                    energy == null ? 0 : energy.getEnergyStored(),
                    energy == null ? -1 : energy.getMaxEnergyStored(),
                    tank ? fluidName(fluid) : "",
                    tank ? fluid.getFluidInTank(0).getAmount() : 0,
                    tank ? fluid.getTankCapacity(0) : -1);
        }
        return null;
    }

    private static String fluidName(IFluidHandler handler) {
        return handler.getFluidInTank(0).isEmpty() ? "Empty" : handler.getFluidInTank(0).getHoverName().getString();
    }

    private static final Direction[] SIDES = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};

    private static int count(IItemHandler items) {
        int total = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            total += stack.getCount();
        }
        return total;
    }
}
