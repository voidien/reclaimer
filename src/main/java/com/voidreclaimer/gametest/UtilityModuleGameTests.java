package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Utility modules: always on, in the mounts every body's core section offers. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UtilityModuleGameTests {
    private UtilityModuleGameTests() {}

    private static ItemStack utility() {
        ItemStack stack = new ItemStack(ModItems.BRASS_TRIM.get());
        stack.set(ModDataComponents.MODULE.get(), ModuleStats.simple(ModuleKind.UTILITY, 1, 0.1f));
        return stack;
    }

    /** Every body has somewhere for one, it takes that and nothing else, and a body wearing one saves and loads. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyBodyHasUtilityMounts(GameTestHelper helper) {
        BlockPos origin = new BlockPos(185_008, 300, 8);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Map<ResourceKey<Chassis>, Integer> mounts = Map.of(BuiltinBodies.DRONE, 1, BuiltinBodies.MECH, 1, BuiltinBodies.SHIP, 2, BuiltinBodies.SHUTTLE, 2, BuiltinBodies.TITAN, 3);
            mounts.forEach((chassis, expected) -> {
                Bodies.store(player, Bodies.preset(chassis));
                BodyView view = Bodies.view(player);
                List<BodyData.Mount> utility = view.mounts().stream().filter(mount -> mount.kind() == ModuleKind.UTILITY).toList();
                helper.assertTrue(utility.size() == expected, chassis.location() + " has " + utility.size() + " utility mounts");
                BodyData.Mount mount = utility.get(0);
                helper.assertTrue(BodyRules.canInstallModule(view, mount, utility()) == BodyRules.Check.OK, "a utility module did not fit " + chassis.location());
                helper.assertTrue(BodyRules.canInstallModule(view, mount, new ItemStack(ModItems.BRASS_TRIM.get())) == BodyRules.Check.WRONG_MOUNT, "a cosmetic went in a utility mount");
                BodyData worn = view.data().withModule(mount, utility());
                var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                Tag saved = BodyData.CODEC.encodeStart(ops, worn).getOrThrow();
                BodyData read = BodyData.CODEC.parse(ops, saved).getOrThrow();
                helper.assertTrue(!read.module(mount).isEmpty(), "a utility module did not survive a save on " + chassis.location());
            });
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** It adds to what a body holds of every kind and adds no rows, its tier is the core's business, and a full Ship can now pay for a Titan's kit. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDustCompressorStoresDustAndAddsNoRows(GameTestHelper helper) {
        BlockPos origin = new BlockPos(185_408, 300, 8);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.SHIP));
            BodyView bare = Bodies.view(player);
            float holds = DustData.capacity(player);
            BodyData.Mount mount = bare.mounts().stream().filter(found -> found.kind() == ModuleKind.UTILITY).findFirst().orElseThrow();
            ItemStack mk1 = new ItemStack(ModItems.DUST_COMPRESSOR_MK1.get());
            helper.assertTrue(BodyRules.canInstallModule(bare, mount, mk1) == BodyRules.Check.OK, "a Compressor did not fit a Ship");
            Bodies.store(player, bare.data().withModule(mount, mk1));
            helper.assertTrue(DustData.capacity(player) == holds + 600f, "a Ship with a Mk I holds " + DustData.capacity(player) + ", not " + (holds + 600f));
            Bodies.store(player, bare.data().withModule(mount, new ItemStack(ModItems.DUST_COMPRESSOR_MK3.get())));
            helper.assertTrue(DustData.capacity(player) == holds + 2400f, "a Ship with a Mk III holds " + DustData.capacity(player));

            // A Ship with its storage mounts full holds 1400: short of a Titan Core Kit's 1650 metallic until it has a Compressor.
            Bodies.store(player, Bodies.preset(BuiltinBodies.SHIP, true));
            helper.assertTrue(DustData.capacity(player) < 1650f, "a full Ship could already pay for the kit: " + DustData.capacity(player));
            Bodies.store(player, Bodies.data(player).withModule(mount, mk1));
            helper.assertTrue(DustData.capacity(player) >= 1650f, "a full Ship with a Mk I can't hold a Titan Core Kit's price: " + DustData.capacity(player));

            Bodies.store(player, Bodies.starter());
            BodyView drone = Bodies.view(player);
            BodyData.Mount small = drone.mounts().stream().filter(found -> found.kind() == ModuleKind.UTILITY).findFirst().orElseThrow();
            helper.assertTrue(BodyRules.canInstallModule(drone, small, mk1) == BodyRules.Check.TIER_TOO_HIGH, "a Core Mk I took a Mk II part");
            // A Drone, because a Ship is at the row limit already and more rows couldn't show.
            Bodies.store(player, drone.data().withModule(small, mk1));
            helper.assertTrue(Bodies.stats(player).rows() == drone.stats().rows(), "a Compressor added inventory rows");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** It adds health and mass in a utility mount, its tier is the core's business, and taking it off takes the health back. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hullPlatingAddsHealthAndMass(GameTestHelper helper) {
        BlockPos origin = new BlockPos(185_824, 300, 8);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.starter());
            BodyView bare = Bodies.view(player);
            BodyData.Mount mount = bare.mounts().stream().filter(found -> found.kind() == ModuleKind.UTILITY).findFirst().orElseThrow();
            ItemStack mk1 = new ItemStack(ModItems.HULL_PLATING_MK1.get());
            helper.assertTrue(BodyRules.canInstallModule(bare, mount, mk1) == BodyRules.Check.OK, "a Drone could not wear a Mk I");
            helper.assertTrue(BodyRules.canInstallModule(bare, mount, new ItemStack(ModItems.HULL_PLATING_MK2.get())) == BodyRules.Check.TIER_TOO_HIGH, "a Core Mk I took a Mk II");

            Bodies.store(player, bare.data().withModule(mount, mk1));
            float whole = bare.stats().maxHealth() + 4f;
            helper.assertTrue(player.getMaxHealth() == whole, "a plated Drone has " + player.getMaxHealth() + " health, not " + whole);
            helper.assertTrue(Bodies.stats(player).mass() == bare.stats().mass() + 0.5f, "plating added no mass: " + Bodies.stats(player).mass());
            player.setHealth(whole);

            // Any module can carry the spec, so a datapack's own plating works the same way.
            ItemStack custom = mk1.copy();
            custom.set(com.voidreclaimer.registry.ModDataComponents.MODULE.get(), com.voidreclaimer.api.body.ModuleStats.utility(1, 0.1f, new com.voidreclaimer.api.body.ModuleStats.Plating(10f)));
            Bodies.store(player, bare.data().withModule(mount, custom));
            helper.assertTrue(player.getMaxHealth() == bare.stats().maxHealth() + 10f, "a module of its own gave " + player.getMaxHealth());
            player.setHealth(player.getMaxHealth());

            Bodies.store(player, bare.data());
            helper.assertTrue(player.getMaxHealth() == bare.stats().maxHealth(), "the health stayed after the plating came off");
            helper.assertTrue(player.getHealth() == bare.stats().maxHealth(), "health over the new limit: " + player.getHealth());
            TestPlayers.remove(player);
            helper.succeed();
        });
    }
}
