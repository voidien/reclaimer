package com.voidreclaimer.station;

import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Holds a visiting ship against the station. A ship whose block sits in the cell in front of the clamp's face is docked
 * when the clamp is powered (or told to): its blocks move into the station's plot, so the two become one Sable body and
 * the station's thrusters carry them both. Releasing (power off, or told to) assembles the recorded blocks back into
 * their own ship, which inherits the station's pose and velocity. The visitor must be level and within
 * {@value #ALIGN_DEGREES} degrees of a quarter turn relative to the station, since blocks only move by quarter turns.
 */
public class DockingClampBlockEntity extends BlockEntity {
    /** Ticks between looks for a visitor while powered and empty. */
    static final int LOOK_TICKS = 5;
    /** How far from a quarter turn, and from level, a visitor may be. */
    static final double ALIGN_DEGREES = 10.0;

    /** What the clamp last did or found, for the peripheral. */
    public enum Status { IDLE, NOT_ON_SHIP, NO_VISITOR, MISALIGNED, BLOCKED, DOCKED, RELEASED, EMPTY, FAILED }

    private final List<BlockPos> docked = new ArrayList<>();
    private String dockedName = "";
    private CompoundTag dockedData = new CompoundTag();
    private boolean dockedByPower;
    private Status status = Status.IDLE;

    public DockingClampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOCKING_CLAMP.get(), pos, state);
    }

    public boolean isDocked() {
        return !docked.isEmpty();
    }

    public String dockedName() {
        return dockedName;
    }

    public Status status() {
        return status;
    }

    /** The cell in front of the clamp's face, in the clamp's own (plot) coordinates. */
    public BlockPos front() {
        return worldPosition.relative(getBlockState().getValue(DockingClampBlock.FACING));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DockingClampBlockEntity clamp) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % LOOK_TICKS != 0) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (clamp.isDocked()) {
            if (!powered && clamp.dockedByPower) {
                clamp.release();
            }
        } else if (powered) {
            if (clamp.dock() == Status.DOCKED) {
                clamp.dockedByPower = true;
            }
        }
    }

    /** Docks the ship whose block sits in front of the clamp, if there is one and it is aligned. */
    public Status dock() {
        if (!(level instanceof ServerLevel server)) {
            return set(Status.FAILED);
        }
        if (isDocked()) {
            return set(Status.DOCKED);
        }
        ServerSubLevel station = Ships.shipAt(server, worldPosition);
        if (station == null) {
            return set(Status.NOT_ON_SHIP);
        }
        BlockPos front = front();
        if (!server.getBlockState(front).isAir()) {
            return set(Status.BLOCKED);
        }
        Vec3 point = station.logicalPose().transformPosition(Vec3.atCenterOf(front));
        ServerSubLevel visitor = null;
        BlockPos touching = null;
        for (ServerSubLevel ship : SubLevelContainer.getContainer(server).getAllSubLevels()) {
            if (ship == station || ship.isRemoved()) {
                continue;
            }
            Vec3 local = ship.logicalPose().transformPositionInverse(point);
            BlockPos candidate = BlockPos.containing(local);
            if (!server.getBlockState(candidate).isAir()) {
                visitor = ship;
                touching = candidate;
                break;
            }
        }
        if (visitor == null) {
            return set(Status.NO_VISITOR);
        }
        Quaterniond relative = new Quaterniond(station.logicalPose().orientation()).conjugate().mul(new Quaterniond(visitor.logicalPose().orientation()));
        if (!StationMath.isLevel(relative, ALIGN_DEGREES) || StationMath.yawResidualDegrees(relative) > ALIGN_DEGREES) {
            return set(Status.MISALIGNED);
        }
        int turns = StationMath.quarterTurns(relative);
        Rotation rotation = StationMath.blockRotation(turns);
        List<BlockPos> blocks = StationAssembly.blocksOf(server, visitor);
        if (blocks.isEmpty()) {
            return set(Status.NO_VISITOR);
        }
        SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(touching, front, turns, rotation, server);
        List<BlockPos> landing = new ArrayList<>(blocks.size());
        for (BlockPos block : blocks) {
            BlockPos where = transform.apply(block);
            if (!server.getBlockState(where).isAir()) {
                return set(Status.BLOCKED);
            }
            landing.add(where);
        }
        dockedName = visitor.getName() == null ? "" : visitor.getName();
        dockedData = visitor.getUserDataTag() == null ? new CompoundTag() : visitor.getUserDataTag().copy();
        SimAssemblyHelper.disassembleSubLevel(server, visitor, touching, front, rotation, true);
        docked.clear();
        docked.addAll(landing);
        dockedByPower = false;
        setChanged();
        return set(Status.DOCKED);
    }

    /** Lets the docked ship go: its blocks become their own ship again, moving as the station moves. */
    public Status release() {
        if (!(level instanceof ServerLevel server)) {
            return set(Status.FAILED);
        }
        if (!isDocked()) {
            return set(Status.EMPTY);
        }
        List<BlockPos> blocks = new ArrayList<>();
        BlockPos min = null, max = null;
        for (BlockPos pos : docked) {
            if (server.getBlockState(pos).isAir()) {
                continue;
            }
            blocks.add(pos);
            min = min == null ? pos : new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = max == null ? pos : new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
        }
        docked.clear();
        dockedByPower = false;
        setChanged();
        if (blocks.isEmpty()) {
            return set(Status.EMPTY);
        }
        ServerSubLevel ship = SubLevelAssemblyHelper.assembleBlocks(server, blocks.get(0), blocks, new BoundingBox3i(min, max));
        if (ship == null) {
            return set(Status.FAILED);
        }
        if (!dockedName.isBlank()) {
            ship.setName(dockedName);
        }
        if (!dockedData.isEmpty()) {
            ship.setUserDataTag(dockedData.copy());
        }
        return set(Status.RELEASED);
    }

    private Status set(Status status) {
        this.status = status;
        return status;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        docked.clear();
        for (long packed : tag.getLongArray("docked")) {
            docked.add(BlockPos.of(packed));
        }
        dockedName = tag.getString("docked_name");
        dockedData = tag.getCompound("docked_data");
        dockedByPower = tag.getBoolean("docked_by_power");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] packed = new long[docked.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = docked.get(i).asLong();
        }
        tag.put("docked", new LongArrayTag(packed));
        tag.putString("docked_name", dockedName);
        tag.put("docked_data", dockedData);
        tag.putBoolean("docked_by_power", dockedByPower);
    }

    @Nullable
    static Direction facingOf(BlockState state) {
        return state.hasProperty(DockingClampBlock.FACING) ? state.getValue(DockingClampBlock.FACING) : null;
    }
}
