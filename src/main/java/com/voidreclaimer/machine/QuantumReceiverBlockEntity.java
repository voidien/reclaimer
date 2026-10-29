package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Sub-Light Quantum Receiver: a rotation source fed by the Dyson Sphere. Its output is proportional to the Shell Truss
 * Modules launched, from nothing up to the configured maximum (262,144 SU at 128 RPM) once the shell is complete. It works in
 * any dimension on the server, so a Receiver on a ship or on Ruined Earth draws on the same sphere.
 */
public class QuantumReceiverBlockEntity extends GeneratingKineticBlockEntity {
    /** Ticks between looks at the sphere's progress. */
    private static final int REFRESH_TICKS = 20;

    /** Receivers loaded on the client, so the deep space sky can draw their power beams. Pruned by the sky when a block is gone. */
    public static final java.util.Set<BlockPos> CLIENT_RECEIVERS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private long seenTrusses = -1;
    private long seenTotal = -1;

    public QuantumReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public QuantumReceiverBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.QUANTUM_RECEIVER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_RECEIVERS.add(worldPosition);
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (level != null && level.isClientSide) {
            CLIENT_RECEIVERS.remove(worldPosition);
        }
    }

    private DysonSphereData sphere() {
        if (level instanceof ServerLevel server) {
            return DysonSphereData.of(server.getServer());
        }
        return DysonSphereData.createDefault();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || level.getGameTime() % REFRESH_TICKS != 0) {
            return;
        }
        DysonSphereData data = sphere();
        if (data.trusses() != seenTrusses || data.shellTotal() != seenTotal) {
            seenTrusses = data.trusses();
            seenTotal = data.shellTotal();
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return Config.isEnabled("quantumReceiver") && sphere().trusses() > 0 ? Config.RECEIVER_RPM.get() : 0f;
    }

    /** Stress per RPM, so that the whole network gets {@code receiverMaxStress} SU at the full shell. */
    @Override
    public float calculateAddedStressCapacity() {
        DysonSphereData data = sphere();
        float perRpm = MassDriverMath.receiverCapacityPerRpm(data.trusses(), data.shellTotal(),
                Config.RECEIVER_MAX_STRESS.get(), Config.RECEIVER_RPM.get());
        this.lastCapacityProvided = perRpm;
        return perRpm;
    }
}
