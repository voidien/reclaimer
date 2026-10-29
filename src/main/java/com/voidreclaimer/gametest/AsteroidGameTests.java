package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.asteroid.AsteroidBody;
import com.voidreclaimer.asteroid.AsteroidChunkGenerator;
import com.voidreclaimer.asteroid.AsteroidField;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 6: the asteroid generator, drill gating, and what each tool gives. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AsteroidGameTests {
    private AsteroidGameTests() {}

    private static AsteroidField fieldOf(ServerLevel level) {
        Vec3 spawn = new Vec3(Config.SPAWN_X.get() + 0.5, Config.SPAWN_Y.get(), Config.SPAWN_Z.get() + 0.5);
        return new AsteroidField(level.getSeed(), spawn, AsteroidChunkGenerator.SPAWN_CLEARANCE);
    }

    /** The first body of {@code size} in the cells around the origin that the field puts in the deep space world. */
    private static AsteroidBody firstBody(AsteroidField field, AsteroidBody.Size size) {
        for (int r = 0; r < 8; r++) {
            for (int cx = -r; cx <= r; cx++) {
                for (int cz = -r; cz <= r; cz++) {
                    for (int cy = -1; cy <= 0; cy++) {
                        for (AsteroidBody body : field.bodiesIn(cx, cy, cz)) {
                            if (body.size() == size) {
                                return body;
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no " + size + " body near the origin");
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void generatorBuildsTheFieldsBodies(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        AsteroidBody body = firstBody(fieldOf(space), AsteroidBody.Size.LARGE);
        BlockPos middle = BlockPos.containing(body.center());
        BlockState expected = AsteroidChunkGenerator.blockFor(body.kind());
        space.setChunkForced(middle.getX() >> 4, middle.getZ() >> 4, true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(space.getBlockState(middle).equals(expected), "no " + body.kind() + " rock at the middle of " + body + ", found "
                    + space.getBlockState(middle));
            double reach = body.maxExtent();
            BlockPos outside = BlockPos.containing(body.center().add(reach + 3, 0, 0));
            space.setChunkForced(outside.getX() >> 4, outside.getZ() >> 4, true);
            helper.runAfterDelay(20, () -> {
                // Beyond the body's outer limit there is no rock of its own (other bodies stay in other cells).
                helper.assertFalse(space.getBlockState(BlockPos.containing(body.center().add(reach + 1, 0, 0))).equals(expected),
                        "rock outside the body's limit");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void spawnSurroundingsStayEmpty(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos spawn = new BlockPos(Config.SPAWN_X.get(), Config.SPAWN_Y.get(), Config.SPAWN_Z.get());
        space.setChunkForced(spawn.getX() >> 4, spawn.getZ() >> 4, true);
        helper.runAfterDelay(20, () -> {
            for (int dx = -8; dx <= 8; dx += 8) {
                for (int dy = -8; dy <= 8; dy += 8) {
                    BlockState state = space.getBlockState(spawn.offset(dx, dy, 0));
                    helper.assertFalse(state.is(ModBlocks.ASTEROID_BLOCKS), "asteroid rock next to the spawn at " + dx + "," + dy);
                }
            }
            helper.succeed();
        });
    }

    private static final BlockPos ROCK_AREA = new BlockPos(84_000, 80, 0);

    /** A survival player with {@code tool} in the first tool mount, facing a rock block of {@code kind} in a cleared area. */
    private static void withRock(GameTestHelper helper, ItemStack tool, java.util.function.Consumer<Rock> body) {
        NaniteGameTests.inLoadedArea(helper, ROCK_AREA, 8, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, ROCK_AREA.getX() + 0.5, ROCK_AREA.getY(), ROCK_AREA.getZ() + 0.5);
            try {
                if (!tool.isEmpty()) {
                    TestPlayers.installModule(player, ModuleKind.TOOL, 0, tool);
                }
                BlockPos pos = ROCK_AREA.offset(2, 0, 0);
                player.serverLevel().setBlock(pos, ModBlocks.ASTEROID_METALLIC.get().defaultBlockState(), 3);
                body.accept(new Rock(player, pos));
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    private record Rock(ServerPlayer player, BlockPos pos) {
        BlockState state() {
            return player.serverLevel().getBlockState(pos);
        }

        float speed() {
            PlayerEvent.BreakSpeed event = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(player, state(), 1f, pos));
            return event.getNewSpeed();
        }

        BlockEvent.BreakEvent breakIt() {
            return NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(player.serverLevel(), pos, state(), player));
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void rockCannotBeBrokenWithoutADrill(GameTestHelper helper) {
        withRock(helper, ItemStack.EMPTY, rock -> {
            helper.assertTrue(rock.speed() == 0f, "digging speed " + rock.speed() + " without a drill");
            helper.assertTrue(rock.breakIt().isCanceled(), "break allowed without a drill");
            helper.assertTrue(rock.state().is(ModBlocks.ASTEROID_METALLIC.get()), "rock removed");
            // Ordinary blocks are untouched by the rule.
            rock.player().serverLevel().setBlock(rock.pos(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertFalse(rock.breakIt().isCanceled(), "stone blocked");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void drillGrindsRockToDustOfItsKind(GameTestHelper helper) {
        withRock(helper, new ItemStack(ModItems.MECHANICAL_DRILL.get()), rock -> {
            ServerPlayer player = rock.player();
            helper.assertTrue(rock.speed() > 5f, "drill speed " + rock.speed());
            float dustBefore = player.getData(ModAttachments.DUST).get(DustKinds.METALLIC);
            float energyBefore = player.getData(ModAttachments.ENERGY).energy();
            BlockEvent.BreakEvent event = rock.breakIt();
            helper.assertTrue(event.isCanceled(), "the drill leaves the vanilla break to run");
            helper.assertTrue(rock.state().isAir(), "rock still there");
            float gained = player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - dustBefore;
            helper.assertTrue(Math.abs(gained - Config.DRILL_DUST_YIELD.get()) < 1e-3, "dust gained " + gained);
            helper.assertTrue(player.getData(ModAttachments.DUST).get(DustKinds.SILICATE) == 0f, "another kind gained dust");
            helper.assertTrue(energyBefore - player.getData(ModAttachments.ENERGY).energy() > Config.DRILL_ENERGY.get() - 0.5,
                    "no energy used");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void harvesterLetsTheBlockDropWhole(GameTestHelper helper) {
        withRock(helper, new ItemStack(ModItems.MATTER_HARVESTER.get()), rock -> {
            ServerPlayer player = rock.player();
            float speed = rock.speed();
            helper.assertTrue(speed > 1f && speed < 10f, "harvester speed " + speed + " should beat bare hands but trail the drill");
            float dustBefore = player.getData(ModAttachments.DUST).total();
            float energyBefore = player.getData(ModAttachments.ENERGY).energy();
            helper.assertFalse(rock.breakIt().isCanceled(), "harvester break was cancelled");
            helper.assertTrue(player.getData(ModAttachments.DUST).total() == dustBefore, "harvester refunded dust");
            helper.assertTrue(energyBefore - player.getData(ModAttachments.ENERGY).energy() > Config.DRILL_ENERGY.get() - 0.5, "no energy used");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void drillIsOfflineInLowPowerMode(GameTestHelper helper) {
        withRock(helper, new ItemStack(ModItems.MECHANICAL_DRILL.get()), rock -> {
            EnergyData energy = rock.player().getData(ModAttachments.ENERGY);
            rock.player().setData(ModAttachments.ENERGY, new EnergyData(0f, energy.capacity(), true));
            helper.assertTrue(rock.speed() == 0f, "powered-down drill still digs");
            helper.assertTrue(rock.breakIt().isCanceled() && !rock.state().isAir(), "powered-down drill broke the rock");
            helper.succeed();
        });
    }
}
