package com.voidreclaimer.helper;

import com.voidreclaimer.Config;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustLedger;
import com.voidreclaimer.dust.DustTicker;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A helper drone: a small flying worker drawn with the mining probe model. Its jobs, chosen by its links:
 * <ul>
 * <li>the ferry ({@link Ferry}): it carries items from its source inventory to its destination inventory, up to
 * {@code helper.carry} at a time;</li>
 * <li>mining ({@link Mining}): it flies to the nearest asteroid block within {@code helper.mineRadius} of its mine
 * point, digs it for {@code helper.digTicks}, and carries the block to its destination;</li>
 * <li>collecting ({@link Collecting}): it hovers at its collect point gathering the cloud's dust into a tank of
 * {@code helper.tank}, and when full flies to its owner (online, within {@code helper.ownerRange}) and hands it over.</li>
 * </ul>
 * It flies at {@code helper.speed} blocks per tick, round blocks ({@link HelperSteering}), and hovers at a free face of what
 * it works on, the top if it can. With no way there it holds still and says so. It has no gravity. Sneak-use with an empty
 * hand takes it back as an item with its links; a hit breaks it into that item as well. Right-click reports what it is doing.
 */
public class HelperDrone extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(HelperDrone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> MINE_PHASE = SynchedEntityData.defineId(HelperDrone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> COLLECT_PHASE = SynchedEntityData.defineId(HelperDrone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HOLDING = SynchedEntityData.defineId(HelperDrone.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.void_reclaimer.mining_probe.idle");
    private static final RawAnimation THRUST = RawAnimation.begin().thenLoop("animation.void_reclaimer.mining_probe.thrust");
    /** The drone hovers this far above the block it works on. */
    public static final double HOVER = 1.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private HelperLinks links = HelperLinks.EMPTY;
    private ItemStack carried = ItemStack.EMPTY;
    private int wait;
    @Nullable
    private UUID owner;
    /** Mining: the rock being worked, how long it has been dug, and when to look for a rock again. */
    @Nullable
    private BlockPos rock;
    private int dug;
    private int scanIn;
    /** Collecting: the dust gathered and not yet handed over. */
    private DustData tank = DustData.EMPTY;
    /** The way to the current target. Not saved: a drone works it out again after a load. */
    private final HelperSteering.Route route = new HelperSteering.Route();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final HelperSteering.Cells cells = this::canFlyThrough;

    public HelperDrone(EntityType<? extends HelperDrone> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawns a drone at {@code pos} with {@code links}, owned by {@code owner} (may be null). */
    public static HelperDrone spawn(ServerLevel level, Vec3 pos, HelperLinks links, @Nullable UUID owner) {
        HelperDrone drone = new HelperDrone(ModEntities.HELPER_DRONE.get(), level);
        drone.setPos(pos.x, pos.y, pos.z);
        drone.links = links;
        drone.owner = owner;
        if (links.collecting()) {
            drone.setCollectPhase(Collecting.after(Collecting.Phase.IDLE, Collecting.Action.NONE, links.complete(), false, false, false, false, false));
        } else if (links.mining()) {
            drone.setMinePhase(Mining.after(Mining.Phase.IDLE, Mining.Action.NONE, links.complete(), false, false, false, false));
        } else {
            drone.setPhase(Ferry.after(Ferry.Phase.IDLE, Ferry.Action.NONE, links.complete(), false, false));
        }
        level.addFreshEntity(drone);
        return drone;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, (byte) Ferry.Phase.IDLE.ordinal());
        builder.define(MINE_PHASE, (byte) Mining.Phase.IDLE.ordinal());
        builder.define(COLLECT_PHASE, (byte) Collecting.Phase.IDLE.ordinal());
        builder.define(HOLDING, false);
    }

    public Collecting.Phase collectPhase() {
        return Collecting.Phase.values()[entityData.get(COLLECT_PHASE)];
    }

    private void setCollectPhase(Collecting.Phase phase) {
        entityData.set(COLLECT_PHASE, (byte) phase.ordinal());
    }

    /** The dust a collector holds. */
    public DustData tank() {
        return tank;
    }

    public Ferry.Phase phase() {
        return Ferry.Phase.values()[entityData.get(PHASE)];
    }

    private void setPhase(Ferry.Phase phase) {
        entityData.set(PHASE, (byte) phase.ordinal());
    }

    public Mining.Phase minePhase() {
        return Mining.Phase.values()[entityData.get(MINE_PHASE)];
    }

    private void setMinePhase(Mining.Phase phase) {
        entityData.set(MINE_PHASE, (byte) phase.ordinal());
    }

    public HelperLinks links() {
        return links;
    }

    public ItemStack carried() {
        return carried;
    }

    /** For tests: what the drone holds. */
    public void setCarried(ItemStack stack) {
        carried = stack;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    /** The rock a mining drone is heading for or digging, or null. */
    @Nullable
    public BlockPos rock() {
        return rock;
    }

    /** Whether the drone found no way to its target and is holding still until its next try. */
    public boolean blocked() {
        return route.blocked();
    }

    /** The waypoints of a detour still ahead. */
    public List<Vec3> waypoints() {
        return route.waypoints();
    }

    /** Whether the drone is flying somewhere (the thrust animation). */
    public boolean moving() {
        if (entityData.get(HOLDING)) {
            return false;
        }
        if (links.collecting()) {
            return Collecting.moving(collectPhase());
        }
        return links.mining() ? Mining.moving(minePhase()) : Ferry.moving(phase());
    }

    /** Where the drone is heading in its current phase, or empty when it is idle, waiting or digging. */
    public Optional<Vec3> target() {
        if (links.mining()) {
            return switch (minePhase()) {
                case TO_ROCK -> Optional.ofNullable(rock).map(this::spot);
                case TO_DESTINATION -> links.destination().map(this::spot);
                default -> Optional.empty();
            };
        }
        return switch (phase()) {
            case TO_SOURCE -> links.source().map(this::spot);
            case TO_DESTINATION -> links.destination().map(this::spot);
            default -> Optional.empty();
        };
    }

    private Vec3 spot(BlockPos pos) {
        return HelperSteering.workSpot(pos, cells);
    }

    /** Whether the drone can fly through a cell. A chunk that is not loaded counts as closed and is never loaded for this. */
    public boolean canFlyThrough(int x, int y, int z) {
        Level level = level();
        probe.set(x, y, z);
        if (level.isOutsideBuildHeight(y) || !level.isLoaded(probe)) {
            return false;
        }
        return level.getBlockState(probe).getCollisionShape(level, probe).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (links.collecting()) {
            tickCollecting(server);
        } else if (links.mining()) {
            tickMining(server);
        } else {
            tickFerry(server);
        }
    }

    /** Flies one step towards {@code target}; returns whether the drone is now within reach of it. */
    private boolean fly(Optional<Vec3> target) {
        if (target.isEmpty()) {
            route.clear();
            entityData.set(HOLDING, false);
            return false;
        }
        double reach = Config.HELPER_REACH.get();
        Vec3 next = HelperSteering.step(position(), target.get(), Config.HELPER_SPEED.get());
        setYRot(HelperSteering.yaw(position(), next, getYRot()));
        setPos(next.x, next.y, next.z);
        entityData.set(HOLDING, route.blocked());
        return HelperSteering.reached(next, target.get(), reach, cells);
    }

    private void tickFerry(ServerLevel server) {
        Ferry.Phase phase = phase();
        boolean arrived = fly(target());
        Ferry.Action action = Ferry.actionOn(phase, arrived);
        switch (action) {
            case LOAD -> links.source().ifPresent(pos -> load(server, pos));
            case UNLOAD -> links.destination().ifPresent(pos -> unload(server, pos));
            default -> { }
        }
        boolean waitOver = phase == Ferry.Phase.WAITING && --wait <= 0;
        Ferry.Phase next = Ferry.after(phase, action, links.complete(), !carried.isEmpty(), waitOver);
        if (next == Ferry.Phase.WAITING && phase != Ferry.Phase.WAITING) {
            wait = Config.HELPER_PAUSE.get();
        }
        if (next != phase) {
            setPhase(next);
        }
    }

    private void tickMining(ServerLevel server) {
        Mining.Phase phase = minePhase();
        boolean rockFound = rock != null && isRock(server, rock);
        if (!rockFound && --scanIn <= 0 && links.mine().isPresent()) {
            rock = findRock(server, links.mine().get());
            scanIn = Config.HELPER_PAUSE.get();
            rockFound = rock != null;
        }
        boolean arrived = fly(target());
        boolean digDone = phase == Mining.Phase.DIGGING && rockFound && ++dug >= Config.HELPER_DIG_TICKS.get();
        Mining.Action action = Mining.actionOn(phase, arrived, digDone);
        switch (action) {
            case DIG -> {
                if (rock != null && dug % 10 == 0) {
                    BlockState state = server.getBlockState(rock);
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), rock.getX() + 0.5, rock.getY() + 1.1, rock.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.05);
                }
            }
            case BREAK -> breakRock(server);
            case UNLOAD -> links.destination().ifPresent(pos -> unload(server, pos));
            default -> { }
        }
        boolean waitOver = phase == Mining.Phase.WAITING && --wait <= 0;
        Mining.Phase next = Mining.after(phase, action, links.complete(), rockFound, arrived, !carried.isEmpty(), waitOver);
        if (next == Mining.Phase.WAITING && phase != Mining.Phase.WAITING) {
            wait = Config.HELPER_PAUSE.get();
        }
        if (next == Mining.Phase.TO_ROCK && phase != Mining.Phase.TO_ROCK) {
            dug = 0;
        }
        if (next != phase) {
            setMinePhase(next);
        }
    }

    private void tickCollecting(ServerLevel server) {
        Collecting.Phase phase = collectPhase();
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        double range = Config.HELPER_OWNER_RANGE.get();
        boolean ownerNear = player != null && player.level() == server && player.distanceToSqr(this) <= range * range;
        Optional<Vec3> target = switch (phase) {
            case TO_POINT -> links.collect().map(this::spot);
            case TO_OWNER -> ownerNear ? Optional.of(spot(player.blockPosition())) : Optional.empty();
            default -> Optional.empty();
        };
        boolean arrived = fly(target);
        Collecting.Action action = Collecting.actionOn(phase, arrived);
        switch (action) {
            case GATHER -> gather(server);
            case DELIVER -> {
                if (player != null) {
                    deliver(player);
                }
            }
            default -> { }
        }
        boolean full = tank.total() >= Config.HELPER_TANK.get().floatValue() - 1.0e-4f;
        boolean waitOver = phase == Collecting.Phase.WAITING && --wait <= 0;
        Collecting.Phase next = Collecting.after(phase, action, links.complete(), full, tank.total() > 0f, ownerNear, arrived, waitOver);
        if (next == Collecting.Phase.WAITING && phase != Collecting.Phase.WAITING) {
            wait = Config.HELPER_PAUSE.get();
        }
        if (next != phase) {
            setCollectPhase(next);
        }
    }

    /** Gathers the cloud's dust where the drone hovers, as a still body would, times {@code helper.collectRate}. */
    private void gather(ServerLevel server) {
        DustField field = DustField.of(server);
        if (field == null) {
            return;
        }
        DustField.Sample sample = field.sample(position());
        if (!sample.inCloud() || sample.cloud() == null) {
            return;
        }
        float gain = DustTicker.gainPerTick(sample.density(), 0.0, Config.DUST_TRICKLE.get(), Config.DUST_SCOOP.get())
                * Config.HELPER_COLLECT_RATE.get().floatValue();
        tank = tank.add(sample.cloud().kind(), gain, Config.HELPER_TANK.get().floatValue());
    }

    /** Hands the tank's dust to the owner, as much as fits; the rest stays aboard. */
    private void deliver(ServerPlayer player) {
        DustData dust = player.getData(ModAttachments.DUST);
        float capacity = DustData.capacity(player);
        DustData left = tank;
        for (var entry : tank.amounts().entrySet()) {
            DustData after = dust.add(entry.getKey(), entry.getValue(), capacity);
            float moved = after.get(entry.getKey()) - dust.get(entry.getKey());
            dust = after;
            left = left.with(entry.getKey(), Math.max(0f, entry.getValue() - moved));
            DustLedger.record(player, entry.getKey(), moved);
        }
        player.setData(ModAttachments.DUST, dust);
        player.syncData(ModAttachments.DUST);
        tank = left;
    }

    private static boolean isRock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ASTEROID_BLOCKS);
    }

    /** The asteroid block nearest to {@code centre} within {@code helper.mineRadius} with an open face, or null. */
    @Nullable
    private BlockPos findRock(ServerLevel level, BlockPos centre) {
        int radius = Config.HELPER_MINE_RADIUS.get();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (isRock(level, pos)) {
                double distance = pos.distSqr(centre);
                if (distance < bestDistance && HelperSteering.exposed(pos, cells)) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    /** Takes the rock as a block item (as the Matter Harvester lets it drop) and clears it. */
    private void breakRock(ServerLevel server) {
        if (rock == null || !isRock(server, rock)) {
            rock = null;
            return;
        }
        ItemStack mined = new ItemStack(server.getBlockState(rock).getBlock().asItem());
        server.destroyBlock(rock, false);
        if (carried.isEmpty()) {
            carried = mined;
        } else if (ItemStack.isSameItemSameComponents(carried, mined) && carried.getCount() < Config.HELPER_CARRY.get()) {
            carried.grow(1);
        } else {
            level().addFreshEntity(new ItemEntity(level(), rock.getX() + 0.5, rock.getY() + 0.5, rock.getZ() + 0.5, mined));
        }
        rock = null;
        dug = 0;
    }

    /** Takes up to {@code helper.carry} items of the first non-empty slot of the inventory at {@code pos}. */
    private void load(ServerLevel server, BlockPos pos) {
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) {
            return;
        }
        int carry = Config.HELPER_CARRY.get();
        for (int slot = 0; slot < handler.getSlots() && carried.getCount() < carry; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, inSlot))) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, carry - carried.getCount(), false);
            if (taken.isEmpty()) {
                continue;
            }
            if (carried.isEmpty()) {
                carried = taken;
            } else {
                carried.grow(taken.getCount());
            }
        }
    }

    /** Gives what it carries to the inventory at {@code pos}; what does not fit stays aboard. */
    private void unload(ServerLevel server, BlockPos pos) {
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null || carried.isEmpty()) {
            return;
        }
        carried = ItemHandlerHelper.insertItem(handler, carried, false);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            ItemStack item = toItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            dropCarried();
            discard();
            player.displayClientMessage(Component.translatable("message.void_reclaimer.helper.picked_up"), true);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(status(), true);
        return InteractionResult.CONSUME;
    }

    /** One line about the job: the phase, what is carried, and the links. */
    public Component status() {
        if (route.blocked()) {
            return Component.translatable("message.void_reclaimer.helper.status.blocked", load());
        }
        String phase = (links.collecting() ? collectPhase().name() : links.mining() ? minePhase().name() : phase().name()).toLowerCase(java.util.Locale.ROOT);
        return Component.translatable("message.void_reclaimer.helper.status." + phase, load());
    }

    private Component load() {
        return links.collecting() ? Component.translatable("message.void_reclaimer.helper.dust", String.format(java.util.Locale.ROOT, "%.1f", tank.total()))
                : carried.isEmpty() ? Component.translatable("message.void_reclaimer.helper.empty")
                : Component.translatable("message.void_reclaimer.helper.carrying", carried.getCount(), carried.getHoverName());
    }

    /** The drone as an item, with its links. */
    public ItemStack toItem() {
        ItemStack item = new ItemStack(ModItems.HELPER_DRONE.get());
        item.set(ModDataComponents.HELPER_LINKS.get(), links);
        return item;
    }

    private void dropCarried() {
        if (!carried.isEmpty()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), carried.copy()));
            carried = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && !isRemoved()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), toItem()));
            dropCarried();
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        links = tag.contains("links") ? HelperLinks.CODEC.parse(NbtOps.INSTANCE, tag.get("links")).result().orElse(HelperLinks.EMPTY) : HelperLinks.EMPTY;
        carried = tag.contains("carried") ? ItemStack.parseOptional(registryAccess(), tag.getCompound("carried")) : ItemStack.EMPTY;
        wait = tag.getInt("wait");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        int phase = tag.getInt("phase");
        setPhase(phase >= 0 && phase < Ferry.Phase.values().length ? Ferry.Phase.values()[phase] : Ferry.Phase.IDLE);
        int minePhase = tag.getInt("minePhase");
        setMinePhase(minePhase >= 0 && minePhase < Mining.Phase.values().length ? Mining.Phase.values()[minePhase] : Mining.Phase.IDLE);
        rock = tag.contains("rock") ? NbtUtils.readBlockPos(tag, "rock").orElse(null) : null;
        dug = tag.getInt("dug");
        int collectPhase = tag.getInt("collectPhase");
        setCollectPhase(collectPhase >= 0 && collectPhase < Collecting.Phase.values().length ? Collecting.Phase.values()[collectPhase] : Collecting.Phase.IDLE);
        tank = tag.contains("tank") ? DustData.CODEC.parse(NbtOps.INSTANCE, tag.get("tank")).result().orElse(DustData.EMPTY) : DustData.EMPTY;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        HelperLinks.CODEC.encodeStart(NbtOps.INSTANCE, links).result().ifPresent(encoded -> tag.put("links", (Tag) encoded));
        if (!carried.isEmpty()) {
            tag.put("carried", carried.save(registryAccess()));
        }
        tag.putInt("wait", wait);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putInt("phase", phase().ordinal());
        tag.putInt("minePhase", minePhase().ordinal());
        if (rock != null) {
            tag.put("rock", NbtUtils.writeBlockPos(rock));
        }
        tag.putInt("dug", dug);
        tag.putInt("collectPhase", collectPhase().ordinal());
        DustData.CODEC.encodeStart(NbtOps.INSTANCE, tank).result().ifPresent(encoded -> tag.put("tank", (Tag) encoded));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4,
                state -> state.setAndContinue(moving() ? THRUST : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
