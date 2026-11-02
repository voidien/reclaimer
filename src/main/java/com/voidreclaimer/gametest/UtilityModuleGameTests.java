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
}
