package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Replicator;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Cloud Collector module: raw items out of the dust a body gathers. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CollectorGameTests {
    private static final int TICKS = 300;
    private static final Map<ResourceLocation, String> RAW = Map.of(
            DustKinds.METALLIC, "metallic_grains", DustKinds.SILICATE, "silicon_grit",
            DustKinds.ICE, "raw_ice_shards", DustKinds.CARBON, "carbon_flakes");

    private CollectorGameTests() {}

    /** A cloud whose heart is dense, so a short flight gathers several dust. */
    private static DustCloud denseCloud(GameTestHelper helper) {
        DustField field = DustGameTests.field(helper);
        for (int r = 0; r < 40; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    for (int y = -1; y <= 0; y++) {
                        var cloud = field.cloudInCell(x, y, z);
                        if (cloud.isPresent() && Math.abs(cloud.get().center().y) < 1500 && field.density(cloud.get(), cloud.get().center()) > 0.6) {
                            return cloud.get();
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no dense dust cloud found near the origin");
    }

    private static Item raw(DustCloud cloud) {
        return BuiltInRegistries.ITEM.get(VoidReclaimer.id(RAW.get(cloud.kind())));
    }

    /** All of one item the player has: inventory and the Replicator's output buffer. */
    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            total += stack.is(item) ? stack.getCount() : 0;
        }
        for (ItemStack stack : Replicator.data(player).buffer()) {
            total += stack.is(item) ? stack.getCount() : 0;
        }
        return total;
    }

    private static ServerPlayer flyer(GameTestHelper helper, DustCloud cloud, boolean collector) {
        Vec3 c = cloud.center();
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, c.x, c.y, c.z);
        player.getInventory().clearContent();
        if (collector) {
            TestPlayers.installModule(player, ModuleKind.TOOL, 0, new ItemStack(ModItems.CLOUD_COLLECTOR.get()));
        }
        return player;
    }

    @GameTest(template = "empty")
    public static void collectorMakesTheCloudsRawItemAtItsYield(GameTestHelper helper) {
        DustCloud cloud = denseCloud(helper);
        ServerPlayer player = flyer(helper, cloud, true);
        try {
            DustGameTests.moveAndTick(player, TICKS);
            float dust = player.getData(ModAttachments.DUST).get(cloud.kind());
            int items = count(player, raw(cloud));
            helper.assertTrue(dust >= 2f, "the flight gathered only " + dust + " dust, too little to judge");
            // One item per dust gathered (yield 1.0), less the fraction still being carried.
            helper.assertTrue(items >= Math.floor(dust) - 1 && items <= Math.ceil(dust), items + " items for " + dust + " dust");
            for (Map.Entry<ResourceLocation, String> other : RAW.entrySet()) {
                if (!other.getKey().equals(cloud.kind())) {
                    helper.assertTrue(count(player, BuiltInRegistries.ITEM.get(VoidReclaimer.id(other.getValue()))) == 0,
                            "made " + other.getValue() + " in a " + cloud.kind() + " cloud");
                }
            }
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void withoutTheModuleThereAreNoItems(GameTestHelper helper) {
        DustCloud cloud = denseCloud(helper);
        ServerPlayer player = flyer(helper, cloud, false);
        try {
            DustGameTests.moveAndTick(player, TICKS);
            helper.assertTrue(player.getData(ModAttachments.DUST).get(cloud.kind()) > 0f, "no dust gathered");
            helper.assertTrue(count(player, raw(cloud)) == 0, "raw items without a collector installed");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void collectorUsesEnergyAndStopsInLowPower(GameTestHelper helper) {
        DustCloud cloud = denseCloud(helper);
        ServerPlayer with = flyer(helper, cloud, true);
        ServerPlayer without = flyer(helper, cloud, false);
        ServerPlayer dark = flyer(helper, cloud, true);
        try {
            DustGameTests.moveAndTick(with, TICKS);
            DustGameTests.moveAndTick(without, TICKS);
            // Sunlight would lift the player out of low power part-way through, so hold it there every tick.
            double x = dark.getX();
            for (int i = 0; i < TICKS; i++) {
                dark.setData(ModAttachments.ENERGY, new EnergyData(0f, dark.getData(ModAttachments.ENERGY).capacity(), true));
                x += 0.4;
                dark.setPos(x, dark.getY(), dark.getZ());
                dark.doTick();
            }
            float spent = without.getData(ModAttachments.ENERGY).energy() - with.getData(ModAttachments.ENERGY).energy();
            helper.assertTrue(spent > 5f, "the collector cost only " + spent + " energy over " + TICKS + " ticks");
            helper.assertTrue(count(dark, raw(cloud)) == 0, "a collector worked in low power");
            helper.succeed();
        } finally {
            TestPlayers.remove(with);
            TestPlayers.remove(without);
            TestPlayers.remove(dark);
        }
    }

    @GameTest(template = "empty")
    public static void fullInventoryOverflowsIntoTheOutputBuffer(GameTestHelper helper) {
        DustCloud cloud = denseCloud(helper);
        ServerPlayer player = flyer(helper, cloud, true);
        try {
            for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                player.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE));
            }
            DustGameTests.moveAndTick(player, TICKS);
            helper.assertTrue(Replicator.data(player).buffer().stream().anyMatch(stack -> stack.is(raw(cloud))),
                    "nothing reached the output buffer");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
