package com.voidreclaimer.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.compat.act.HoloScript;
import com.voidreclaimer.compat.create.HoloDisplaySetup;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloGlobes;
import com.voidreclaimer.display.HoloMapImage;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModRegistries;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.blockentity.ComputerBlockEntity;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.ComputerOS;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.Terminal;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.Interpreter;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.ScriptError;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Holo Projector: its block, its saved content, its refresh and what writes to it. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoloGameTests {
    private HoloGameTests() {}

    static HoloProjectorBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.HOLO_PROJECTOR.get().defaultBlockState(), 3);
        return (HoloProjectorBlockEntity) level.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void projectorPlacesAndSavesItsLines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            helper.assertTrue(projector.display().equals(HoloDisplay.EMPTY), "a new projector is empty");
            projector.setLine(0, "hello", HoloDisplay.NO_COLOR);
            projector.setLine(2, "world", 0xFF0000);
            projector.setScale(2f);
            HolderLookup.Provider registries = level.registryAccess();
            CompoundTag saved = projector.saveWithFullMetadata(registries);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            HoloProjectorBlockEntity restored = place(level, pos);
            helper.assertTrue(restored.display().equals(HoloDisplay.EMPTY), "the new block started with the old content");
            restored.loadWithComponents(saved, registries);
            helper.assertTrue(restored.display().texts().equals(List.of("hello", "", "world")), "lines " + restored.display().texts());
            helper.assertTrue(restored.display().lines().get(2).color() == 0xFF0000, "a line lost its colour");
            helper.assertTrue(restored.display().scale() == 2f, "scale " + restored.display().scale());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSneakUseCyclesTheReadoutSources(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_100, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLine(0, "text", HoloDisplay.NO_COLOR);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            // A plain use does nothing: the block is not a button.
            player.setShiftKeyDown(false);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.TEXT, "a plain use changed the projector");
            player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.READOUT, "a sneak-use did not pick a readout");
            var first = projector.display().source();
            helper.assertTrue(ModRegistries.HOLO_READOUTS.containsKey(first), "picked " + first);
            int kinds = ModRegistries.HOLO_READOUTS.size();
            for (int i = 0; i < kinds; i++) {
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            }
            helper.assertTrue(projector.display().source().equals(first), "cycling every kind did not come back round");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aReadoutOnlyResyncsWhenItsLinesChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_200, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            GameTestPack.GAUGE_READINGS.put(pos, 1);
            projector.setSource(GameTestPack.GAUGE.location());
            int syncs = projector.syncs();
            // Several refreshes go by with the ticker running: the same lines are never sent again.
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(projector.syncs() == syncs, "unchanged lines were sent " + (projector.syncs() - syncs) + " times");
                GameTestPack.GAUGE_READINGS.put(pos, 2);
                projector.refresh();
                helper.assertTrue(projector.display().texts().equals(List.of("gauge 2")), "lines " + projector.display().texts());
                helper.assertTrue(projector.syncs() == syncs + 1, "changed lines were sent " + (projector.syncs() - syncs) + " times");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theServerTickerRefreshesAReadout(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_300, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(HoloDisplay.DEFAULT_SOURCE);
            List<String> shown = projector.display().texts();
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(!projector.display().texts().equals(shown), "the ticker did not refresh the clock");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aProgramWriteReplacesAReadoutAndTheTickerLeavesItAlone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_400, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(HoloDisplay.DEFAULT_SOURCE);
            projector.setLine(0, "mine", HoloDisplay.NO_COLOR);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(projector.display().texts().equals(List.of("mine")), "lines " + projector.display().texts());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anAddonsReadoutKindShowsOnAProjector(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_500, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(GameTestPack.GAUGE.location());
            helper.assertTrue(projector.display().texts().equals(List.of("gauge 0")), "lines " + projector.display().texts());
            projector.setSource(VoidReclaimer.id("no_such_kind"));
            helper.assertTrue(projector.display().lines().isEmpty(), "an unknown kind showed " + projector.display().texts());
            helper.assertTrue(projector.display().source().equals(VoidReclaimer.id("no_such_kind")), "an unknown kind was not kept");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aReadoutSourceComposesAndFollowsItsSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_600, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            BlockPos near = pos.east(3);
            BlockPos nearer = pos.west(2);
            BlockPos far = pos.south(9);
            level.setBlock(far, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            projector.setSource(VoidReclaimer.id("backup_core"));
            helper.assertTrue(projector.display().lines().isEmpty(), "a core out of range showed " + projector.display().texts());
            level.setBlock(near, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            level.setBlock(nearer, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            ((BackupCoreBlockEntity) level.getBlockEntity(near)).setName("near");
            ((BackupCoreBlockEntity) level.getBlockEntity(nearer)).setName("nearer");
            projector.refresh();
            helper.assertTrue(projector.display().text(1).equals("nearer"), "read " + projector.display().texts());
            level.setBlock(nearer, Blocks.AIR.defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().text(1).equals("near"), "did not move on: " + projector.display().texts());
            level.setBlock(near, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(far, Blocks.AIR.defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().lines().isEmpty(), "still showing " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void playerReadoutReadsTheNearestBody(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_700, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(VoidReclaimer.id("player"));
            helper.assertTrue(projector.display().lines().isEmpty(), "no body in reach, but " + projector.display().texts());
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
            player.setHealth(5);
            projector.refresh();
            helper.assertTrue(projector.display().text(0).equals(player.getGameProfile().getName()), "lines " + projector.display().texts());
            helper.assertTrue(projector.display().text(1).startsWith("Health 5.0 /"), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDysonReadoutNeedsNoSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            DysonSphereData saved = DysonSphereData.of(level);
            Dyson.set(level.getServer(), new DysonSphereData(123, 4, 0, 0));
            projector.setSource(VoidReclaimer.id("dyson"));
            List<String> lines = projector.display().texts();
            Dyson.set(level.getServer(), saved);
            helper.assertTrue(lines.get(2).equals("Satellites 123"), "lines " + lines);
            helper.assertTrue(lines.get(3).startsWith("Trusses 4 / "), "lines " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDeviceReadoutReadsAnyTankBesideIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_900, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(VoidReclaimer.id("device"));
            helper.assertTrue(projector.display().lines().isEmpty(), "nothing beside it, but " + projector.display().texts());
            level.setBlock(pos.below(), ModBlocks.ION_THRUSTER.get().defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().text(1).startsWith("Energy 0 / "), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aCreateDisplayLinkDeliversText(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLine(1, "kept", HoloDisplay.NO_COLOR);
            // The link reads the block behind it: here the Nixie Tube below, which needs no player to set its text.
            BlockPos nixie = pos.east(2);
            BlockPos link = nixie.above();
            level.setBlock(nixie, AllBlocks.ORANGE_NIXIE_TUBE.getDefaultState(), 3);
            level.setBlock(link, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.UP), 3);
            ((NixieTubeBlockEntity) level.getBlockEntity(nixie)).displayCustomText("{\"text\":\"from create\"}", 1);
            DisplayLinkBlockEntity display = (DisplayLinkBlockEntity) level.getBlockEntity(link);
            display.target(pos);
            display.activeSource = DisplaySource.getAll(level, nixie).get(0);
            display.targetLine = 0;
            display.updateGatheredData();
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.TEXT, "mode " + projector.display().mode());
            helper.assertTrue(projector.display().text(0).startsWith("from create"), "lines " + projector.display().texts());
            helper.assertTrue(projector.display().text(1).equals("kept"), "a link wiped another line: " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aDirectAcceptTextKeepsOtherLines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_100, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLines(List.of("one", "two", "three"));
            DisplayTarget target = DisplayTarget.get(level, pos);
            helper.assertTrue(target == HoloDisplaySetup.HOLO_PROJECTOR.get(), "the projector is not a display target: " + target);
            helper.assertTrue(target.provideStats(null).maxRows() == HoloDisplay.MAX_LINES, "rows");
            helper.assertTrue(target.provideStats(null).maxColumns() == HoloDisplay.MAX_CHARS, "columns");
            DisplayLinkContext context = new DisplayLinkContext(level, null) {
                @Override
                public BlockEntity getTargetBlockEntity() {
                    return projector;
                }
            };
            target.acceptText(1, List.of(Component.literal("TWO"), Component.literal("THREE")), context);
            helper.assertTrue(projector.display().texts().equals(List.of("one", "TWO", "THREE")), "lines " + projector.display().texts());
            // Past the last line is ignored, as the link's own offset can run off the end.
            target.acceptText(7, List.of(Component.literal("eight"), Component.literal("nine")), context);
            helper.assertTrue(projector.display().text(7).equals("eight"), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    /** A computer's own block entity, as the addon places it. */
    private static ComputerBlockEntity computer(ServerLevel level, BlockPos pos) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("light_bulb_addon_create_crafts_and__addions", "computer"));
        level.setBlock(pos, block.defaultBlockState(), 3);
        return (ComputerBlockEntity) level.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void cogScriptWritesToTheProjector(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_200, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            ComputerBlockEntity computer = computer(level, pos.east(3));
            Interpreter script = new Interpreter(() -> {});
            HoloScript.install(script, computer, Supplier::get);
            String address = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            script.run("addr = holo.find()\nholo.set(addr, 2, \"from cogscript\", \"red\")\nholo.scale(addr, 2)");
            helper.assertTrue(projector.display().text(1).equals("from cogscript"), "lines " + projector.display().texts());
            helper.assertTrue(projector.display().lines().get(1).color() == DyeColor.RED.getTextColor(), "colour");
            helper.assertTrue(projector.display().scale() == 2f, "scale " + projector.display().scale());
            script.run("seen = holo.get(addr)");
            helper.assertTrue(script.globals().get("seen") instanceof Map<?, ?> seen && "text".equals(seen.get("mode")), "holo.get: " + script.globals().get("seen"));
            script.run("holo.globe(addr, \"earth\")");
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.GLOBE && HoloGlobes.EARTH.equals(projector.display().subject()), "holo.globe did not take");
            script.run("holo.map(addr)");
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.MAP, "holo.map did not take");
            script.run("holo.ship(addr)");
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.SHIP, "holo.ship did not take");
            script.run("holo.source(\"" + address + "\", \"clock\")");
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.READOUT, "the source did not take");
            // Out of reach is a script error, not a write.
            HoloProjectorBlockEntity far = place(level, pos.east(30));
            String farAddress = (pos.getX() + 30) + "," + pos.getY() + "," + pos.getZ();
            try {
                script.run("holo.clear(\"" + farAddress + "\")");
                helper.fail("a projector out of reach took a write");
            } catch (ScriptError expected) {
                helper.assertTrue(expected.getMessage().contains("no Holo Projector"), expected.getMessage());
            }
            helper.assertTrue(far.display().equals(HoloDisplay.EMPTY), "the far projector changed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theCogScriptMixinInstallsTheModule(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_300, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            ComputerOS os = new ComputerOS(computer(level, pos), new Terminal(), new HashMap<>());
            Interpreter script = new Interpreter(os);
            install(helper, os, script);
            helper.assertTrue(script.globals().get("holo") instanceof Map<?, ?>, "holo is not defined");
            helper.succeed();
        });
    }

    /** A real program runs on its computer's thread and must hop to the server's: this is the path the other tests skip. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aProgramOnItsOwnThreadReachesTheProjector(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_400, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            ComputerOS os = new ComputerOS(computer(level, pos.east(3)), new Terminal(), new HashMap<>());
            Interpreter script = new Interpreter(os);
            install(helper, os, script);
            Thread program = new Thread(() -> script.run("holo.set(holo.find(), 1, \"from its thread\")"), "holo-test-program");
            program.setDaemon(true);
            program.start();
            // The hop waits in a queue that the computer's own tick empties; here that tick is called by hand.
            try {
                for (int i = 0; i < 400 && program.isAlive(); i++) {
                    os.tick();
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                helper.fail("interrupted");
            }
            helper.assertTrue(projector.display().text(0).equals("from its thread"), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    private static void install(GameTestHelper helper, ComputerOS os, Interpreter script) {
        try {
            Method install = ComputerOS.class.getDeclaredMethod("installApi", Interpreter.class, List.class);
            install.setAccessible(true);
            install.invoke(os, script, List.of());
        } catch (ReflectiveOperationException e) {
            helper.fail("the addon's installApi moved: " + e);
        }
    }

    /** A sampler that assumes deep space would stop the server on Earth: the ticker calls it twice a second. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void everyReadoutKindReadsOnRuinedEarth(GameTestHelper helper) {
        ServerLevel earth = helper.getLevel().getServer().getLevel(RuinedEarth.DIMENSION);
        helper.assertTrue(earth != null, "no ruined Earth");
        for (var kind : ModRegistries.HOLO_READOUTS) {
            kind.lines(earth, BlockPos.ZERO);
        }
        List<String> cloud = ModRegistries.HOLO_READOUTS.get(VoidReclaimer.id("cloud")).lines(earth, BlockPos.ZERO);
        helper.assertTrue(cloud.equals(List.of("DUST CLOUD", "Clear space")), "lines " + cloud);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aModelSavesAndTheTickerLeavesItAlone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_500, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(HoloDisplay.DEFAULT_SOURCE);
            projector.setModel(VoidReclaimer.id("entity/mining_probe"));
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.MODEL, "mode " + projector.display().mode());
                helper.assertTrue(projector.display().lines().isEmpty(), "readout lines came back: " + projector.display().texts());
                HolderLookup.Provider registries = level.registryAccess();
                CompoundTag saved = projector.saveWithFullMetadata(registries);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                HoloProjectorBlockEntity restored = place(level, pos);
                restored.loadWithComponents(saved, registries);
                helper.assertTrue("void_reclaimer:entity/mining_probe".equals(restored.display().subject()), "subject " + restored.display().subject());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aGlobeFollowsTheSphereAndOnlyResyncsOnAChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_600, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            MinecraftServer server = level.getServer();
            ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
            HoloProjectorBlockEntity projector = place(level, pos);
            helper.assertTrue(!projector.setGlobe("mars"), "a globe that is not one was taken");
            DysonSphereData saved = DysonSphereData.of(level);
            SectorMap savedMap = SectorMap.of(earth);
            try {
                Dyson.set(server, new DysonSphereData(123, 4, 0, 0));
                EarthRestoration.set(earth, SectorMap.NONE.with(5).with(9));
                helper.assertTrue(projector.setGlobe(HoloGlobes.EARTH), "earth refused");
                HoloGlobes.Progress progress = projector.display().progress();
                helper.assertTrue(progress.satellites() == 123 && progress.trusses() == 4, "counters " + progress);
                helper.assertTrue(progress.isRestored(5) && progress.isRestored(9) && progress.restoredCount() == 2, "sectors " + progress);
                int syncs = projector.syncs();
                projector.refresh();
                helper.assertTrue(projector.syncs() == syncs, "unchanged progress was sent again");
                Dyson.set(server, new DysonSphereData(124, 4, 0, 0));
                EarthRestoration.set(earth, SectorMap.NONE.with(5).with(9).with(10));
                projector.refresh();
                helper.assertTrue(projector.display().progress().satellites() == 124, "a launch did not show");
                helper.assertTrue(projector.display().progress().restoredCount() == 3, "a restored sector did not show");
                helper.assertTrue(projector.syncs() == syncs + 1, "changed progress was sent " + (projector.syncs() - syncs) + " times");
                helper.succeed();
            } finally {
                Dyson.set(server, saved);
                EarthRestoration.set(earth, savedMap);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShipMiniatureIsSavedAndTheLastWriterWins(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_700, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setShip();
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.SHIP, "mode " + projector.display().mode());
            HolderLookup.Provider registries = level.registryAccess();
            CompoundTag saved = projector.saveWithFullMetadata(registries);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            HoloProjectorBlockEntity restored = place(level, pos);
            restored.loadWithComponents(saved, registries);
            helper.assertTrue(restored.display().mode() == HoloDisplay.Mode.SHIP, "mode after a load " + restored.display().mode());
            restored.setLine(0, "text", HoloDisplay.NO_COLOR);
            helper.assertTrue(restored.display().mode() == HoloDisplay.Mode.TEXT, "text did not replace the ship");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aMapShowsTheBlocksAroundItAndOnlyResyncsOnAChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            // In the projector's own chunk, which the test holds a ticket on, and west of the middle, where rounding bites. On top
            // of the column: a fresh world can put an asteroid anywhere, and a map shows what is highest.
            // At the default radius a pixel shows every second block, the odd offsets: these are -7 and +1.
            int x = (pos.getX() & ~15) + 1;
            int z = (pos.getZ() & ~15) + 1;
            BlockPos block = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
            level.setBlock(block, Blocks.RED_CONCRETE.defaultBlockState(), 3);
            projector.setMap();
            int radius = projector.mapRadius();
            int at = HoloMapImage.pixel(block.getZ() - pos.getZ(), radius) * HoloMapImage.SIZE + HoloMapImage.pixel(block.getX() - pos.getX(), radius);
            int red = MapColor.COLOR_RED.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
            helper.assertTrue((projector.mapImage()[at] & 0xFF) == red, "pixel " + (projector.mapImage()[at] & 0xFF) + ", red is " + red);
            int syncs = projector.syncs();
            projector.refreshMap();
            helper.assertTrue(projector.syncs() == syncs, "the same picture was sent again");
            level.setBlock(block, Blocks.AIR.defaultBlockState(), 3);
            projector.refreshMap();
            helper.assertTrue((projector.mapImage()[at] & 0xFF) != red, "the block is gone but its pixel stayed");
            helper.assertTrue(projector.syncs() == syncs + 1, "the new picture was not sent");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theMapGoesWithTheBlocksUpdateNotItsSave(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_900, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setMap();
            HolderLookup.Provider registries = level.registryAccess();
            helper.assertTrue(projector.getUpdateTag(registries).contains("map"), "a client would get no picture");
            helper.assertTrue(!projector.saveWithFullMetadata(registries).contains("map"), "the picture went into the save");
            projector.setLine(0, "text", HoloDisplay.NO_COLOR);
            helper.assertTrue(!projector.getUpdateTag(registries).contains("map"), "text still sends a picture");
            helper.succeed();
        });
    }

    /**
     * A map of the relays zooms out to them and shows which are lit, wherever they are, and a plain map comes back as blocks. Its own
     * batch and an empty list: the map fits every row in the level, and rows from other tests would zoom it out past what this reads.
     */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "network_map")
    public static void theNetworkMapShowsTheRelays(GameTestHelper helper) {
        BlockPos pos = new BlockPos(118_400, 80, 8);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            level.setBlock(pos, com.voidreclaimer.registry.ModBlocks.HOLO_PROJECTOR.get().defaultBlockState(), 3);
            var projector = (com.voidreclaimer.display.HoloProjectorBlockEntity) level.getBlockEntity(pos);
            level.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, com.voidreclaimer.link.RelayList.EMPTY);
            var lit = new java.util.UUID(118_400L, 1L);
            var dark = new java.util.UUID(118_400L, 2L);
            var east = net.minecraft.world.phys.Vec3.atCenterOf(pos).add(4000.0, 0.0, 0.0);
            var south = net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0.0, 0.0, 4000.0);
            BlockPos nowhere = new BlockPos(118_400, 80, 900_000);
            com.voidreclaimer.link.Relays.note(level, new com.voidreclaimer.link.RelayList.Entry(lit, nowhere, east, true, java.util.Optional.empty()));
            com.voidreclaimer.link.Relays.note(level, new com.voidreclaimer.link.RelayList.Entry(dark, nowhere, south, false, java.util.Optional.empty()));
            projector.setNetworkMap();
            int radius = projector.mapRadius();
            helper.assertTrue(radius >= 4000, "the map did not zoom out to its relays: " + radius);
            int x = com.voidreclaimer.display.HoloMapImage.pixel(4000.0, radius);
            int y = com.voidreclaimer.display.HoloMapImage.pixel(4000.0, radius);
            int middle = com.voidreclaimer.display.HoloMapImage.pixel(0.0, radius);
            byte[] image = projector.mapImage();
            helper.assertTrue((image[middle * com.voidreclaimer.display.HoloMapImage.SIZE + x] & 0xFF) == com.voidreclaimer.display.HoloProjectorBlockEntity.NETWORK_COLOURS.lit(), "no lit relay to the east");
            helper.assertTrue((image[y * com.voidreclaimer.display.HoloMapImage.SIZE + middle] & 0xFF) == com.voidreclaimer.display.HoloProjectorBlockEntity.NETWORK_COLOURS.dark(), "no dark relay to the south");
            projector.setMap();
            helper.assertTrue(projector.mapRadius() == com.voidreclaimer.Config.HOLO_MAP_RADIUS.get(), "a plain map stayed zoomed out");
            level.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, com.voidreclaimer.link.Relays.of(level).without(lit).without(dark));
            level.removeBlock(pos, false);
            helper.succeed();
        });
    }
}
