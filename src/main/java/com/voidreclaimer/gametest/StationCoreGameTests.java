package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.station.StationCoreBlockEntity;
import com.voidreclaimer.station.StationList;
import com.voidreclaimer.station.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Station Core in the world (no ship needed): the station list, and the Replicator's bonus at a station. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationCoreGameTests {
    private StationCoreGameTests() {}

    /** A placed core shows up in the level's station list under its name, at its spot, not assembled. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aPlacedCoreIsListed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(66_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(pos, ModBlocks.STATION_CORE.get().defaultBlockState(), 3);
            StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(pos);
            core.setStationName("Waypoint");
            helper.runAfterDelay(Stations.REFRESH_TICKS + 2, () -> {
                StationList list = space.getData(ModAttachments.STATIONS);
                StationList.Entry entry = list.stations().stream().filter(e -> e.name().equals("Waypoint")).findFirst().orElse(null);
                helper.assertTrue(entry != null, "Waypoint is not listed: " + list);
                helper.assertTrue(!entry.assembled() && entry.position().distanceTo(Vec3.atCenterOf(pos)) < 0.01, "listed wrongly: " + entry);
                space.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                helper.runAfterDelay(Stations.REFRESH_TICKS + 2, () -> {
                    boolean gone = space.getData(ModAttachments.STATIONS).stations().stream().noneMatch(e -> e.name().equals("Waypoint"));
                    helper.assertTrue(gone, "a broken core is still listed");
                    helper.succeed();
                });
            });
        });
    }

    /** Within range of a station the Replicator makes the configured steps of progress per tick; away from it, one. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theReplicatorIsFasterAtAStation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(67_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(pos, ModBlocks.STATION_CORE.get().defaultBlockState(), 3);
            ServerPlayer near = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
            ServerPlayer far = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 60.5, pos.getY(), pos.getZ() + 0.5);
            helper.runAfterDelay(Stations.REFRESH_TICKS + 2, () -> {
                try {
                    ServerPlayer nearNow = space.getServer().getPlayerList().getPlayer(near.getUUID());
                    ServerPlayer farNow = space.getServer().getPlayerList().getPlayer(far.getUUID());
                    helper.assertTrue(Stations.replicatorSteps(nearNow) == com.voidreclaimer.Config.STATION_REPLICATOR_SPEED.get(), "no bonus at the station");
                    helper.assertTrue(Stations.replicatorSteps(farNow) == 1, "a bonus far from any station");
                    helper.succeed();
                } finally {
                    space.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    TestPlayers.remove(near);
                    TestPlayers.remove(far);
                }
            });
        });
    }
}
