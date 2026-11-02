package com.voidreclaimer.weather;

import com.voidreclaimer.Config;
import com.voidreclaimer.body.BodyCrash;
import com.voidreclaimer.body.BoxCollision;
import com.voidreclaimer.body.CrashRules;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A rock in a debris storm. It flies straight and ends at the first thing it meets, a body by its hit boxes, or a block, which it breaks if
 * it is weak enough by the crash rule. It is never saved and never a real block, so a storm leaves nothing behind but what it broke.
 */
public class DebrisRock extends Entity implements ItemSupplier {
    /** Most of a storm's rocks in the air around one body. */
    private static final int MOST_AROUND_A_BODY = 24;
    private static final double SPAWN_NEAR = 32.0;
    private static final double SPAWN_FAR = 48.0;
    private static final double SLOWEST = 18.0;
    private static final double FASTEST = 30.0;
    /** How far from a body's middle a rock may be aimed. */
    private static final double SCATTER = 3.0;
    /** A rock counts as this heavy in the crash rule: light, so even the fastest rock breaks glass and stops at stone. */
    private static final double MASS = 0.5;
    private static final int LIFE_TICKS = 200;
    private static final ItemStack LOOK = new ItemStack(Items.COBBLESTONE);

    public DebrisRock(EntityType<? extends DebrisRock> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** One rock from {@code from}, moving {@code speed} blocks a tick. */
    public static DebrisRock launch(ServerLevel level, Vec3 from, Vec3 speed) {
        DebrisRock rock = new DebrisRock(ModEntities.DEBRIS_ROCK.get(), level);
        rock.setPos(from);
        rock.setDeltaMovement(speed);
        level.addFreshEntity(rock);
        return rock;
    }

    /** One rock from somewhere around {@code body}, aimed at it, unless there are enough in the air there already. */
    public static void aimAt(ServerPlayer body) {
        ServerLevel level = body.serverLevel();
        Vec3 middle = body.getBoundingBox().getCenter();
        if (level.getEntitiesOfClass(DebrisRock.class, new AABB(middle, middle).inflate(SPAWN_FAR)).size() >= MOST_AROUND_A_BODY) {
            return;
        }
        RandomSource random = body.getRandom();
        Vec3 way = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
        if (way.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 from = middle.add(way.normalize().scale(SPAWN_NEAR + random.nextDouble() * (SPAWN_FAR - SPAWN_NEAR)));
        Vec3 aim = middle.add((random.nextDouble() - 0.5) * SCATTER, (random.nextDouble() - 0.5) * SCATTER, (random.nextDouble() - 0.5) * SCATTER);
        BlockPos start = BlockPos.containing(from);
        if (!level.getBlockState(start).getCollisionShape(level, start).isEmpty()) {
            return;
        }
        double speed = (SLOWEST + random.nextDouble() * (FASTEST - SLOWEST)) / FlightStats.TICKS_PER_SECOND;
        launch(level, from, aim.subtract(from).normalize().scale(speed));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        if (!level().isClientSide && level() instanceof ServerLevel server) {
            if (tickCount > LIFE_TICKS) {
                discard();
                return;
            }
            BlockHitResult block = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            // Only as far as the block: a body behind it is covered.
            LivingEntity struck = BoxCollision.bodyOnLine(server, from, block.getType() == HitResult.Type.MISS ? to : block.getLocation());
            if (struck != null) {
                hit(server, struck);
                return;
            }
            if (block.getType() != HitResult.Type.MISS) {
                hit(server, block);
                return;
            }
        }
        setPos(to);
    }

    private void hit(ServerLevel level, LivingEntity body) {
        if (body instanceof ServerPlayer player) {
            BodyCrash.struck(player, speed(), Config.WEATHER_ROCK_DAMAGE.get());
        }
        end(level);
    }

    private void hit(ServerLevel level, BlockHitResult block) {
        if (BodyCrash.canBreak(level, block.getBlockPos(), CrashRules.hardness(speed(), MASS, Config.CRASH_SAFE_SPEED.get()))) {
            level.destroyBlock(block.getBlockPos(), true);
        }
        end(level);
    }

    private void end(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        discard();
    }

    /** In blocks a second. */
    private double speed() {
        return getDeltaMovement().length() * FlightStats.TICKS_PER_SECOND;
    }

    @Override
    public ItemStack getItem() {
        return LOOK;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
