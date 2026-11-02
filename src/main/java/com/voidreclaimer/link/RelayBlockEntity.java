package com.voidreclaimer.link;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.ship.ShipTickets;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A Relay. It runs on Forge Energy from any energy mod, through the standard capability, and goes dark without it. Its id lasts as long
 * as the block does, wherever a ship takes it, because a position on a ship means nothing. It writes its own row in the {@link RelayList}.
 * While it has power it holds its ship loaded, so the ship can go on drifting and the relay on spending energy with nobody near.
 */
public class RelayBlockEntity extends BlockEntity {
    private static final String ID = "relay_id";
    private static final String ENERGY = "energy";
    private static final int NOTE_TICKS = 20;

    private final IEnergyStorage energy = new Energy();
    private UUID id = UUID.randomUUID();
    private int stored;

    public RelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELAY.get(), pos, state);
    }

    public UUID id() {
        return id;
    }

    public boolean isLit() {
        return getBlockState().getValue(RelayBlock.LIT);
    }

    /** Forge Energy input; nothing can be taken out. */
    public IEnergyStorage energy() {
        return energy;
    }

    public int storedEnergy() {
        return stored;
    }

    public int energyCapacity() {
        return Config.LINK_RELAY_FE_BUFFER.get();
    }

    public int energyPerTick() {
        return Config.LINK_RELAY_FE_PER_TICK.get();
    }

    /** How many relays this level knows, this one included once it has ticked. */
    public int relaysInLevel() {
        return level instanceof ServerLevel server ? Relays.of(server).relays().size() : 0;
    }

    /** How far it reaches now, in blocks, while it has power. */
    public double range() {
        return level == null || level.getServer() == null ? 0.0 : Signal.relayRange(level.getServer());
    }

    /** What a right-click says. */
    public Component readout() {
        boolean lit = isLit();
        return Component.translatable(lit ? "message.void_reclaimer.relay.online" : "message.void_reclaimer.relay.dark",
                stored, energyCapacity(), energyPerTick(), relaysInLevel(), (long) range()).withStyle(lit ? ChatFormatting.AQUA : ChatFormatting.RED);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, RelayBlockEntity relay) {
        int perTick = relay.energyPerTick();
        boolean lit = state.getValue(RelayBlock.LIT);
        boolean runs = RelayPower.runs(relay.stored, lit, perTick, relay.energyCapacity());
        if (runs && relay.stored > 0) {
            relay.stored = RelayPower.after(relay.stored, perTick);
            relay.setChanged();
        }
        if (runs != lit) {
            level.setBlock(pos, state.setValue(RelayBlock.LIT, runs), Block.UPDATE_ALL);
        }
        if (runs != lit || level.getGameTime() % NOTE_TICKS == 0) {
            relay.note((ServerLevel) level, runs);
        }
    }

    /** Writes this relay's row. A removed ship's plot lingers for a while, and a block in it must not put back the row that left with the ship. */
    private void note(ServerLevel level, boolean lit) {
        ServerSubLevel ship = Ships.shipAt(level, worldPosition);
        if (ship == null && Ships.inPlots(level, worldPosition)) {
            return;
        }
        Vec3 middle = Vec3.atCenterOf(worldPosition);
        Vec3 place = ship == null ? middle : ship.logicalPose().transformPosition(middle);
        Relays.note(level, new RelayList.Entry(id, worldPosition, place, lit, Optional.ofNullable(ship).map(ServerSubLevel::getUniqueId)));
        if (ship != null) {
            if (lit) {
                ShipTickets.hold(level, ship, id);
            } else {
                ShipTickets.release(level, ship, id);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID(ID, id);
        tag.putInt(ENERGY, stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(ID)) {
            id = tag.getUUID(ID);
        }
        stored = Math.max(0, tag.getInt(ENERGY));
    }

    /** Takes energy up to the configured buffer; gives none back. */
    private final class Energy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(maxReceive, energyCapacity() - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
