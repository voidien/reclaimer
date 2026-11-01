package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.compat.cc.CCCompat;
import com.voidreclaimer.compat.cc.DysonPeripheral;
import com.voidreclaimer.compat.cc.HelmPeripheral;
import com.voidreclaimer.compat.cc.HoloPeripheral;
import com.voidreclaimer.compat.cc.MachinePeripheral;
import com.voidreclaimer.compat.cc.SpawnTravelPeripherals;
import com.voidreclaimer.compat.cc.SpirePeripheral;
import com.voidreclaimer.compat.cc.ThrusterPeripheral;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.dyson.DysonSphereData;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import com.voidreclaimer.launch.LaunchComplex;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.launch.LaunchPad;
import com.voidreclaimer.machine.CloudSiphonBlockEntity;
import com.voidreclaimer.machine.ElectrolyzerBlockEntity;
import com.voidreclaimer.machine.MassDriver;
import com.voidreclaimer.machine.MassDriverBaseBlockEntity;
import com.voidreclaimer.machine.PayloadChamberBlockEntity;
import com.voidreclaimer.machine.QuantumReceiverBlockEntity;
import com.voidreclaimer.machine.SpinnerBlockEntity;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.ship.ChemicalThrusterBlockEntity;
import com.voidreclaimer.ship.HelmBlockEntity;
import com.voidreclaimer.ship.IonThrusterBlockEntity;
import com.voidreclaimer.terraform.SpireBlockEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * CC: Tweaked peripherals (optional integration; CC is on the dev and GameTest runtime). Each test checks two things: CC
 * itself finds our functions on the block (what a computer next to it would see), and the functions return the block's
 * real state.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CCPeripheralGameTests {
    private CCPeripheralGameTests() {}

    /** What CC offers a computer for the block entity at {@code pos}: its peripheral type and method names. */
    static final class Lookup {
        final String type;
        final Set<String> methods;

        private Lookup(String type, Set<String> methods) {
            this.type = type;
            this.methods = methods;
        }

        /**
         * Asks CC's own generic peripheral provider, the one computers use. It is internal (we compile against the API
         * only), so it is reached by reflection; this is test code and fails loudly if CC changes it.
         */
        static Lookup of(ServerLevel level, BlockPos pos) {
            try {
                Class<?> provider = Class.forName("dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider");
                Object instance = provider.getConstructor().newInstance();
                Object peripheral = provider.getMethod("getPeripheral", ServerLevel.class, BlockPos.class, Direction.class, BlockEntity.class)
                        .invoke(instance, level, pos, Direction.UP, level.getBlockEntity(pos));
                if (peripheral == null) {
                    return new Lookup(null, Set.of());
                }
                var dynamic = (dan200.computercraft.api.peripheral.IDynamicPeripheral) peripheral;
                return new Lookup(dynamic.getType(), new TreeSet<>(Arrays.asList(dynamic.getMethodNames())));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("CC: Tweaked's generic peripheral provider changed", e);
            }
        }
    }

    private static <T extends BlockEntity> T place(ServerLevel level, BlockPos pos, Block block, Class<T> type) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        return type.cast(level.getBlockEntity(pos));
    }

    private static void requireMethods(GameTestHelper helper, Lookup lookup, String block, String... names) {
        helper.assertTrue(lookup.type != null, "CC offers no peripheral for the " + block);
        for (String name : names) {
            helper.assertTrue(lookup.methods.contains(name), "the " + block + " peripheral lacks " + name + ": " + lookup.methods);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void ccIsLoadedAndOurSourcesAreRegistered(GameTestHelper helper) {
        helper.assertTrue(CCCompat.isLoaded(), "CC: Tweaked is not on the GameTest runtime");
        helper.assertTrue(!CCCompat.Sources.all().isEmpty(), "no peripheral sources");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theFactoryMachinesArePeripherals(GameTestHelper helper) {
        BlockPos base = new BlockPos(60_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, base, 6, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            MachinePeripheral lua = new MachinePeripheral();

            BlockPos siphonPos = base;
            CloudSiphonBlockEntity siphon = place(level, siphonPos, ModBlocks.CLOUD_SIPHON.get(), CloudSiphonBlockEntity.class);
            int stored = siphon.energy().receiveEnergy(1234, false);
            siphon.inventory().setStackInSlot(2, new ItemStack(ModMaterials.HIGH_PURITY_GRAPHITE.get(), 5));
            requireMethods(helper, Lookup.of(level, siphonPos), "Cloud Siphon",
                    "getDensity", "getPower", "getSpeed", "getEnergy", "getEnergyCapacity", "getItems");
            helper.assertTrue(lua.getEnergy(siphon) == stored && stored == 1234, "getEnergy " + lua.getEnergy(siphon));
            helper.assertTrue(lua.getEnergyCapacity(siphon) == siphon.energy().getMaxEnergyStored(), "getEnergyCapacity");
            Map<Integer, Map<String, Object>> items = lua.getItems(siphon);
            helper.assertTrue(items.size() == 1 && items.get(3) != null && Integer.valueOf(5).equals(items.get(3).get("count"))
                    && ("void_reclaimer:" + "high_purity_graphite").equals(items.get(3).get("name")), "getItems " + items);
            helper.assertTrue(lua.getDensity(siphon) == siphon.density(), "getDensity");

            BlockPos electrolyzerPos = base.east(3);
            ElectrolyzerBlockEntity electrolyzer = place(level, electrolyzerPos, ModBlocks.ELECTROLYZER.get(), ElectrolyzerBlockEntity.class);
            electrolyzer.waterTank().fill(new FluidStack(Fluids.WATER, 700), IFluidHandler.FluidAction.EXECUTE);
            requireMethods(helper, Lookup.of(level, electrolyzerPos), "Electrolyzer", "getProgress", "getSpeed", "getTanks");
            List<Map<String, Object>> tanks = lua.getTanks(electrolyzer);
            helper.assertTrue(tanks.size() == 3 && "minecraft:water".equals(tanks.get(0).get("name"))
                    && Integer.valueOf(700).equals(tanks.get(0).get("amount")), "getTanks " + tanks);
            helper.assertTrue(lua.getProgress(electrolyzer) >= 0 && lua.getProgress(electrolyzer) <= 1, "getProgress out of range");

            BlockPos spinnerPos = base.west(3);
            SpinnerBlockEntity spinner = place(level, spinnerPos, ModBlocks.SPINNER.get(), SpinnerBlockEntity.class);
            spinner.inventory().setStackInSlot(0, new ItemStack(ModMaterials.HIGH_PURITY_GRAPHITE.get(), 3));
            requireMethods(helper, Lookup.of(level, spinnerPos), "Spinner", "getProgress", "getSpeed", "getItems");
            helper.assertTrue(Integer.valueOf(3).equals(lua.getItems(spinner).get(1).get("count")), "Spinner getItems " + lua.getItems(spinner));

            for (BlockPos pos : List.of(siphonPos, electrolyzerPos, spinnerPos)) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDysonLaunchChainIsAPeripheral(GameTestHelper helper) {
        BlockPos base = new BlockPos(60_100, 80, 0);
        NaniteGameTests.inLoadedArea(helper, base, 6, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            DysonPeripheral lua = new DysonPeripheral();
            MassDriverBaseBlockEntity driver = place(level, base, ModBlocks.KINETIC_DRIVE_BASE.get(), MassDriverBaseBlockEntity.class);
            requireMethods(helper, Lookup.of(level, base), "Kinetic Drive Base",
                    "isAssembled", "getProgress", "getLaunches", "getSpeed", "hasPayload", "getSphere");
            helper.assertFalse(lua.isAssembled(driver), "a lone base reads as assembled");
            helper.assertFalse(lua.hasPayload(driver), "a lone base has a payload");

            PayloadChamberBlockEntity chamber = place(level, MassDriver.chamberPos(base), ModBlocks.PAYLOAD_CHAMBER.get(),
                    PayloadChamberBlockEntity.class);
            level.setBlock(MassDriver.nozzlePos(base), ModBlocks.MAGNETIC_BARREL_NOZZLE.get().defaultBlockState(), 3);
            chamber.inventory().setStackInSlot(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get()));
            helper.assertTrue(lua.isAssembled(driver), "the full stack does not read as assembled");
            helper.assertTrue(lua.hasPayload(driver), "a loaded chamber does not read as loaded");
            requireMethods(helper, Lookup.of(level, MassDriver.chamberPos(base)), "Payload Chamber", "getItems");
            helper.assertTrue(lua.getItems(chamber).size() == 1, "chamber getItems " + lua.getItems(chamber));

            Map<String, Object> sphere = lua.getSphere(driver);
            DysonSphereData data = DysonSphereData.of(level);
            helper.assertTrue(Long.valueOf(data.satellites()).equals(sphere.get("satellites"))
                    && Long.valueOf(data.trusses()).equals(sphere.get("trusses"))
                    && Integer.valueOf(data.tier()).equals(sphere.get("tier")), "getSphere " + sphere);

            BlockPos receiverPos = base.east(4);
            QuantumReceiverBlockEntity receiver = place(level, receiverPos, ModBlocks.QUANTUM_RECEIVER.get(), QuantumReceiverBlockEntity.class);
            requireMethods(helper, Lookup.of(level, receiverPos), "Quantum Receiver", "getSpeed", "getStressCapacity", "getSphere");
            helper.assertTrue(lua.getSpeed(receiver) == receiver.getGeneratedSpeed(), "receiver getSpeed");
            helper.assertTrue(lua.getSphere(receiver).equals(sphere), "the receiver sees a different sphere");

            for (BlockPos pos : List.of(base, MassDriver.chamberPos(base), MassDriver.nozzlePos(base), receiverPos)) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            helper.succeed();
        });
    }

    /** The controller at chunk-local x 8 facing south keeps its 15 x 15 pad in one force-loaded chunk. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theLaunchComplexIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_904, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            DysonPeripheral lua = new DysonPeripheral();
            net.minecraft.core.Direction facing = net.minecraft.core.Direction.SOUTH;
            for (int across = -LaunchPad.HALF; across <= LaunchPad.HALF; across++) {
                for (int forward = 0; forward < LaunchPad.SIZE; forward++) {
                    level.setBlock(LaunchPad.cell(pos, facing, across, forward), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
            level.setBlock(pos, ModBlocks.LAUNCH_CONTROLLER.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing), 3);
            LaunchControllerBlockEntity controller = (LaunchControllerBlockEntity) level.getBlockEntity(pos);
            requireMethods(helper, Lookup.of(level, pos), "Launch Controller",
                    "isAssembled", "assemble", "disassemble", "getProgress", "getLaunches", "getSpeed", "hasPayload", "getItems", "getSphere");
            helper.assertTrue("ring_incomplete".equals(lua.assemble(controller)), "assemble() without a ring: " + lua.assemble(controller));
            for (BlockPos cell : LaunchPad.ring(pos, facing)) {
                level.setBlock(cell, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
            }
            helper.assertTrue("assembled".equals(lua.assemble(controller)) && lua.isAssembled(controller), "assemble() with a ring");
            helper.assertFalse(lua.hasPayload(controller), "an empty controller has a payload");
            controller.inventory().setStackInSlot(1, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 3));
            helper.assertTrue(lua.hasPayload(controller), "a loaded controller reads as empty");
            helper.assertTrue(lua.getItems(controller).size() == 1, "getItems " + lua.getItems(controller));
            helper.assertTrue(lua.getLaunches(controller) == 0 && lua.getProgress(controller) == 0.0, "fresh counters");
            Map<String, Object> sphere = lua.getSphere(controller);
            helper.assertTrue(Long.valueOf(DysonSphereData.of(level).trusses()).equals(sphere.get("trusses")), "getSphere " + sphere);
            helper.assertTrue("disassembled".equals(lua.disassemble(controller)) && !lua.isAssembled(controller), "disassemble()");
            helper.assertTrue("not_assembled".equals(lua.disassemble(controller)), "disassemble() twice");
            controller.inventory().setStackInSlot(1, ItemStack.EMPTY);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theSpireIsAPeripheralAndPulsesUnderThePlayersRules(GameTestHelper helper) {
        SpirePeripheral lua = new SpirePeripheral();
        // In deep space the Spire refuses, as it does for a right-click.
        BlockPos spacePos = new BlockPos(60_200, 80, 0);
        NaniteGameTests.inLoadedArea(helper, spacePos, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            SpireBlockEntity idle = place(space, spacePos, ModBlocks.TERRAFORMING_SPIRE.get(), SpireBlockEntity.class);
            requireMethods(helper, Lookup.of(space, spacePos), "Terraforming Spire",
                    "isRunning", "getProgress", "getPulses", "getRadius", "pulse", "isAssembled", "assemble", "disassemble", "getSpeed",
                    "getSector", "getSectors", "getRestoredCount", "isCharging", "getCharge", "getCost", "getStock");
            helper.assertFalse(lua.isAssembled(idle), "a lone Spire reads as assembled");
            helper.assertTrue("frame_incomplete".equals(lua.assemble(idle)), "assemble() without a frame: " + lua.assemble(idle));
            helper.assertTrue("not_assembled".equals(lua.disassemble(idle)), "disassemble() without a tower: " + lua.disassemble(idle));
            helper.assertTrue(lua.getSpeed(idle) == 0.0, "getSpeed " + lua.getSpeed(idle));
            Object[] refused = lua.pulse(idle).getResult();
            helper.assertTrue(Boolean.FALSE.equals(refused[0]) && "wrong_world".equals(refused[1]),
                    "pulse() in deep space: " + Arrays.toString(refused));
            helper.assertFalse(lua.isRunning(idle), "running in deep space");
            helper.assertTrue(lua.getRadius(idle) == 0, "getRadius away from the planet " + lua.getRadius(idle));
            space.setBlock(spacePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

            // On ruined Earth pulse() starts it, and a second call is refused while it runs. The block is removed in
            // the same tick, before the wave touches any ground of the persistent test world.
            ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
            BlockPos earthPos = new BlockPos(344, 200, -304);
            SpireTowerGameTests.clear(earth, earthPos);
            SpireBlockEntity spire = SpireTowerGameTests.place(earth, earthPos);
            try {
                Object[] noTower = lua.pulse(spire).getResult();
                helper.assertTrue(Boolean.FALSE.equals(noTower[0]) && "not_assembled".equals(noTower[1]), "pulse() with no tower: " + Arrays.toString(noTower));
                SpireTowerGameTests.frame(earth, earthPos, ModBlocks.SCAFFOLD.get());
                helper.assertTrue("assembled".equals(lua.assemble(spire)) && lua.isAssembled(spire), "assemble() with a frame");
                java.util.Map<String, Object> sector = lua.getSector(spire);
                helper.assertTrue(sector != null && sector.get("column").equals(4) && sector.get("row").equals(1), "getSector " + sector);
                helper.assertTrue(lua.getRadius(spire) > 128, "getRadius of a tower off the middle of its sector " + lua.getRadius(spire));
                com.voidreclaimer.earth.SectorMap map = com.voidreclaimer.earth.SectorMap.of(earth);
                helper.assertTrue(lua.getRestoredCount(idle) == map.count() && lua.getSectors(idle).size() == 16, "the planet read from deep space");
                Object[] unpaid = lua.pulse(spire).getResult();
                helper.assertTrue(Boolean.FALSE.equals(unpaid[0]) && "no_catalyst".equals(unpaid[1]), "pulse() with nothing loaded: " + Arrays.toString(unpaid));
                helper.assertTrue(lua.getCost(spire).get("water") == com.voidreclaimer.Config.SPIRE_WATER.get(), "getCost " + lua.getCost(spire));
                spire.catalyst().setStackInSlot(0, new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModMaterials.ATMOSPHERIC_CATALYST_CORE.get()));
                spire.waterTank().fill(new net.neoforged.neoforge.fluids.FluidStack(com.voidreclaimer.registry.ModFluids.PURIFIED_WATER.source().get(), 8000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                spire.slurryTank().fill(new net.neoforged.neoforge.fluids.FluidStack(com.voidreclaimer.registry.ModFluids.BIO_NUTRIENT_SLURRY.source().get(), 4000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                helper.assertTrue(lua.getStock(spire).get("catalysts") == 1 && lua.getStock(spire).get("slurry") == 4000, "getStock " + lua.getStock(spire));
                Object[] still = lua.pulse(spire).getResult();
                helper.assertTrue(Boolean.FALSE.equals(still[0]) && "no_rotation".equals(still[1]), "pulse() with no rotation: " + Arrays.toString(still));
                spire.setSpeed(64f);
                Object[] started = lua.pulse(spire).getResult();
                helper.assertTrue(started.length == 1 && Boolean.TRUE.equals(started[0]), "pulse() on ruined Earth: " + Arrays.toString(started));
                helper.assertTrue(lua.isCharging(spire) && !lua.isRunning(spire), "not charging after pulse()");
                helper.assertTrue(lua.getCharge(spire) == 0.0, "charge before the first tick " + lua.getCharge(spire));
                Object[] busy = lua.pulse(spire).getResult();
                helper.assertTrue(Boolean.FALSE.equals(busy[0]) && "busy".equals(busy[1]), "a second pulse(): " + Arrays.toString(busy));
                helper.assertTrue("disassembled".equals(lua.disassemble(spire)) && !lua.isAssembled(spire), "disassemble()");
            } finally {
                SpireTowerGameTests.clear(earth, earthPos);
            }
            helper.succeed();
        });
    }

    /** The peripheral CC finds through its block capability, for our plain blocks (no block entity). */
    private static dan200.computercraft.api.peripheral.IPeripheral capability(ServerLevel level, BlockPos pos) {
        return level.getCapability(dan200.computercraft.api.peripheral.PeripheralCapability.get(), pos, Direction.UP);
    }

    private static void requireLuaFunction(GameTestHelper helper, Class<?> type, String name) {
        boolean found = Arrays.stream(type.getMethods()).anyMatch(m -> m.getName().equals(name)
                && m.isAnnotationPresent(dan200.computercraft.api.lua.LuaFunction.class));
        helper.assertTrue(found, type.getSimpleName() + "." + name + " is not a Lua function");
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theSpawnAndTravelBlocksArePeripherals(GameTestHelper helper) {
        BlockPos base = new BlockPos(60_300, 80, 0);
        NaniteGameTests.inLoadedArea(helper, base, 8, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            var coreLua = new SpawnTravelPeripherals.BackupCore();

            // Backup Core (block entity: a generic peripheral).
            BackupCoreBlockEntity core = place(level, base, ModBlocks.BACKUP_CORE.get(), BackupCoreBlockEntity.class);
            core.setName("Home");
            core.setCharges(2);
            requireMethods(helper, Lookup.of(level, base), "Backup Core", "getName", "getOwner", "getCharges", "getMaxCharges", "getSunProgress");
            helper.assertTrue("Home".equals(coreLua.getName(core)) && coreLua.getCharges(core) == 2
                    && coreLua.getMaxCharges(core) == com.voidreclaimer.core.CoreCharging.MAX_CHARGES, "Backup Core values");
            helper.assertTrue(coreLua.getOwner(core) == null, "an unplaced core has an owner");

            // Tether Anchor (plain block: a capability peripheral).
            BlockPos anchorPos = base.east(3);
            level.setBlock(anchorPos, ModBlocks.TETHER_ANCHOR.get().defaultBlockState(), 3);
            var anchor = capability(level, anchorPos);
            helper.assertTrue(anchor instanceof SpawnTravelPeripherals.TetherAnchor && "void_reclaimer:tether_anchor".equals(anchor.getType()),
                    "CC finds no Tether Anchor peripheral: " + anchor);
            requireLuaFunction(helper, SpawnTravelPeripherals.TetherAnchor.class, "getTethered");
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, anchorPos.getX() + 0.5, anchorPos.getY() + 1, anchorPos.getZ() + 2.5);
            try {
                helper.assertTrue(((SpawnTravelPeripherals.TetherAnchor) anchor).getTethered().isEmpty(), "someone is tethered already");
                com.voidreclaimer.tether.Tether.toggleClip(player, anchorPos);
                helper.assertTrue(((SpawnTravelPeripherals.TetherAnchor) anchor).getTethered().contains(player.getGameProfile().getName()),
                        "the clipped player is not listed: " + ((SpawnTravelPeripherals.TetherAnchor) anchor).getTethered());
            } finally {
                TestPlayers.remove(player);
            }

            // Atmospheric Portal Frame (plain block): nil alone, then a 3 x 3 frame, unlit and lit.
            BlockPos origin = base.west(6);
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, 0), origin.offset(3, 3, 0))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            BlockPos bottom = origin.below();
            level.setBlock(bottom, ModBlocks.PORTAL_FRAME.get().defaultBlockState(), 3);
            var frame = capability(level, bottom);
            helper.assertTrue(frame instanceof SpawnTravelPeripherals.PortalFrame, "CC finds no portal frame peripheral: " + frame);
            requireLuaFunction(helper, SpawnTravelPeripherals.PortalFrame.class, "getPortal");
            helper.assertTrue(((SpawnTravelPeripherals.PortalFrame) frame).getPortal() == null, "a lone frame block reports a portal");
            for (int i = 0; i < 3; i++) {
                for (BlockPos pos : List.of(origin.east(i).below(), origin.east(i).above(3), origin.west().above(i), origin.east(3).above(i))) {
                    level.setBlock(pos, ModBlocks.PORTAL_FRAME.get().defaultBlockState(), 3);
                }
            }
            Map<String, Object> unlit = ((SpawnTravelPeripherals.PortalFrame) frame).getPortal();
            helper.assertTrue(unlit != null && Boolean.FALSE.equals(unlit.get("lit")) && Integer.valueOf(3).equals(unlit.get("width"))
                    && Integer.valueOf(3).equals(unlit.get("height")) && "x".equals(unlit.get("axis")), "unlit frame " + unlit);
            helper.assertTrue(com.voidreclaimer.earth.AtmosphericPortals.tryLight(level, bottom), "the test frame did not light");
            helper.assertTrue(Boolean.TRUE.equals(((SpawnTravelPeripherals.PortalFrame) frame).getPortal().get("lit")), "a lit frame reads as unlit");

            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, 0), origin.offset(3, 3, 0))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(anchorPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theChemicalThrusterIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_400, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            ThrusterPeripheral lua = new ThrusterPeripheral();
            ChemicalThrusterBlockEntity thruster = place(level, pos, ModBlocks.CHEMICAL_THRUSTER.get(), ChemicalThrusterBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Chemical Thruster", "getThrottle", "getForce", "isFiring", "getSpeed", "getTanks");
            thruster.tank().fill(new FluidStack(com.voidreclaimer.registry.ModFluids.NEBULITE_PROPELLANT.source().get(), 500), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(lua.getThrottle(thruster) == 0.0 && !lua.isFiring(thruster), "idle thruster reads as open");
            helper.assertTrue(Integer.valueOf(500).equals(lua.getTanks(thruster).get(0).get("amount")), "getTanks " + lua.getTanks(thruster));
            level.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            helper.assertTrue(lua.getThrottle(thruster) == 1.0, "getThrottle with a redstone block: " + lua.getThrottle(thruster));
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theIonThrusterIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_500, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            ThrusterPeripheral lua = new ThrusterPeripheral();
            IonThrusterBlockEntity thruster = place(level, pos, ModBlocks.ION_THRUSTER.get(), IonThrusterBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Ion Thruster", "getThrottle", "getForce", "isFiring", "getEnergy", "getEnergyCapacity");
            thruster.energy().receiveEnergy(500, false);
            helper.assertTrue(lua.getEnergy(thruster) == 500 && lua.getEnergyCapacity(thruster) == com.voidreclaimer.Config.ION_FE_BUFFER.get(),
                    "energy " + lua.getEnergy(thruster));
            helper.assertTrue(lua.getThrottle(thruster) == 0.0 && !lua.isFiring(thruster), "idle thruster reads as open");
            // The RCS Thruster shares the functions through their common base class.
            BlockPos rcs = pos.east(2);
            level.setBlock(rcs, ModBlocks.RCS_THRUSTER.get().defaultBlockState(), 3);
            requireMethods(helper, Lookup.of(level, rcs), "RCS Thruster", "getThrottle", "getForce", "isFiring", "getEnergy", "getEnergyCapacity");
            level.setBlock(rcs, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theUplinkSeatIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(61_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            var lua = new com.voidreclaimer.compat.cc.UplinkSeatPeripheral();
            var seat = place(level, pos, ModBlocks.UPLINK_SEAT.get(), com.voidreclaimer.link.UplinkSeatBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Uplink Seat", "getSeatId", "getRange", "isOccupied", "getOccupant");
            helper.assertTrue(lua.getRange(seat) >= com.voidreclaimer.Config.LINK_SEAT_RANGE.get(), "the seat reaches " + lua.getRange(seat));
            helper.assertTrue(seat.id().toString().equals(lua.getSeatId(seat)), "the seat's id");
            helper.assertTrue(!lua.isOccupied(seat) && "".equals(lua.getOccupant(seat)), "a fresh seat reads as taken");
            level.removeBlock(pos, false);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDockingClampIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            var lua = new com.voidreclaimer.compat.cc.DockingClampPeripheral();
            var clamp = place(level, pos, ModBlocks.DOCKING_CLAMP.get(), com.voidreclaimer.station.DockingClampBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Docking Clamp", "isDocked", "getDockedName", "getStatus", "dock", "release");
            helper.assertTrue(!lua.isDocked(clamp) && "".equals(lua.getDockedName(clamp)) && "idle".equals(lua.getStatus(clamp)), "a fresh clamp reads wrongly");
            helper.assertTrue("not_on_ship".equals(lua.dock(clamp)), "docking in the world: " + lua.getStatus(clamp));
            helper.assertTrue("empty".equals(lua.release(clamp)), "releasing nothing: " + lua.getStatus(clamp));
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theStationCoreIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_700, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            var lua = new com.voidreclaimer.compat.cc.StationPeripheral();
            var core = place(level, pos, ModBlocks.STATION_CORE.get(), com.voidreclaimer.station.StationCoreBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Station Core", "getName", "setName", "isAssembled", "assemble", "disassemble", "getPosition", "getFilledSlots");
            lua.setName(core, "Relay Nine");
            helper.assertTrue("Relay Nine".equals(lua.getName(core)) && !lua.isAssembled(core) && lua.getFilledSlots(core) == 0, "a fresh core reads wrongly");
            helper.assertTrue(lua.getPosition(core).get("x") == pos.getX() + 0.5, "position " + lua.getPosition(core));
            helper.assertTrue("nothing".equals(lua.assemble(core)), "assembling a lone core: " + lua.assemble(core));
            helper.assertTrue("not_assembled".equals(lua.disassemble(core)), "disassembling a core in the world: " + lua.disassemble(core));
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theHoloProjectorIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_900, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloPeripheral lua = new HoloPeripheral();
            HoloProjectorBlockEntity holo = HoloGameTests.place(level, pos);
            requireMethods(helper, Lookup.of(level, pos), "Holo Projector", "getLines", "getLine", "setLine", "setLines", "clear", "setColor",
                    "getScale", "setScale", "getHeight", "setHeight", "setBillboard", "setModel", "setBody", "setGlobe", "setShip", "getModel", "getMode", "getSource", "setSource", "getSources", "getSize");
            try {
                Object[] written = lua.setLine(holo, new ObjectArguments(2.0, "two", "red")).getResult();
                helper.assertTrue(Boolean.TRUE.equals(written[0]), "setLine refused");
                helper.assertTrue("two".equals(lua.getLine(holo, 2)), "lines count from 1 in Lua: " + lua.getLines(holo));
                helper.assertTrue(holo.display().lines().get(1).color() == DyeColor.RED.getTextColor(), "the dye name was not used");
                Object[] past = lua.setLine(holo, new ObjectArguments(9.0, "no")).getResult();
                helper.assertTrue(Boolean.FALSE.equals(past[0]) && "no_such_line".equals(past[1]), "line 9 " + Arrays.toString(past));
                lua.setColor(holo, new ObjectArguments("#123456"));
                helper.assertTrue(holo.display().color() == 0x123456, "setColor " + holo.display().color());
                boolean refused = false;
                try {
                    lua.setColor(holo, new ObjectArguments("plaid"));
                } catch (LuaException e) {
                    refused = true;
                }
                helper.assertTrue(refused, "setColor took a colour that is none");
            } catch (LuaException e) {
                throw new IllegalStateException(e);
            }
            helper.assertTrue(lua.setModel(holo, "entity/mining_probe"), "setModel refused a good id");
            helper.assertTrue("void_reclaimer:entity/mining_probe".equals(lua.getModel(holo)), "a bare model id is ours: " + lua.getModel(holo));
            helper.assertTrue("model".equals(lua.getMode(holo)), "getMode " + lua.getMode(holo));
            helper.assertTrue(!lua.setModel(holo, "Not An Id"), "setModel took a malformed id");
            helper.assertTrue(Boolean.TRUE.equals(lua.setGlobe(holo, "earth").getResult()[0]), "setGlobe refused earth");
            Object[] mars = lua.setGlobe(holo, "mars").getResult();
            helper.assertTrue(Boolean.FALSE.equals(mars[0]) && "unknown_globe".equals(mars[1]), "setGlobe " + Arrays.toString(mars));
            helper.assertTrue("globe".equals(lua.getMode(holo)), "getMode " + lua.getMode(holo));
            lua.setShip(holo);
            helper.assertTrue("ship".equals(lua.getMode(holo)), "getMode " + lua.getMode(holo));
            lua.setBody(holo);
            helper.assertTrue("body".equals(lua.getMode(holo)), "getMode " + lua.getMode(holo));
            helper.assertTrue(lua.getModel(holo) == null, "a body has no model id");
            lua.setLines(holo, Map.of(1.0, "a", 2.0, "b"));
            helper.assertTrue(lua.getLines(holo).equals(List.of("a", "b")), "setLines " + lua.getLines(holo));
            lua.clear(holo);
            helper.assertTrue(lua.getLines(holo).isEmpty(), "clear " + lua.getLines(holo));
            lua.setScale(holo, 99);
            helper.assertTrue(lua.getScale(holo) == HoloDisplay.MAX_SCALE, "setScale " + lua.getScale(holo));
            lua.setHeight(holo, 2);
            helper.assertTrue(lua.getHeight(holo) == 2, "setHeight " + lua.getHeight(holo));
            lua.setBillboard(holo, false);
            helper.assertTrue(!holo.display().billboard(), "setBillboard");
            helper.assertTrue("text".equals(lua.getMode(holo)), "getMode " + lua.getMode(holo));
            helper.assertTrue(lua.getSources(holo).contains("void_reclaimer:clock"), "getSources " + lua.getSources(holo));
            Object[] unknown = lua.setSource(holo, "void_reclaimer:nope").getResult();
            helper.assertTrue(Boolean.FALSE.equals(unknown[0]) && "text".equals(lua.getMode(holo)), "an unknown source was taken");
            Object[] known = lua.setSource(holo, "clock").getResult();
            helper.assertTrue(Boolean.TRUE.equals(known[0]) && "readout".equals(lua.getMode(holo)), "a bare id means our namespace");
            helper.assertTrue("void_reclaimer:clock".equals(lua.getSource(holo)), "getSource " + lua.getSource(holo));
            helper.assertTrue(!lua.getLines(holo).isEmpty(), "the readout composed nothing");
            Object[] size = lua.getSize(holo).getResult();
            helper.assertTrue(Integer.valueOf(HoloDisplay.MAX_LINES).equals(size[0]) && Integer.valueOf(HoloDisplay.MAX_CHARS).equals(size[1]), "getSize");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theHelmIsAPeripheral(GameTestHelper helper) {
        BlockPos pos = new BlockPos(60_600, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HelmPeripheral lua = new HelmPeripheral();
            HelmBlockEntity helm = place(level, pos, ModBlocks.HELM.get(), HelmBlockEntity.class);
            requireMethods(helper, Lookup.of(level, pos), "Helm", "isOnShip", "getPilot", "getThrusterCount", "getTranslation", "getRotation", "getSupplies",
                    "getVelocity", "getSpin", "getAttitude");
            helper.assertTrue(!lua.isOnShip(helm) && lua.getPilot(helm) == null && lua.getThrusterCount(helm) == 0, "a Helm in the world reads as a ship");
            helper.assertTrue(lua.getTranslation(helm).size() == 6 && lua.getRotation(helm).size() == 6, "six axes each way");
            helper.assertTrue(lua.getVelocity(helm).get("speed") == 0.0 && lua.getSpin(helm).size() == 3 && lua.getAttitude(helm).get("heading") == 0.0,
                    "a Helm in the world is still and level");
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
    }
}
