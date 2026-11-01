package com.voidreclaimer.display;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.holo.HoloReadout;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed Holo Projector: its {@link HoloDisplay}, its owner, and on the server the refresh of a readout. Every change goes
 * through {@link #apply}, which saves and resends only when something really changed, so a steady readout costs no packets.
 */
public final class HoloProjectorBlockEntity extends BlockEntity {
    private HoloDisplay display = HoloDisplay.EMPTY;
    @Nullable
    private UUID owner;
    /** Packets this projector asked for (server), for tests. */
    private int syncs;
    /** Bumped each time the client gets new content, so render caches know when to rebuild. */
    private int revision;

    public HoloProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOLO_PROJECTOR.get(), pos, state);
    }

    public HoloDisplay display() {
        return display;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int syncs() {
        return syncs;
    }

    public int revision() {
        return revision;
    }

    /** Writes one line (from 0) as text; false for a line beyond the cap. */
    public boolean setLine(int index, String text, int color) {
        if (!HoloDisplay.fits(index)) {
            return false;
        }
        apply(display.withLine(index, text, color));
        return true;
    }

    public void setLines(List<String> texts) {
        List<String> lines = new ArrayList<>();
        for (String text : texts) {
            lines.addAll(HoloText.wrap(text));
        }
        apply(display.withTexts(lines));
    }

    public void clearLines() {
        apply(display.withTexts(List.of()));
    }

    public void setColor(int color) {
        apply(display.withColor(color));
    }

    /** Shows a readout from now on and composes it at once. An unknown id is kept and shows nothing. */
    public void setSource(ResourceLocation id) {
        HoloDisplay next = display.withSource(id);
        apply(next.withReadoutLines(compose(next)));
    }

    /** Moves on to the next readout kind, as a sneak-use does. */
    public ResourceLocation cycleSource() {
        setSource(Readouts.next(List.copyOf(ModRegistries.HOLO_READOUTS.keySet()), display.source()));
        return display.source();
    }

    public void setModel(ResourceLocation id) {
        apply(display.withModel(id));
    }

    public void setBody() {
        apply(display.withBody());
    }

    public void setScale(float scale) {
        apply(display.withScale(scale));
    }

    public void setHeight(float height) {
        apply(display.withHeight(height));
    }

    public void setBillboard(boolean billboard) {
        apply(display.withBillboard(billboard));
    }

    /** Composes the readout's lines now; a projector showing text keeps them. */
    public void refresh() {
        if (display.mode() == HoloDisplay.Mode.READOUT) {
            apply(display.withReadoutLines(compose(display)));
        }
    }

    private List<String> compose(HoloDisplay shown) {
        HoloReadout<?> kind = ModRegistries.HOLO_READOUTS.get(shown.source());
        return level == null ? List.of() : Readouts.lines(kind, level, worldPosition);
    }

    private void apply(HoloDisplay next) {
        if (next.equals(display)) {
            return;
        }
        display = next;
        setChanged();
        if (level instanceof ServerLevel server) {
            syncs++;
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, HoloProjectorBlockEntity projector) {
        if ((level.getGameTime() + pos.asLong()) % Config.HOLO_REFRESH_TICKS.get() == 0) {
            projector.refresh();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        HoloDisplay.CODEC.encodeStart(NbtOps.INSTANCE, display).resultOrPartial(LogUtils.getLogger()::warn).ifPresent(saved -> tag.put("display", saved));
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        display = tag.contains("display")
                ? HoloDisplay.CODEC.parse(NbtOps.INSTANCE, tag.get("display")).resultOrPartial(LogUtils.getLogger()::warn).orElse(HoloDisplay.EMPTY)
                : HoloDisplay.EMPTY;
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        revision++;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
