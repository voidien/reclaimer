package com.voidreclaimer.debug;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.earth.EarthSky;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Plain-text facts about what a free camera is looking at, read from the server's real state: the first block or entity along the
 * view with its saved data, inventories, tanks, energy and Create rotation, plus the place the camera is in and the player. Photo
 * mode's debug overlay shows these and saves them beside its screenshots, so a check can read exact numbers instead of guessing
 * from pixels. Call it on the server thread.
 */
public final class DebugReadout {
    /** How far along the view the readout looks, in blocks. */
    public static final double REACH = 256.0;

    /** What the look ray hit. {@code pos} is a block's position, or null for an entity or nothing. */
    public record Target(@Nullable BlockPos pos, @Nullable Entity entity, double distance) {
        public static final Target NONE = new Target(null, null, REACH);
    }

    private DebugReadout() {}

    /** The first thing along the view: the nearer of the first outlined block and the first entity. */
    public static Target target(ServerLevel level, Vec3 from, Vec3 look) {
        Vec3 to = from.add(look.normalize().scale(REACH));
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
        boolean hitBlock = block.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? block.getLocation() : to;
        double best = from.distanceTo(end);
        Entity closest = null;
        for (Entity entity : level.getEntities((Entity) null, new AABB(from, end).inflate(1.0), e -> true)) {
            var hit = entity.getBoundingBox().clip(from, end);
            if (hit.isPresent() && from.distanceTo(hit.get()) < best) {
                best = from.distanceTo(hit.get());
                closest = entity;
            }
        }
        if (closest != null) {
            return new Target(null, closest, best);
        }
        return hitBlock ? new Target(block.getBlockPos(), null, best) : Target.NONE;
    }

    /** Every line for a camera at {@code from} looking along {@code look}; {@code player} adds the player's own block. */
    public static List<String> lines(ServerLevel level, Vec3 from, Vec3 look, @Nullable ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        Target target = target(level, from, look);
        if (target.pos() != null) {
            lines.addAll(block(level, target.pos(), target.distance()));
        } else if (target.entity() != null) {
            lines.addAll(entity(target.entity(), target.distance()));
        } else {
            lines.add("Nothing within " + (int) REACH + " blocks");
        }
        lines.addAll(place(level, from));
        if (player != null) {
            lines.addAll(player(player));
        }
        return lines;
    }

    /** A block's id, properties, block entity, storage and saved data. */
    public static List<String> block(ServerLevel level, BlockPos pos, double distance) {
        List<String> lines = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        lines.add("Block " + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " at " + pos.toShortString() + " (" + one(distance) + " away)");
        if (!state.getValues().isEmpty()) {
            StringBuilder properties = new StringBuilder("  state");
            state.getValues().forEach((property, value) -> properties.append(' ').append(property.getName()).append('=').append(value));
            lines.add(properties.toString());
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            lines.add("  block entity " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
            if (blockEntity instanceof KineticBlockEntity kinetic) {
                lines.add("  rotation " + one(kinetic.getSpeed()) + " rpm, stress impact " + one(kinetic.calculateStressApplied())
                        + (kinetic.isOverStressed() ? ", OVERSTRESSED" : "") + (kinetic.hasNetwork() ? "" : ", no network"));
            }
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            int used = 0;
            for (int slot = 0; slot < items.getSlots(); slot++) {
                used += items.getStackInSlot(slot).isEmpty() ? 0 : 1;
            }
            lines.add("  items " + used + " of " + items.getSlots() + " slots used");
        }
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (fluids != null) {
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                FluidStack stack = fluids.getFluidInTank(tank);
                lines.add("  tank " + tank + ": " + (stack.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(stack.getFluid()) + " " + stack.getAmount())
                        + " of " + fluids.getTankCapacity(tank) + " mB");
            }
        }
        IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (energy != null) {
            lines.add("  energy " + energy.getEnergyStored() + " of " + energy.getMaxEnergyStored() + " FE");
        }
        if (blockEntity != null) {
            lines.add("  data " + blockEntity.saveWithoutMetadata(level.registryAccess()));
        }
        return lines;
    }

    /** An entity's type, place, motion and saved data. */
    public static List<String> entity(Entity entity, double distance) {
        List<String> lines = new ArrayList<>();
        lines.add("Entity " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " (" + one(distance) + " away)");
        lines.add("  at " + vec(entity.position()) + ", moving " + vec(entity.getDeltaMovement()) + " per tick");
        lines.add("  box " + one(entity.getBbWidth()) + " x " + one(entity.getBbHeight()) + ", scale "
                + (entity instanceof net.minecraft.world.entity.LivingEntity living ? one(living.getScale()) : "1"));
        if (!entity.getTags().isEmpty()) {
            lines.add("  tags " + entity.getTags());
        }
        lines.add("  data " + entity.saveWithoutId(new CompoundTag()));
        return lines;
    }

    /** The place itself: dimension, light, the dust cloud, Earth's era. */
    public static List<String> place(ServerLevel level, Vec3 at) {
        List<String> lines = new ArrayList<>();
        BlockPos pos = BlockPos.containing(at);
        lines.add("Place " + level.dimension().location() + " at " + vec(at));
        lines.add("  light sky " + level.getBrightness(LightLayer.SKY, pos) + ", block " + level.getBrightness(LightLayer.BLOCK, pos)
                + (level.isLoaded(pos) ? "" : ", not loaded"));
        DustField field = DustField.of(level);
        if (field != null) {
            DustField.Sample sample = field.sample(at);
            lines.add(sample.cloud() == null ? "  no dust cloud" : "  cloud " + sample.cloud().kind() + ", density " + two(sample.density()));
        }
        if (level.dimension().equals(RuinedEarth.DIMENSION)) {
            lines.add("  sky era " + EarthSky.get(level.getServer()) + ", " + com.voidreclaimer.earth.EarthRestoration.map(level.getServer()).count() + " of 16 sectors restored");
        }
        return lines;
    }

    /** The player's position, body and reserves. */
    public static List<String> player(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        BodyStats stats = Bodies.stats(player);
        lines.add("Player at " + vec(player.position()) + ", moving " + vec(player.getDeltaMovement()) + " per tick");
        lines.add("  body " + stats.size().location() + ", scale " + one(stats.scale()) + ", mass " + one(stats.mass()) + " of " + one(stats.massBudget())
                + (stats.overloaded() ? " OVERLOADED" : ""));
        lines.add("  energy " + one(player.getData(ModAttachments.ENERGY).energy()) + " of " + one(stats.energyCapacity())
                + ", dust " + one(player.getData(ModAttachments.DUST).total()));
        return lines;
    }

    private static String vec(Vec3 v) {
        return one(v.x) + " " + one(v.y) + " " + one(v.z);
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
