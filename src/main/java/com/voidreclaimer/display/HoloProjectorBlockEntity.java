package com.voidreclaimer.display;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.holo.HoloReadout;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModRegistries;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

/**
 * A placed Holo Projector: its {@link HoloDisplay}, its owner, and on the server the refresh of a readout. Every change goes
 * through {@link #apply}, which saves and resends only when something really changed, so a steady readout costs no packets.
 */
public final class HoloProjectorBlockEntity extends BlockEntity {
    /** How a network map draws: the projector, a lit relay, a dark one, and what the lit ones reach. */
    public static final HoloNetworkMap.Colours NETWORK_COLOURS = new HoloNetworkMap.Colours(
            MapColor.SNOW.getPackedId(MapColor.Brightness.HIGH) & 0xFF, MapColor.DIAMOND.getPackedId(MapColor.Brightness.HIGH) & 0xFF,
            MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, MapColor.COLOR_BLUE.getPackedId(MapColor.Brightness.LOW) & 0xFF);

    private HoloDisplay display = HoloDisplay.EMPTY;
    @Nullable
    private UUID owner;
    /** Packets this projector asked for (server), for tests. */
    private int syncs;
    /** Bumped each time the client gets new content, so render caches know when to rebuild. */
    private int revision;
    /** The newest map picture. Sent to clients, not saved: the blocks are there to draw it again. */
    private byte[] mapImage = new byte[0];
    private int mapRadius;

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

    public void setShip() {
        apply(display.withShip());
    }

    /** Shows a map of this level's relays and draws its first picture at once. */
    public void setNetworkMap() {
        apply(display.withNetworkMap());
        refreshMap();
    }

    /** Shows a map of the blocks around the projector and draws its first picture at once. */
    public void setMap() {
        apply(display.withMap());
        refreshMap();
    }

    /** The newest map picture: empty before the first. See {@link HoloMapImage}. */
    public byte[] mapImage() {
        return mapImage;
    }

    /** Blocks from the middle of the map to its edge. */
    public int mapRadius() {
        return mapRadius;
    }

    /** Takes a new picture of the blocks around; sends it only if it differs. */
    public void refreshMap() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean network = display.showsNetwork();
        List<HoloNetworkMap.Node> relays = network ? relays() : List.of();
        Vec3 here = network ? worldSpot() : Vec3.ZERO;
        int radius = network ? HoloNetworkMap.radius(relays, here.x, here.z) : Config.HOLO_MAP_RADIUS.get();
        byte[] image = network ? HoloNetworkMap.draw(relays, here.x, here.z, radius, com.voidreclaimer.link.Signal.relayRange(level.getServer()), NETWORK_COLOURS)
                : HoloMapImage.compose((x, y) -> mapPixel(level, worldPosition.getX() + HoloMapImage.offset(x, radius), worldPosition.getZ() + HoloMapImage.offset(y, radius)));
        if (radius != mapRadius || !Arrays.equals(image, mapImage)) {
            mapImage = image;
            mapRadius = radius;
            syncs++;
            ((ServerLevel) level).sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private List<HoloNetworkMap.Node> relays() {
        return com.voidreclaimer.link.Relays.of((ServerLevel) level).relays().stream()
                .map(relay -> new HoloNetworkMap.Node(relay.position().x, relay.position().z, relay.lit())).toList();
    }

    /** Where the projector is in the world: on a ship that is not where its block is. */
    private Vec3 worldSpot() {
        Vec3 middle = Vec3.atCenterOf(worldPosition);
        var ship = com.voidreclaimer.ship.Ships.shipAt((ServerLevel) level, worldPosition);
        return ship == null ? middle : ship.logicalPose().transformPosition(middle);
    }

    /** The top block of a column as a packed map colour; 0 for nothing there or a chunk that is not loaded. */
    private static int mapPixel(Level level, int x, int z) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return 0;
        }
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        BlockPos pos = new BlockPos(x, top, z);
        BlockState state = chunk.getBlockState(pos);
        return state.isAir() ? 0 : state.getMapColor(level, pos).getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
    }

    /** Shows a globe and fills in its progress at once; false for a name that is not one. */
    public boolean setGlobe(String globe) {
        if (!HoloGlobes.NAMES.contains(globe)) {
            return false;
        }
        apply(display.withGlobe(globe));
        refresh();
        return true;
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

    /** Composes a readout's lines or a globe's progress now; a projector showing anything else keeps what it has. */
    public void refresh() {
        if (display.mode() == HoloDisplay.Mode.READOUT) {
            apply(display.withReadoutLines(compose(display)));
        } else if (display.mode() == HoloDisplay.Mode.GLOBE && level != null && level.getServer() != null) {
            apply(display.withProgress(HoloGlobes.sample(level.getServer())));
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
        long tick = level.getGameTime() + pos.asLong();
        if (tick % Config.HOLO_REFRESH_TICKS.get() == 0) {
            projector.refresh();
        }
        if (projector.display.mode() == HoloDisplay.Mode.MAP && tick % Config.HOLO_MAP_REFRESH_TICKS.get() == 0) {
            projector.refreshMap();
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
        if (tag.contains("map")) {
            mapImage = tag.getByteArray("map");
            mapRadius = tag.getInt("mapRadius");
        }
        revision++;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        if (display.mode() == HoloDisplay.Mode.MAP) {
            tag.putByteArray("map", mapImage);
            tag.putInt("mapRadius", mapRadius);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
