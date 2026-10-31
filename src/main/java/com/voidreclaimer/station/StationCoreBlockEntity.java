package com.voidreclaimer.station;

import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The heart of a station: a name (the item's custom name when placed, or set through CC), a shared chest of
 * {@value #SLOTS} slots, and the owner who placed it. Built into a Sable hull it makes that hull a station: assembled
 * and disassembled from here (see {@link StationAssembly}), listed and marked on the HUD ({@link Stations}), a faster
 * Replicator nearby, and a respawn point that moves with it when a Backup Core is aboard.
 */
public class StationCoreBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOTS = 27;
    public static final String DEFAULT_NAME_KEY = "container.void_reclaimer.station";

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private UUID id = UUID.randomUUID();
    @Nullable
    private UUID owner;
    /** The station's own name; empty means the default. Set from the item's custom name when placed, or by CC. */
    private String stationName = "";

    public StationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION_CORE.get(), pos, state);
    }

    /** A stable id for the station list and the HUD, kept across assembly and disassembly. */
    public UUID id() {
        return id;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /** The station's name as shown in lists and on the HUD. */
    public String stationName() {
        return stationName.isBlank() ? Component.translatable(DEFAULT_NAME_KEY).getString() : stationName;
    }

    public void setStationName(String name) {
        this.stationName = name.strip();
        setChanged();
    }

    /** The ship this core is built into, or null while it sits in the world as plain blocks. */
    @Nullable
    public ServerSubLevel ship() {
        return level instanceof ServerLevel server ? Ships.shipAt(server, worldPosition) : null;
    }

    /** Where the core is in the world right now: its block, or its block carried by its ship. */
    public Vec3 worldSpot() {
        Vec3 centre = Vec3.atCenterOf(worldPosition);
        ServerSubLevel ship = ship();
        return ship == null ? centre : ship.logicalPose().transformPosition(centre);
    }

    /** Non-empty slots, for the peripheral. */
    public int filledSlots() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            Stations.track(this);
        }
    }

    /** The chest's title is the station's name (the container's own custom name is not used). */
    @Override
    protected Component getDefaultName() {
        return stationName.isBlank() ? Component.translatable(DEFAULT_NAME_KEY) : Component.literal(stationName);
    }

    /** A Station Core item renamed in an anvil names the station it becomes. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        Component custom = input.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (custom != null) {
            stationName = custom.getString().strip();
        }
    }

    /** Breaking the core keeps the station's name on the item. */
    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!stationName.isBlank()) {
            components.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(stationName));
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.hasUUID("id")) {
            id = tag.getUUID("id");
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        stationName = tag.getString("station_name");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putUUID("id", id);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putString("station_name", stationName);
    }
}
