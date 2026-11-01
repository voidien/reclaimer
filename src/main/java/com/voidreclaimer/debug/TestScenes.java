package com.voidreclaimer.debug;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.launch.LaunchComplex;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.launch.LaunchPad;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.station.StationAssembly;
import com.voidreclaimer.station.StationCoreBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Ready-made setups for checks: one command builds a scene in deep space, far from any play, through the real assembly code, so a
 * check starts at the thing being looked at. Each scene has its own spot and can be built again over itself.
 */
public final class TestScenes {
    /** Half the arena's floor, in blocks: room for a Titan with the camera at fit distance. */
    public static final int ARENA_HALF = 64;
    public static final int ARENA_WALL = 48;
    private static final int SPACING = 512;
    private static final BlockPos FIRST = new BlockPos(20_000, 200, 20_000);
    private static final Direction PAD_FACING = Direction.SOUTH;
    private static final int STRIPE = 16;
    private static final int LAMP = 8;
    private static final String STATION_NAME = "Test Station";
    /** The launch motor's speed: a creative motor starts at 16, and the driver needs 64 to wind up. */
    private static final int LAUNCH_RPM = 128;

    /** The scenes, by name in the command. */
    public enum Scene {
        ARENA, LAUNCH, DRONES, STATION;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Where to stand to look at a scene. */
    public record View(Vec3 at, float yaw, float pitch) {}

    private TestScenes() {}

    /** The centre of a scene. */
    public static BlockPos origin(Scene scene) {
        return FIRST.offset(scene.ordinal() * SPACING, 0, 0);
    }

    /** Builds a scene in deep space. {@code owner} owns its drones and station, and may be null in a test. */
    public static View build(ServerLevel space, Scene scene, @Nullable UUID owner) {
        if (!space.dimension().equals(ModDimensions.DEEP_SPACE)) {
            throw new IllegalArgumentException("scenes are built in deep space");
        }
        BlockPos origin = origin(scene);
        return switch (scene) {
            case ARENA -> {
                arena(space, origin, ARENA_HALF, ARENA_WALL);
                yield new View(Vec3.atCenterOf(origin.above(4)), 0f, 0f);
            }
            case LAUNCH -> launch(space, origin);
            case DRONES -> drones(space, origin, owner);
            case STATION -> station(space, origin, owner);
        };
    }

    /** A pale floor with a back and a side wall, lit, with a line every 16 blocks to read sizes by. Dark bodies show up against it. */
    public static void arena(ServerLevel level, BlockPos origin, int half, int wall) {
        for (int a = -half; a < half; a++) {
            for (int b = -half; b < half; b++) {
                set(level, origin.offset(a, 0, b), panel(a, b));
            }
            for (int up = 1; up < wall; up++) {
                set(level, origin.offset(a, up, half), panel(a, up));
                set(level, origin.offset(-half, up, a), panel(a, up));
            }
        }
    }

    private static BlockState panel(int a, int b) {
        if (a % LAMP == 0 && b % LAMP == 0) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        }
        return (a % STRIPE == 0 || b % STRIPE == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** A complete launch complex turned by a creative motor, with payloads loaded. */
    private static View launch(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity old) {
            LaunchComplex.disassemble(old);
        }
        level.setBlock(pos, ModBlocks.LAUNCH_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, PAD_FACING), Block.UPDATE_ALL);
        for (BlockPos cell : LaunchPad.ring(pos, PAD_FACING)) {
            level.setBlock(cell, ModBlocks.SCAFFOLD.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos motorPos = pos.relative(PAD_FACING.getOpposite());
        level.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, PAD_FACING), Block.UPDATE_ALL);
        if (level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor) {
            motor.generatedSpeed.setValue(LAUNCH_RPM);
        }
        if (level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller) {
            LaunchComplex.assemble(controller);
            var slots = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (slots != null) {
                ItemHandlerHelper.insertItem(slots, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 16), false);
            }
        }
        return new View(Vec3.atCenterOf(LaunchPad.centre(pos, PAD_FACING)).add(26, 18, 0), 90f, 20f);
    }

    /** A ferry between two chests, and a miner working a line of rock into a third. */
    private static View drones(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        level.getEntitiesOfClass(HelperDrone.class, new AABB(pos).inflate(32)).forEach(HelperDrone::discard);
        BlockPos from = pos.offset(-8, 0, -4);
        BlockPos to = pos.offset(8, 0, -4);
        BlockPos rockChest = pos.offset(-8, 0, 4);
        for (BlockPos chest : new BlockPos[] {from, to, rockChest}) {
            level.setBlock(chest, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockEntity source = level.getBlockEntity(from);
        if (source instanceof Container container) {
            for (int slot = 0; slot < 6; slot++) {
                container.setItem(slot, new ItemStack(Items.STONE, 64));
            }
        }
        BlockPos rock = pos.offset(4, 0, 4);
        for (int i = 0; i < 6; i++) {
            level.setBlock(rock.offset(i, 0, 0), ModBlocks.ASTEROID_SILICATE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        HelperDrone.spawn(level, Vec3.atCenterOf(from.above(2)), new HelperLinks(Optional.of(from), Optional.of(to)), owner);
        HelperDrone.spawn(level, Vec3.atCenterOf(rockChest.above(2)),
                new HelperLinks(Optional.empty(), Optional.of(rockChest), Optional.of(rock)), owner);
        return new View(Vec3.atCenterOf(pos).add(0, 12, -18), 0f, 30f);
    }

    /** A small assembled station with a core, built once: building again only goes back to it. */
    private static View station(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        // Asked of Sable directly: the station list only catches up on the next server tick.
        boolean built = SubLevelContainer.getContainer(level).getAllSubLevels().stream()
                .anyMatch(ship -> !ship.isRemoved() && STATION_NAME.equals(ship.getName()));
        if (!built) {
            for (int a = -4; a <= 4; a++) {
                for (int b = -2; b <= 2; b++) {
                    level.setBlock(pos.offset(a, -1, b), Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            level.setBlock(pos, ModBlocks.STATION_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.offset(2, 0, 0), ModBlocks.HELM.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof StationCoreBlockEntity core) {
                core.setStationName(STATION_NAME);
                core.setOwner(owner);
                StationAssembly.assemble(core);
            }
        }
        return new View(Vec3.atCenterOf(pos).add(0, 6, -14), 0f, 20f);
    }
}
