package com.voidreclaimer.gametest;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.ship.ChemicalThrusterBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Chemical Thruster as a block: its tank, fuel burn and force. That it really moves a ship is in {@link AeronauticsGameTests}. */
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
}
