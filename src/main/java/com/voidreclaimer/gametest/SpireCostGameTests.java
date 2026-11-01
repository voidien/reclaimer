package com.voidreclaimer.gametest;

import com.simibubi.create.api.stress.BlockStressValues;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.terraform.SpireAssembly;
import com.voidreclaimer.terraform.SpireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a Genesis Pulse costs: a catalyst, two tanks and rotation while charging. Towers stand high over sector 14 of ruined
 * Earth, which nothing restores, and every test takes its tower down in the tick it ends, before a wave can touch the ground.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpireCostGameTests {
    private SpireCostGameTests() {}

    private interface Test {
        void run(ServerLevel earth, SpireBlockEntity spire, BlockPos pos);
    }

    private static void inTower(GameTestHelper helper, int x, Test test) {
        ServerLevel earth = SpireGameTests.earth(helper);
        BlockPos pos = new BlockPos(x, 200, 300);
        SpireGameTests.forceChunks(earth, pos, 8, true);
        helper.runAfterDelay(5, () -> {
            try {
                SpireTowerGameTests.clear(earth, pos);
                test.run(earth, SpireTowerGameTests.build(helper, earth, pos), pos);
            } finally {
                SpireTowerGameTests.clear(earth, pos);
                SpireGameTests.forceChunks(earth, pos, 8, false);
            }
        });
    }

    private static void load(SpireBlockEntity spire) {
        spire.catalyst().setStackInSlot(0, new ItemStack(ModMaterials.ATMOSPHERIC_CATALYST_CORE.get(), 2));
        spire.waterTank().fill(new FluidStack(ModFluids.PURIFIED_WATER.source().get(), Config.SPIRE_WATER.get() + 100), IFluidHandler.FluidAction.EXECUTE);
        spire.slurryTank().fill(new FluidStack(ModFluids.BIO_NUTRIENT_SLURRY.source().get(), Config.SPIRE_SLURRY.get() + 100), IFluidHandler.FluidAction.EXECUTE);
    }

    private static void turn(SpireBlockEntity spire, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            spire.setSpeed(rpm);
            spire.tick();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aPulseNeedsEveryPartOfItsCost(GameTestHelper helper) {
        inTower(helper, 20, (earth, spire, pos) -> {
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_CATALYST, "no catalyst");
            spire.catalyst().setStackInSlot(0, new ItemStack(ModMaterials.ATMOSPHERIC_CATALYST_CORE.get()));
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_WATER, "no water");
            spire.waterTank().fill(new FluidStack(ModFluids.PURIFIED_WATER.source().get(), Config.SPIRE_WATER.get() - 1), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_WATER, "one millibucket short of water");
            spire.waterTank().fill(new FluidStack(ModFluids.PURIFIED_WATER.source().get(), 1), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_SLURRY, "no slurry");
            spire.slurryTank().fill(new FluidStack(ModFluids.BIO_NUTRIENT_SLURRY.source().get(), Config.SPIRE_SLURRY.get()), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_ROTATION, "no rotation");
            spire.setSpeed(Config.SPIRE_MIN_RPM.get() - 1);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_ROTATION, "too slow");
            spire.setSpeed(-Config.SPIRE_MIN_RPM.get());
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.STARTED, "did not start with everything there");
            helper.assertTrue(spire.charging() && !spire.running(), "a pulse should charge first");
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.BUSY, "a second start during the charge");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theChargeNeedsRotationAndPaysWhenItEnds(GameTestHelper helper) {
        inTower(helper, 52, (earth, spire, pos) -> {
            load(spire);
            spire.setSpeed(64f);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.STARTED, "did not start");
            int ticks = Config.SPIRE_CHARGE_TICKS.get();
            turn(spire, 64f, ticks / 2);
            float half = spire.chargeProgress();
            helper.assertTrue(half > 0.4f && half < 0.6f, "half the ticks gave " + half);
            turn(spire, 0f, 50);
            helper.assertTrue(spire.charging() && spire.chargeProgress() == half, "a still shaft moved the charge: " + spire.chargeProgress());
            helper.assertTrue(spire.catalyst().getStackInSlot(0).getCount() == 2 && spire.waterTank().getFluidAmount() == Config.SPIRE_WATER.get() + 100,
                    "paid before the charge ended");
            turn(spire, 128f, (ticks - ticks / 2) / 2 + 1);
            helper.assertTrue(!spire.charging() && spire.running(), "the pulse did not leave: " + spire.chargeProgress());
            helper.assertTrue(spire.catalyst().getStackInSlot(0).getCount() == 2 - Config.SPIRE_CATALYSTS.get(), "catalysts left " + spire.catalyst().getStackInSlot(0));
            helper.assertTrue(spire.waterTank().getFluidAmount() == 100, "water left " + spire.waterTank().getFluidAmount());
            helper.assertTrue(spire.slurryTank().getFluidAmount() == 100, "slurry left " + spire.slurryTank().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aFallenTowerEndsTheChargeAndKeepsTheCost(GameTestHelper helper) {
        inTower(helper, 84, (earth, spire, pos) -> {
            load(spire);
            spire.setSpeed(64f);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.STARTED, "did not start");
            turn(spire, 64f, 5);
            SpireAssembly.disassemble(spire);
            turn(spire, 64f, 1);
            helper.assertFalse(spire.charging(), "still charging with no tower");
            helper.assertTrue(spire.catalyst().getStackInSlot(0).getCount() == 2, "the catalyst was spent");
            helper.succeed();
        });
    }

    /** Pipes and funnels only ever feed it, and only what its tags allow. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void automationOnlyPutsThingsIn(GameTestHelper helper) {
        inTower(helper, 116, (earth, spire, pos) -> {
            IItemHandler items = earth.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            IFluidHandler fluids = earth.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
            helper.assertTrue(items != null && fluids != null, "no capabilities on the Spire");
            helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "took dirt");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.ATMOSPHERIC_CATALYST_CORE.get()), false).isEmpty(), "refused a catalyst");
            helper.assertTrue(items.extractItem(0, 1, false).isEmpty(), "gave a catalyst back");
            helper.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took lava");
            helper.assertTrue(fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.source().get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "refused water");
            helper.assertTrue(fluids.fill(new FluidStack(ModFluids.BIO_NUTRIENT_SLURRY.source().get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "refused slurry");
            helper.assertTrue(spire.waterTank().getFluidAmount() == 1000 && spire.slurryTank().getFluidAmount() == 1000, "the fluids went to the wrong tanks");
            helper.assertTrue(fluids.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "gave fluid back");
            helper.succeed();
        });
    }

    /** The GameTest datapack adds the amethyst shard and vanilla water to the tags: no code names them. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theTagsDecideWhatItTakes(GameTestHelper helper) {
        inTower(helper, 148, (earth, spire, pos) -> {
            helper.assertTrue(spire.items().insertItem(0, new ItemStack(Items.AMETHYST_SHARD), false).isEmpty(), "a tagged item was refused");
            helper.assertTrue(spire.fluids().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "a tagged fluid was refused");
            helper.assertTrue(spire.slurryTank().getFluid().is(Fluids.WATER), "the tag names the slurry tank: " + spire.slurryTank().getFluid());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aPlayerLoadsItByHand(GameTestHelper helper) {
        inTower(helper, 180, (earth, spire, pos) -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5);
            try {
                player.teleportTo(earth, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5, 0f, 0f);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMaterials.ATMOSPHERIC_CATALYST_CORE.get(), 3));
                earth.getBlockState(pos).useItemOn(player.getMainHandItem(), earth, player, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(spire.catalyst().getStackInSlot(0).getCount() == 3 && player.getMainHandItem().isEmpty(), "the catalysts did not go in");

                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModFluids.PURIFIED_WATER.bucket().get()));
                earth.getBlockState(pos).useItemOn(player.getMainHandItem(), earth, player, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(spire.waterTank().getFluidAmount() == 1000, "the bucket did not empty: " + spire.waterTank().getFluidAmount());
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "no empty bucket back: " + player.getMainHandItem());
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theStockAndTheChargeSurviveAReload(GameTestHelper helper) {
        inTower(helper, 212, (earth, spire, pos) -> {
            load(spire);
            spire.setSpeed(64f);
            spire.tryStart();
            turn(spire, 64f, 10);
            CompoundTag tag = spire.saveWithFullMetadata(earth.registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(pos, earth.getBlockState(pos), tag, earth.registryAccess());
            helper.assertTrue(loaded instanceof SpireBlockEntity back && back.charging() && back.chargeProgress() == spire.chargeProgress()
                    && back.catalyst().getStackInSlot(0).getCount() == 2 && back.waterTank().getFluidAmount() == spire.waterTank().getFluidAmount()
                    && back.slurryTank().getFluidAmount() == spire.slurryTank().getFluidAmount(), "the Spire did not come back whole");
            helper.assertTrue(BlockStressValues.getImpact(ModBlocks.TERRAFORMING_SPIRE.get()) == Config.SPIRE_STRESS.get(), "stress impact");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void breakingTheSpireDropsItsCatalysts(GameTestHelper helper) {
        inTower(helper, 244, (earth, spire, pos) -> {
            load(spire);
            earth.destroyBlock(pos, false);
            int found = earth.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                    .filter(item -> item.getItem().is(ModMaterials.ATMOSPHERIC_CATALYST_CORE.get())).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(found == 2, "catalysts dropped: " + found);
            earth.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
            helper.succeed();
        });
    }
}
