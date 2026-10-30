package com.voidreclaimer.gametest;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.ship.ThrusterFuel;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataMaps;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.ship.ChemicalThrusterBlockEntity;
import com.voidreclaimer.ship.ElectricThrusterBlockEntity;
import com.voidreclaimer.ship.IonThrusterBlockEntity;
import com.voidreclaimer.ship.RcsThrusterBlockEntity;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The thrusters as blocks: its tank, fuel burn and force. That it really moves a ship is in {@link AeronauticsGameTests}. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThrusterGameTests {
    private static final float FULL_SPIN_RPM = 64f;

    private ThrusterGameTests() {}

    private static void withThruster(GameTestHelper helper, BlockPos pos, Consumer<ChemicalThrusterBlockEntity> body) {
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            level.setBlock(pos, ModBlocks.CHEMICAL_THRUSTER.get().defaultBlockState(), 3);
            ChemicalThrusterBlockEntity thruster = (ChemicalThrusterBlockEntity) level.getBlockEntity(pos);
            thruster.tick(); // Create attaches the block to the kinetic network on its first tick, which resets the speed
            try {
                body.accept(thruster);
                helper.succeed();
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    /** Create resets the speed of a block with no source, so it is set before every tick. */
    private static void tick(KineticBlockEntity machine, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            machine.setSpeed(rpm);
            machine.tick();
        }
    }

    private static void fuel(ChemicalThrusterBlockEntity thruster, int amount) {
        thruster.tank().fill(new FluidStack(ModFluids.NEBULITE_PROPELLANT.source().get(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private static void openThrottle(ChemicalThrusterBlockEntity thruster) {
        thruster.getLevel().setBlock(thruster.getBlockPos().above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void onlyFuelsGoInTheTank(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_000, 80, 0), thruster -> {
            helper.assertTrue(thruster.fluids().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water went in");
            int filled = thruster.fluids().fill(new FluidStack(ModFluids.NEBULITE_PROPELLANT.source().get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "Nebulite Propellant did not go in: " + filled);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aFullThrottleBurnsFuelAndPushes(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_100, 80, 0), thruster -> {
            fuel(thruster, 1000);
            openThrottle(thruster);
            tick(thruster, FULL_SPIN_RPM, 100);
            helper.assertTrue(Math.abs(thruster.force() - Config.CHEMICAL_THRUST.get()) < 1e-6, "full force expected, got " + thruster.force());
            // 2 mB a tick; the kinetic network can take a tick to pick up the speed.
            int left = thruster.tank().getFluidAmount();
            helper.assertTrue(left >= 800 && left <= 802, "2 mB a tick for 100 ticks, left " + left);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void noThrottleMeansNoForceAndNoBurn(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_200, 80, 0), thruster -> {
            fuel(thruster, 1000);
            tick(thruster, FULL_SPIN_RPM, 50);
            helper.assertTrue(thruster.force() == 0.0, "force without redstone " + thruster.force());
            helper.assertTrue(thruster.tank().getFluidAmount() == 1000, "burned without redstone");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShaftBelowTheMinimumSpeedDoesNothing(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_300, 80, 0), thruster -> {
            fuel(thruster, 1000);
            openThrottle(thruster);
            tick(thruster, Config.MACHINE_MIN_RPM.get() - 1, 50);
            helper.assertTrue(thruster.force() == 0.0, "force below the minimum speed " + thruster.force());
            helper.assertTrue(thruster.tank().getFluidAmount() == 1000, "burned below the minimum speed");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aDryTankStopsThePush(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_400, 80, 0), thruster -> {
            fuel(thruster, 10);
            openThrottle(thruster);
            tick(thruster, FULL_SPIN_RPM, 2);
            helper.assertTrue(thruster.force() > 0.0, "not firing with fuel in the tank");
            tick(thruster, FULL_SPIN_RPM, 20);
            helper.assertTrue(thruster.tank().isEmpty(), "fuel left: " + thruster.tank().getFluidAmount());
            helper.assertTrue(thruster.force() == 0.0, "still pushing on an empty tank: " + thruster.force());
        });
    }

    /** An Ion Thruster at {@code pos}, ticked by hand through its server tick. */
    private static void withIon(GameTestHelper helper, BlockPos pos, Consumer<IonThrusterBlockEntity> body) {
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            level.setBlock(pos, ModBlocks.ION_THRUSTER.get().defaultBlockState(), 3);
            try {
                body.accept((IonThrusterBlockEntity) level.getBlockEntity(pos));
                helper.succeed();
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    private static void tickIon(IonThrusterBlockEntity thruster, int ticks) {
        for (int i = 0; i < ticks; i++) {
            IonThrusterBlockEntity.serverTick(thruster.getLevel(), thruster.getBlockPos(), thruster.getBlockState(), thruster);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theIonBufferOnlyTakesEnergyIn(GameTestHelper helper) {
        withIon(helper, new BlockPos(61_500, 80, 0), thruster -> {
            int buffer = Config.ION_FE_BUFFER.get();
            helper.assertTrue(thruster.energy().receiveEnergy(Integer.MAX_VALUE, false) == buffer, "the buffer is not bounded");
            helper.assertTrue(thruster.energy().receiveEnergy(1, false) == 0, "a full buffer took more");
            helper.assertTrue(thruster.energy().extractEnergy(100, false) == 0 && !thruster.energy().canExtract(), "energy came back out");
            helper.assertTrue(thruster.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, thruster.getBlockPos(), null) != null,
                    "energy mods cannot reach it");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anOpenIonThrusterDrawsEnergyAndPushes(GameTestHelper helper) {
        withIon(helper, new BlockPos(61_600, 80, 0), thruster -> {
            thruster.energy().receiveEnergy(1000, false);
            tickIon(thruster, 10);
            helper.assertTrue(thruster.force() == 0.0 && thruster.storedEnergy() == 1000, "it ran with the throttle shut");
            thruster.getLevel().setBlock(thruster.getBlockPos().above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            tickIon(thruster, 10);
            helper.assertTrue(Math.abs(thruster.force() - Config.ION_THRUST.get()) < 1e-6, "full force expected, got " + thruster.force());
            helper.assertTrue(thruster.storedEnergy() == 1000 - 10 * Config.ION_FE_PER_TICK.get(), "energy left " + thruster.storedEnergy());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anEmptyIonThrusterStopsPushing(GameTestHelper helper) {
        withIon(helper, new BlockPos(61_700, 80, 0), thruster -> {
            thruster.energy().receiveEnergy(100, false);
            thruster.getLevel().setBlock(thruster.getBlockPos().above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            tickIon(thruster, 1);
            helper.assertTrue(thruster.force() > 0.0, "not firing on a charged buffer");
            tickIon(thruster, 5);
            helper.assertTrue(thruster.storedEnergy() == 0 && thruster.force() == 0.0, "still pushing on an empty buffer: " + thruster.force());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theRcsThrusterIsAWeakerElectricThruster(GameTestHelper helper) {
        BlockPos pos = new BlockPos(61_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            level.setBlock(pos, ModBlocks.RCS_THRUSTER.get().defaultBlockState(), 3);
            level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            RcsThrusterBlockEntity thruster = (RcsThrusterBlockEntity) level.getBlockEntity(pos);
            try {
                thruster.energy().receiveEnergy(1000, false);
                ElectricThrusterBlockEntity.serverTick(level, pos, thruster.getBlockState(), thruster);
                helper.assertTrue(Math.abs(thruster.force() - Config.RCS_THRUST.get()) < 1e-6, "force " + thruster.force());
                helper.assertTrue(thruster.force() < Config.ION_THRUST.get(), "RCS is not weaker than an Ion Thruster");
                helper.assertTrue(thruster.storedEnergy() == 1000 - Config.RCS_FE_PER_TICK.get(), "energy left " + thruster.storedEnergy());
                helper.succeed();
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    /** It stayed a material of the Sequenced Assembly chain with the same id, and it can now be placed. */
    @GameTest(template = "empty")
    public static void theRcsItemKeepsItsIdAndPlacesTheBlock(GameTestHelper helper) {
        var item = ModMaterials.RCS_THRUSTER.get();
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(VoidReclaimer.id("rcs_thruster")), "the item id changed");
        helper.assertTrue(item.getBlock() == ModBlocks.RCS_THRUSTER.get(), "it does not place the block");
        helper.assertTrue(ModMaterials.ALL.stream().anyMatch(m -> m.item().get() == item), "it left the material list");
        helper.succeed();
    }

    /** Every TFMG fuel is a real fluid, goes in the tank and pushes weaker than Nebulite Propellant. A wrong id in the data map fails here. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void tfmgFuelsBurnAtTheirOwnStrengths(GameTestHelper helper) {
        withThruster(helper, new BlockPos(61_900, 80, 0), thruster -> {
            openThrottle(thruster);
            for (String id : List.of("kerosene", "lpg", "gasoline", "diesel")) {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmg", id));
                helper.assertTrue(fluid != Fluids.EMPTY, "no fluid tfmg:" + id);
                ThrusterFuel fuel = fluid.builtInRegistryHolder().getData(ModDataMaps.THRUSTER_FUEL);
                helper.assertTrue(fuel != null, "tfmg:" + id + " is not a thruster fuel");
                thruster.tank().setFluid(FluidStack.EMPTY);
                helper.assertTrue(thruster.fluids().fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, id + " did not go in");
                tick(thruster, FULL_SPIN_RPM, 3);
                double expected = Config.CHEMICAL_THRUST.get() * fuel.thrust();
                helper.assertTrue(Math.abs(thruster.force() - expected) < 1e-3, id + ": force " + thruster.force() + ", expected " + expected);
                helper.assertTrue(thruster.force() < Config.CHEMICAL_THRUST.get(), id + " is as strong as Nebulite Propellant");
            }
        });
    }
}
