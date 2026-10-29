package com.voidreclaimer.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingTypeRegistry;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.machine.ElectrolyzerBlockEntity;
import com.voidreclaimer.machine.MachineSetup;
import com.voidreclaimer.machine.SpinnerBlockEntity;
import com.voidreclaimer.machine.SpoutCasting;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Phase 7 part 2: the Electrolyzer, the Spinner, fan freezing and spout casting. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
    private MachineGameTests() {}

    private static final BlockPos AREA = new BlockPos(86_000, 80, 0);
    /** Create's own tick eats a tick or two around a speed change, so a cycle test runs a few ticks over (exact timing is in MachineMathTest). */
    private static final int SLACK = 4;

    private interface Test {
        void run(ServerLevel level, BlockPos pos);
    }

    private static void inArea(GameTestHelper helper, int offset, Test test) {
        BlockPos pos = AREA.offset(offset * 20, 0, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 6, () -> test.run(TestPlayers.deepSpace(helper), pos));
    }

    /** Create's first tick attaches the block to the kinetic network, which (with no source) resets its speed: settle it first. */
    private static void settle(com.simibubi.create.content.kinetics.base.KineticBlockEntity machine) {
        machine.tick();
    }

    /**
     * Ticks a machine turning at {@code rpm}. A real source keeps the speed up; here the test does, because Create's periodic
     * validation resets the speed of a block that has no source.
     */
    private static void tick(com.simibubi.create.content.kinetics.base.KineticBlockEntity machine, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            machine.setSpeed(rpm);
            machine.tick();
        }
    }

    // ---- Electrolyzer ------------------------------------------------------------------------------------

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void electrolyzerSplitsWaterIntoGases(GameTestHelper helper) {
        inArea(helper, 0, (level, pos) -> {
            level.setBlock(pos, ModBlocks.ELECTROLYZER.get().defaultBlockState(), 3);
            ElectrolyzerBlockEntity machine = (ElectrolyzerBlockEntity) level.getBlockEntity(pos);
            settle(machine);
            IFluidHandler pipes = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            helper.assertTrue(pipes != null, "no fluid handler for pipes");
            int water = Config.ELECTROLYZER_WATER.get();
            helper.assertTrue(pipes.fill(new FluidStack(Fluids.WATER, 2 * water), IFluidHandler.FluidAction.EXECUTE) == 2 * water, "water refused");
            helper.assertTrue(pipes.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lava accepted");

            // Too slow: nothing happens.
            tick(machine, Config.MACHINE_MIN_RPM.get() - 1, 200);
            helper.assertTrue(machine.waterTank().getFluidAmount() == 2 * water, "ran below the minimum speed");

            tick(machine, 64f, Config.MACHINE_CYCLE_TICKS.get() + SLACK);
            helper.assertTrue(machine.waterTank().getFluidAmount() == water, "one cycle should use " + water + " water, left " + machine.waterTank().getFluidAmount());
            helper.assertTrue(machine.oxygenTank().getFluidAmount() == Config.ELECTROLYZER_OXYGEN.get(), "oxygen " + machine.oxygenTank().getFluidAmount());
            helper.assertTrue(machine.hydrogenTank().getFluidAmount() == Config.ELECTROLYZER_HYDROGEN.get(), "hydrogen " + machine.hydrogenTank().getFluidAmount());
            helper.assertTrue(machine.oxygenTank().getFluid().is(ModFluids.OXYGEN_GAS.source().get()), "wrong gas in the oxygen tank");

            // Twice as fast: a cycle in half the time.
            tick(machine, 128f, Config.MACHINE_CYCLE_TICKS.get() / 2 + SLACK);
            helper.assertTrue(machine.waterTank().getFluidAmount() == 0, "double speed should finish a cycle in half the ticks");

            // Pipes can't take water back out, only gas.
            helper.assertTrue(pipes.drain(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water drained");
            FluidStack gas = pipes.drain(10_000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(gas.is(ModFluids.HYDROGEN_GAS.source().get()) && gas.getAmount() == 2 * Config.ELECTROLYZER_HYDROGEN.get(), "drained " + gas);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void electrolyzerStopsWhenOutputsAreFullOrSwitchedOff(GameTestHelper helper) {
        inArea(helper, 1, (level, pos) -> {
            level.setBlock(pos, ModBlocks.ELECTROLYZER.get().defaultBlockState(), 3);
            ElectrolyzerBlockEntity machine = (ElectrolyzerBlockEntity) level.getBlockEntity(pos);
            settle(machine);
            machine.waterTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            machine.hydrogenTank().fill(new FluidStack(ModFluids.HYDROGEN_GAS.source().get(), machine.hydrogenTank().getCapacity()),
                    IFluidHandler.FluidAction.EXECUTE);
            tick(machine, 64f, 300);
            helper.assertTrue(machine.waterTank().getFluidAmount() == 1000, "ran with a full hydrogen tank");

            machine.hydrogenTank().drain(10_000, IFluidHandler.FluidAction.EXECUTE);
            var toggle = Config.TOGGLES.get("electrolyzer");
            try {
                toggle.set(false);
                tick(machine, 64f, 300);
                helper.assertTrue(machine.waterTank().getFluidAmount() == 1000, "ran while switched off");
            } finally {
                toggle.set(true);
            }
            tick(machine, 64f, Config.MACHINE_CYCLE_TICKS.get() + SLACK);
            helper.assertTrue(machine.waterTank().getFluidAmount() < 1000, "did not resume when switched back on");
            helper.succeed();
        });
    }

    // ---- Spinner -----------------------------------------------------------------------------------------

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spinnerSpinsGraphiteIntoThread(GameTestHelper helper) {
        inArea(helper, 2, (level, pos) -> {
            level.setBlock(pos, ModBlocks.SPINNER.get().defaultBlockState(), 3);
            SpinnerBlockEntity machine = (SpinnerBlockEntity) level.getBlockEntity(pos);
            settle(machine);
            IItemHandler automation = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            helper.assertTrue(automation != null, "no item handler");
            helper.assertTrue(!automation.insertItem(0, new ItemStack(Items.STONE), false).isEmpty(), "stone accepted");
            helper.assertTrue(!automation.insertItem(1, new ItemStack(ModMaterials.HIGH_PURITY_GRAPHITE.get()), false).isEmpty(), "graphite accepted into the output");
            helper.assertTrue(automation.insertItem(0, new ItemStack(ModMaterials.HIGH_PURITY_GRAPHITE.get(), 3), false).isEmpty(), "graphite refused");
            helper.assertTrue(automation.extractItem(0, 1, false).isEmpty(), "input slot gave items out");

            tick(machine, 8f, 300);
            helper.assertTrue(machine.inventory().getStackInSlot(1).isEmpty(), "spun below the minimum speed");

            tick(machine, 64f, Config.MACHINE_CYCLE_TICKS.get() + SLACK);
            ItemStack out = automation.getStackInSlot(1);
            helper.assertTrue(out.is(ModMaterials.CARBON_THREAD.get()) && out.getCount() == Config.SPINNER_THREADS.get(), "output " + out);
            helper.assertTrue(automation.getStackInSlot(0).getCount() == 2, "one graphite should be used per cycle");
            helper.assertTrue(automation.extractItem(1, 64, false).getCount() == Config.SPINNER_THREADS.get(), "thread not extractable");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spinnerWaitsWhenItsOutputIsFull(GameTestHelper helper) {
        inArea(helper, 3, (level, pos) -> {
            level.setBlock(pos, ModBlocks.SPINNER.get().defaultBlockState(), 3);
            SpinnerBlockEntity machine = (SpinnerBlockEntity) level.getBlockEntity(pos);
            settle(machine);
            machine.inventory().setStackInSlot(0, new ItemStack(ModMaterials.HIGH_PURITY_GRAPHITE.get(), 5));
            machine.inventory().setStackInSlot(1, new ItemStack(ModMaterials.CARBON_THREAD.get(), 64));
            tick(machine, 64f, 300);
            helper.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 5, "used graphite with a full output");
            helper.succeed();
        });
    }

    // ---- Stress ------------------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void machinesRegisterTheirStressImpact(GameTestHelper helper) {
        helper.assertTrue(BlockStressValues.getImpact(ModBlocks.ELECTROLYZER.get()) == Config.ELECTROLYZER_STRESS.get(),
                "electrolyzer impact " + BlockStressValues.getImpact(ModBlocks.ELECTROLYZER.get()));
        helper.assertTrue(BlockStressValues.getImpact(ModBlocks.SPINNER.get()) == Config.SPINNER_STRESS.get(),
                "spinner impact " + BlockStressValues.getImpact(ModBlocks.SPINNER.get()));
        helper.succeed();
    }

    // ---- Fan freezing ------------------------------------------------------------------------------------

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void freezingFanTypeIsRegisteredAndFreezesWaterBuckets(GameTestHelper helper) {
        inArea(helper, 4, (level, pos) -> {
            FanProcessingType freezing = MachineSetup.FREEZING.get();
            helper.assertTrue(CreateBuiltInRegistries.FAN_PROCESSING_TYPE.containsKey(VoidReclaimer.id("freezing")), "not in Create's registry");
            helper.assertTrue(FanProcessingTypeRegistry.SORTED_TYPES_VIEW.contains(freezing), "not in the fan's sorted types");

            helper.assertFalse(freezing.isValidAt(level, pos), "valid in air");
            level.setBlock(pos, Blocks.POWDER_SNOW.defaultBlockState(), 3);
            helper.assertTrue(freezing.isValidAt(level, pos), "not valid at powder snow");
            helper.assertTrue(FanProcessingType.getAt(level, pos) == freezing, "the fan would pick " + FanProcessingType.getAt(level, pos));

            helper.assertTrue(freezing.canProcess(new ItemStack(Items.WATER_BUCKET), level), "water bucket not processable");
            helper.assertFalse(freezing.canProcess(new ItemStack(Items.STONE), level), "stone processable");
            List<ItemStack> out = freezing.process(new ItemStack(Items.WATER_BUCKET), level);
            helper.assertTrue(out != null && out.size() == 2 && out.get(0).is(Items.PACKED_ICE) && out.get(1).is(Items.BUCKET), "results " + out);

            var toggle = Config.TOGGLES.get("fanFreezing");
            try {
                toggle.set(false);
                helper.assertFalse(freezing.isValidAt(level, pos), "valid while switched off");
                helper.assertFalse(freezing.canProcess(new ItemStack(Items.WATER_BUCKET), level), "processable while switched off");
            } finally {
                toggle.set(true);
            }
            helper.succeed();
        });
    }

    // ---- Spout casting -----------------------------------------------------------------------------------

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void spoutCastsLiquidSiliconOntoAnEmptyDepot(GameTestHelper helper) {
        inArea(helper, 5, (level, pos) -> {
            level.setBlock(pos, AllBlocks.DEPOT.get().defaultBlockState(), 3);
            BlockSpoutingBehaviour behaviour = BlockSpoutingBehaviour.get(level, pos);
            helper.assertTrue(behaviour instanceof SpoutCasting, "depot has " + behaviour);
            DepotBlockEntity depot = (DepotBlockEntity) level.getBlockEntity(pos);
            int amount = Config.CASTING_AMOUNT.get();
            FluidStack silicon = new FluidStack(ModFluids.LIQUID_SILICON.source().get(), amount);

            helper.assertTrue(behaviour.fillBlock(level, pos, null, new FluidStack(Fluids.WATER, 1000), false) == 0, "cast from water");
            helper.assertTrue(behaviour.fillBlock(level, pos, null, silicon.copyWithAmount(amount - 1), false) == 0, "cast with too little silicon");
            helper.assertTrue(behaviour.fillBlock(level, pos, null, silicon, true) == amount, "simulation should report the amount");
            helper.assertTrue(depot.getHeldItem().isEmpty(), "simulation changed the depot");
            helper.assertTrue(behaviour.fillBlock(level, pos, null, silicon, false) == amount, "cast failed");
            helper.assertTrue(depot.getHeldItem().is(ModMaterials.QUARTZ_WAFER.get()), "depot holds " + depot.getHeldItem());

            helper.assertTrue(behaviour.fillBlock(level, pos, null, silicon, false) == 0, "cast onto an occupied depot");
            var toggle = Config.TOGGLES.get("spoutCasting");
            try {
                depot.setHeldItem(ItemStack.EMPTY);
                toggle.set(false);
                helper.assertTrue(behaviour.fillBlock(level, pos, null, silicon, false) == 0, "cast while switched off");
            } finally {
                toggle.set(true);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void machineRecipesLoad(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("replicator/electrolyzer", "replicator/spinner", "freezing/water_bucket")) {
            helper.assertTrue(manager.byKey(VoidReclaimer.id(name)).isPresent(), "recipe " + name + " did not load");
        }
        ResourceLocation id = VoidReclaimer.id("freezing/water_bucket");
        helper.assertTrue(BuiltInRegistriesRecipeType.is(manager.byKey(id).orElseThrow().value().getType()), "wrong recipe type");
        helper.succeed();
    }

    /** Small helper so the test reads clearly. */
    private static final class BuiltInRegistriesRecipeType {
        static boolean is(net.minecraft.world.item.crafting.RecipeType<?> type) {
            return net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(type).equals(VoidReclaimer.id("freezing"));
        }
    }
}
