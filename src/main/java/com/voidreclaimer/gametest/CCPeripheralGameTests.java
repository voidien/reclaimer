package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.compat.cc.CCCompat;
import com.voidreclaimer.compat.cc.DysonPeripheral;
import com.voidreclaimer.compat.cc.MachinePeripheral;
import com.voidreclaimer.compat.cc.SpawnTravelPeripherals;
import com.voidreclaimer.compat.cc.SpirePeripheral;
import com.voidreclaimer.compat.cc.ThrusterPeripheral;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.dyson.DysonSphereData;
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

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theSpireIsAPeripheralAndPulsesUnderThePlayersRules(GameTestHelper helper) {
        SpirePeripheral lua = new SpirePeripheral();
        // In deep space the Spire refuses, as it does for a right-click.
        BlockPos spacePos = new BlockPos(60_200, 80, 0);
        NaniteGameTests.inLoadedArea(helper, spacePos, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            SpireBlockEntity idle = place(space, spacePos, ModBlocks.TERRAFORMING_SPIRE.get(), SpireBlockEntity.class);
            requireMethods(helper, Lookup.of(space, spacePos), "Terraforming Spire",
                    "isRunning", "getProgress", "getPulses", "getRadius", "pulse");
            Object[] refused = lua.pulse(idle).getResult();
            helper.assertTrue(Boolean.FALSE.equals(refused[0]) && "wrong_world".equals(refused[1]),
                    "pulse() in deep space: " + Arrays.toString(refused));
            helper.assertFalse(lua.isRunning(idle), "running in deep space");
            helper.assertTrue(lua.getRadius(idle) == com.voidreclaimer.Config.SPIRE_RADIUS.get(), "getRadius " + lua.getRadius(idle));
            space.setBlock(spacePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

            // On ruined Earth pulse() starts it, and a second call is refused while it runs. The block is removed in
            // the same tick, before the wave touches any ground of the persistent test world.
            ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
            BlockPos earthPos = new BlockPos(350, 200, -300);
            SpireBlockEntity spire = place(earth, earthPos, ModBlocks.TERRAFORMING_SPIRE.get(), SpireBlockEntity.class);
            try {
                Object[] started = lua.pulse(spire).getResult();
                helper.assertTrue(started.length == 1 && Boolean.TRUE.equals(started[0]), "pulse() on ruined Earth: " + Arrays.toString(started));
                helper.assertTrue(lua.isRunning(spire), "not running after pulse()");
                helper.assertTrue(lua.getProgress(spire) == 0.0, "progress before the first tick " + lua.getProgress(spire));
                Object[] busy = lua.pulse(spire).getResult();
                helper.assertTrue(Boolean.FALSE.equals(busy[0]) && "busy".equals(busy[1]), "a second pulse(): " + Arrays.toString(busy));
            } finally {
                earth.setBlock(earthPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
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
}
