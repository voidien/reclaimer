package com.voidreclaimer.core;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A placed Backup Core: its owner, name and stored respawn charges. Sunlight slowly adds charges. */
public final class BackupCoreBlockEntity extends BlockEntity {
    public static final int MAX_NAME_LENGTH = 24;
    private static final int SUN_UPDATE_TICKS = 20;

    @Nullable
    private UUID owner;
    private String name = "";
    private int charges;
    private int sunProgress;

    public BackupCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BACKUP_CORE.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public boolean isOwnedBy(UUID player) {
        return player.equals(owner);
    }

    public String name() {
        return name;
    }

    public int charges() {
        return charges;
    }

    /** Fraction 0..1 toward the next sunlight charge. */
    public float sunFraction() {
        return charges >= CoreCharging.MAX_CHARGES ? 0f : CoreCharging.fraction(sunProgress, Config.CORE_SUN_TICKS.get());
    }

    void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.name = name;
        changed();
    }

    public void setName(String name) {
        this.name = cleanName(name);
        changed();
    }

    public void setCharges(int charges) {
        this.charges = Math.max(0, Math.min(CoreCharging.MAX_CHARGES, charges));
        changed();
    }

    public static String cleanName(String name) {
        String clean = StringUtil.filterText(name).strip();
        return clean.length() > MAX_NAME_LENGTH ? clean.substring(0, MAX_NAME_LENGTH) : clean;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, BackupCoreBlockEntity core) {
        if (level.getGameTime() % SUN_UPDATE_TICKS != 0 || core.charges >= CoreCharging.MAX_CHARGES) {
            return;
        }
        int sky = Math.max(0, level.getBrightness(LightLayer.SKY, pos.above()) - level.getSkyDarken());
        CoreCharging.State next = CoreCharging.sunTick(core.charges, core.sunProgress, SUN_UPDATE_TICKS, sky, Config.CORE_SUN_TICKS.get());
        core.sunProgress = next.progress();
        if (next.charges() != core.charges) {
            core.charges = next.charges();
            core.changed();
        } else {
            // Keep an open core screen's sunlight bar current; owners' core lists only track charges.
            core.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            BackupCores.onCoreChanged(serverLevel, this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putString("name", name);
        tag.putInt("charges", charges);
        tag.putInt("sun_progress", sunProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        name = tag.getString("name");
        charges = Math.max(0, Math.min(CoreCharging.MAX_CHARGES, tag.getInt("charges")));
        sunProgress = Math.max(0, tag.getInt("sun_progress"));
    }

    /** The client screen shows name, charges and sunlight progress. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
