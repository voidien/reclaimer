package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.event.MassDriverLaunchEvent;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.network.LaunchStreakPayload;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Kinetic Drive Base: the controller of the Mass Driver. Spun by a shaft, it winds up while the Payload Chamber above
 * it holds a payload, then fires it through the Barrel Nozzle into the Dyson Sphere. The faster the shaft, the shorter the
 * wind-up ({@link MassDriverMath#launchTicks}). Below the minimum speed it does nothing.
 */
public class MassDriverBaseBlockEntity extends KineticBlockEntity {
    private float progress;
    private int launches;

    public MassDriverBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MassDriverBaseBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MASS_DRIVER_BASE.get(), pos, state);
    }

    /** Wind-up of the next launch, 0 to 1. */
    public float progress() {
        return progress;
    }

    /** Payloads this driver has fired. */
    public int launches() {
        return launches;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || !Config.isEnabled("massDriver") || !MassDriver.isAssembled(server, worldPosition)) {
            return;
        }
        if (!(server.getBlockEntity(MassDriver.chamberPos(worldPosition)) instanceof PayloadChamberBlockEntity chamber)) {
            return;
        }
        int slot = chamber.firstPayloadSlot();
        float perTick = MassDriverMath.progressPerTick(getSpeed(), Config.MASS_DRIVER_MIN_RPM.get(), Config.MASS_DRIVER_FAST_RPM.get(),
                Config.MASS_DRIVER_SLOW_TICKS.get(), Config.MASS_DRIVER_FAST_TICKS.get());
        if (slot < 0 || perTick <= 0f) {
            return;
        }
        progress += perTick;
        if (progress >= 1f) {
            progress = 0f;
            fire(server, chamber, slot);
        }
    }

    private void fire(ServerLevel server, PayloadChamberBlockEntity chamber, int slot) {
        ItemStack payload = chamber.inventory().extractItem(slot, 1, false);
        if (payload.isEmpty()) {
            return;
        }
        boolean satellite = MassDriver.isSatellite(payload);
        boolean truss = MassDriver.isTruss(payload);
        MassDriverLaunchEvent event = new MassDriverLaunchEvent(server, worldPosition, payload.copy(), satellite ? 1 : 0, truss ? 1 : 0);
        NeoForge.EVENT_BUS.post(event);
        launches++;
        setChanged();
        BlockPos nozzle = MassDriver.nozzlePos(worldPosition);
        server.playSound(null, nozzle, ModSounds.MASS_DRIVER_LAUNCH.get(), SoundSource.BLOCKS, 2f, 1f);
        server.sendParticles(ParticleTypes.CLOUD, nozzle.getX() + 0.5, nozzle.getY() + 1.1, nozzle.getZ() + 0.5, 24, 0.2, 0.1, 0.2, 0.08);
        if (event.isCanceled()) {
            return;
        }
        if (event.getSatellites() > 0) {
            Dyson.launchSatellites(server.getServer(), event.getSatellites());
        }
        if (event.getTrusses() > 0) {
            Dyson.launchTrusses(server.getServer(), event.getTrusses());
        }
        PacketDistributor.sendToAllPlayers(new LaunchStreakPayload(worldPosition, truss && !satellite));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("progress", progress);
        tag.putInt("launches", launches);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getFloat("progress");
        launches = tag.getInt("launches");
    }
}
