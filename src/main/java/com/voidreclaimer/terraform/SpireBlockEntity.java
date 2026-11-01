package com.voidreclaimer.terraform;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.terraform.TerraformProfile;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import com.voidreclaimer.machine.MachineMath;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModTags;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Terraforming Spire, the controller of its tower ({@link SpireAssembly}), and its Genesis Pulse: a square wave that
 * sweeps out from the tower's centre over {@code duration} ticks, rewriting each ring's blocks as it passes by the
 * {@code void_reclaimer:restored} profile, then rewriting the biome of the whole area and resending its chunks. A tower's
 * pulse stops at the edges of the {@link Sectors sector} it stands in, however far from the middle, and restores that
 * sector when it ends. A pulse in progress is saved with the block and carries on after a reload, whatever happens to the
 * tower. The tower is drawn from this block entity (GeckoLib, {@code geo/entity/spire_tower.geo.json}).
 *
 * <p>A pulse is paid for. Firing checks the cost and starts a charge, which needs the shaft turning all the way; the cost
 * (catalysts from the one slot, Purified Water and Bio-Nutrient Slurry from the tanks) is taken when the charge ends and
 * the wave leaves, so nothing is spent on a charge that does not finish. Automation only puts things in ({@link #items()},
 * {@link #fluids()}).
 */
public class SpireBlockEntity extends KineticBlockEntity implements GeoBlockEntity {
    public enum Start {
        STARTED, BUSY, WRONG_WORLD, NOT_ASSEMBLED, OFF_PLANET, SECTOR_RESTORED, NO_PROFILE, NO_CATALYST, NO_WATER, NO_SLURRY, NO_ROTATION;

        /** The name scripts and lang keys use. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How often an assembled tower checks its frame and column. */
    static final int CHECK_TICKS = 20;

    /** Particle points along each ring: the corners and the edge middles. */
    private static final int[][] RING_POINTS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemStackHandler catalyst = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModTags.SPIRE_CATALYSTS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank water = tank(ModTags.SPIRE_WATER);
    private final FluidTank slurry = tank(ModTags.SPIRE_SLURRY);
    private final IItemHandler itemHandler = new ItemHandler();
    private final IFluidHandler fluidHandler = new FluidHandler();
    /** A charge is under way; {@link #charge} counts ticks at the reference speed. */
    private boolean charging;
    private float charge;
    private boolean assembled;
    private int checkIn;
    /** Ticks into the pulse; -1 when idle. */
    private int elapsed = -1;
    private int radius;
    private int duration;
    private int pulses;
    /** Where the running pulse sweeps out from: the tower's centre, or this block for a pulse saved before towers. */
    private BlockPos pulseCentre;
    /** The sector the running pulse is restoring, or {@link Sectors#NONE} for one that only rewrites its square. */
    private int sector = Sectors.NONE;
    /** How many chunks the last finished pulse resent. Not saved. */
    private int lastChunksResent;

    public SpireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        pulseCentre = pos;
    }

    public SpireBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SPIRE.get(), pos, state);
    }

    /** The way the tower stands from the Spire. */
    public Direction facing() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    /** Whether the frame was complete and the column filled the last time the tower was assembled. */
    public boolean assembled() {
        return assembled;
    }

    void setAssembled(boolean value) {
        if (assembled == value) {
            return;
        }
        assembled = value;
        checkIn = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean running() {
        return elapsed >= 0;
    }

    /** Whether the tower is charging for a pulse. */
    public boolean charging() {
        return charging;
    }

    /** How far the charge has come, 0 to 1; 0 when none is under way. */
    public float chargeProgress() {
        return charging ? Math.min(1f, charge / Math.max(1, Config.SPIRE_CHARGE_TICKS.get())) : 0f;
    }

    /** What automation sees: catalysts go in, nothing comes out. */
    public IItemHandler items() {
        return itemHandler;
    }

    /** What pipes see: the two fluids go in, nothing comes out. */
    public IFluidHandler fluids() {
        return fluidHandler;
    }

    /** The raw catalyst slot, for the block itself, scripts and tests. */
    public ItemStackHandler catalyst() {
        return catalyst;
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank slurryTank() {
        return slurry;
    }

    /** Ticks into the running pulse, -1 when idle. */
    public int elapsed() {
        return elapsed;
    }

    /** How far the running pulse has swept, 0 to 1; 0 when idle. */
    public float progress() {
        return running() ? Math.min(1f, elapsed / (float) Math.max(1, duration)) : 0f;
    }

    /** The sector this tower stands in, or {@link Sectors#NONE} off the planet and in any other dimension. */
    public int sector() {
        if (level == null || !RuinedEarth.is(level)) {
            return Sectors.NONE;
        }
        BlockPos centre = SpireTower.centre(worldPosition, facing());
        return Sectors.at(centre.getX(), centre.getZ());
    }

    /** Blocks from the tower to the furthest edge of the running pulse's area, or of the next one; 0 with no sector. */
    public int radius() {
        if (running()) {
            return radius;
        }
        int here = sector();
        BlockPos centre = SpireTower.centre(worldPosition, facing());
        return here == Sectors.NONE ? 0 : Sectors.reach(here, centre.getX(), centre.getZ());
    }

    /** Pulses finished here so far. */
    public int pulses() {
        return pulses;
    }

    /** Where the running or last pulse swept out from. */
    public BlockPos pulseCentre() {
        return pulseCentre;
    }

    public int lastChunksResent() {
        return lastChunksResent;
    }

    /**
     * Starts the charge for a pulse over the tower's sector, if the tower stands in one that is not restored yet, none is
     * under way, the cost is there and the shaft turns. {@link Start#STARTED} means charging has begun.
     */
    public Start tryStart() {
        if (level == null || !RuinedEarth.is(level)) {
            return Start.WRONG_WORLD;
        }
        if (running() || charging) {
            return Start.BUSY;
        }
        if (!assembled) {
            return Start.NOT_ASSEMBLED;
        }
        int here = sector();
        if (here == Sectors.NONE) {
            return Start.OFF_PLANET;
        }
        if (SectorMap.of(level).isRestored(here)) {
            return Start.SECTOR_RESTORED;
        }
        if (profile(level).isEmpty()) {
            return Start.NO_PROFILE;
        }
        if (catalyst.getStackInSlot(0).getCount() < Config.SPIRE_CATALYSTS.get()) {
            return Start.NO_CATALYST;
        }
        if (water.getFluidAmount() < Config.SPIRE_WATER.get()) {
            return Start.NO_WATER;
        }
        if (slurry.getFluidAmount() < Config.SPIRE_SLURRY.get()) {
            return Start.NO_SLURRY;
        }
        if (!MachineMath.runs(getSpeed(), Config.SPIRE_MIN_RPM.get())) {
            return Start.NO_ROTATION;
        }
        charging = true;
        charge = 0f;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.6f);
        return Start.STARTED;
    }

    /** One tick of the charge: stops if the tower fell; waits while the shaft is still; pays and fires at the end. */
    private void chargeTick(ServerLevel server) {
        if (!assembled) {
            charging = false;
            charge = 0f;
            setChanged();
            return;
        }
        charge += MachineMath.progressPerTick(getSpeed(), Config.SPIRE_MIN_RPM.get());
        if (charge > 0f && server.getGameTime() % 10 == 0) {
            BlockPos top = SpireTower.centre(worldPosition, facing()).above(SpireTower.HEIGHT);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, 8, 1.0, 0.5, 1.0, 0.05);
        }
        if (charge < Config.SPIRE_CHARGE_TICKS.get()) {
            return;
        }
        charging = false;
        charge = 0f;
        setChanged();
        int here = sector();
        // Another tower may have finished this sector during the charge, or the numbers changed: nothing is paid then.
        if (here == Sectors.NONE || SectorMap.of(server).isRestored(here) || !pay()) {
            return;
        }
        start(radius(), Config.SPIRE_PULSE_TICKS.get(), here);
    }

    /** Takes one pulse's cost, all of it or none. */
    private boolean pay() {
        int catalysts = Config.SPIRE_CATALYSTS.get();
        int waterCost = Config.SPIRE_WATER.get();
        int slurryCost = Config.SPIRE_SLURRY.get();
        if (catalyst.getStackInSlot(0).getCount() < catalysts || water.getFluidAmount() < waterCost || slurry.getFluidAmount() < slurryCost) {
            return false;
        }
        catalyst.extractItem(0, catalysts, false);
        water.drain(waterCost, IFluidHandler.FluidAction.EXECUTE);
        slurry.drain(slurryCost, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /** Starts a pulse of the given size now, whatever the dimension: from the tower's centre, or this block without a tower. */
    public void start(int radius, int duration) {
        start(radius, duration, Sectors.NONE);
    }

    /** As {@link #start(int, int)}, kept inside {@code sector}, which it restores when it ends; {@link Sectors#NONE} for neither. */
    public void start(int radius, int duration, int sector) {
        this.radius = radius;
        this.duration = duration;
        this.sector = sector;
        this.elapsed = 0;
        this.pulseCentre = assembled ? SpireTower.centre(worldPosition, facing()) : worldPosition;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3f, 0.6f);
        }
    }

    private static Optional<TerraformProfile> profile(Level level) {
        return level.registryAccess().registry(VoidReclaimerRegistries.TERRAFORM_PROFILE)
                .flatMap(registry -> Optional.ofNullable(registry.get(TerraformProfiles.RESTORED)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (assembled && ++checkIn >= CHECK_TICKS) {
            checkIn = 0;
            SpireAssembly.check(this);
        }
        applyCapacity();
        if (charging) {
            chargeTick(server);
        }
        pulseTick(server);
    }

    private void pulseTick(ServerLevel server) {
        if (elapsed < 0) {
            return;
        }
        Optional<TerraformProfile> profile = profile(server);
        if (profile.isEmpty()) {
            elapsed = -1;
            return;
        }
        PulseSchedule schedule = new PulseSchedule(radius, duration);
        elapsed++;
        schedule.forNewRings(elapsed, ring -> {
            PulseSchedule.forRing(ring, (dx, dz) -> {
                int x = pulseCentre.getX() + dx;
                int z = pulseCentre.getZ() + dz;
                if (inArea(x, z)) {
                    Terraforming.swapColumn(server, profile.get(), x, z);
                }
            });
            for (int[] point : RING_POINTS) {
                int x = pulseCentre.getX() + point[0] * ring;
                int z = pulseCentre.getZ() + point[1] * ring;
                if (!inArea(x, z)) {
                    continue;
                }
                int y = server.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 3, 0.4, 0.6, 0.4, 0.0);
            }
        });
        if (schedule.isDone(elapsed)) {
            finish(server, profile.get());
        } else if (elapsed % 20 == 0) {
            setChanged();
        }
    }

    /** Whether the running pulse may touch the column at (x, z). */
    private boolean inArea(int x, int z) {
        return sector == Sectors.NONE || Sectors.contains(sector, x, z);
    }

    private void finish(ServerLevel level, TerraformProfile profile) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(profile.biome());
        int minX = pulseCentre.getX() - radius;
        int maxX = pulseCentre.getX() + radius;
        int minZ = pulseCentre.getZ() - radius;
        int maxZ = pulseCentre.getZ() + radius;
        if (sector != Sectors.NONE) {
            minX = Math.max(minX, Sectors.minX(sector));
            maxX = Math.min(maxX, Sectors.minX(sector) + Sectors.SIZE - 1);
            minZ = Math.max(minZ, Sectors.minZ(sector));
            maxZ = Math.min(maxZ, Sectors.minZ(sector) + Sectors.SIZE - 1);
        }
        lastChunksResent = Terraforming.rewriteBiomes(level, minX, maxX, minZ, maxZ, biome);
        if (sector != Sectors.NONE) {
            EarthRestoration.restore(level, sector);
        }
        sector = Sectors.NONE;
        elapsed = -1;
        pulses++;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 3f, 0.5f);
    }

    /** A tank that holds what a fluid tag allows; its size is set by {@link #applyCapacity}. */
    private FluidTank tank(TagKey<Fluid> allowed) {
        return new FluidTank(Config.SPIRE_TANK.getDefault(), stack -> stack.is(allowed)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** The tank size is a config value, which isn't loaded when the block entity is constructed. */
    private void applyCapacity() {
        int capacity = Config.SPIRE_TANK.get();
        if (water.getCapacity() != capacity) {
            water.setCapacity(capacity);
            slurry.setCapacity(capacity);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("assembled", assembled);
        tag.putInt("elapsed", elapsed);
        tag.putInt("radius", radius);
        tag.putInt("duration", duration);
        tag.putInt("pulses", pulses);
        tag.putLong("pulseCentre", pulseCentre.asLong());
        tag.putInt("sector", sector);
        tag.putBoolean("charging", charging);
        tag.putFloat("charge", charge);
        if (!clientPacket) {
            tag.put("catalyst", catalyst.serializeNBT(registries));
            tag.put("water", water.writeToNBT(registries, new CompoundTag()));
            tag.put("slurry", slurry.writeToNBT(registries, new CompoundTag()));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        assembled = tag.getBoolean("assembled");
        elapsed = tag.contains("elapsed") ? tag.getInt("elapsed") : -1;
        radius = tag.getInt("radius");
        duration = Math.max(1, tag.getInt("duration"));
        pulses = tag.getInt("pulses");
        // A Spire saved before towers has no centre: its pulse carries on from the block itself.
        pulseCentre = tag.contains("pulseCentre") ? BlockPos.of(tag.getLong("pulseCentre")) : worldPosition;
        sector = tag.contains("sector") ? tag.getInt("sector") : Sectors.NONE;
        charging = tag.getBoolean("charging");
        charge = tag.getFloat("charge");
        if (!clientPacket) {
            if (tag.contains("catalyst")) {
                catalyst.deserializeNBT(registries, tag.getCompound("catalyst"));
            }
            water.readFromNBT(registries, tag.getCompound("water"));
            slurry.readFromNBT(registries, tag.getCompound("slurry"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spire", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private final class ItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return catalyst.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return catalyst.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return catalyst.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return catalyst.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return catalyst.isItemValid(slot, stack);
        }
    }

    private final class FluidHandler implements IFluidHandler {
        private FluidTank tank(int index) {
            return index == 0 ? water : slurry;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank(index).getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank(index).getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank(index).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.isFluidValid(resource) ? water.fill(resource, action) : slurry.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
