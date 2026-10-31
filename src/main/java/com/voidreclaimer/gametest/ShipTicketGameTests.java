package com.voidreclaimer.gametest;

import static com.voidreclaimer.gametest.AeronauticsGameTests.clearArea;
import static com.voidreclaimer.gametest.AeronauticsGameTests.command;

import com.voidreclaimer.ship.ShipTickets;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.station.StationAssembly;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Sable's force-load tickets, proved before anything relies on them. */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ShipTicketGameTests {
    /** Well past the 25 ticks a ship took to unload once it was let go. */
    private static final int HELD_TICKS = 200;

    private ShipTicketGameTests() {}

    /** The same ship both ways: held, it stays loaded and readable; let go, it unloads, so the ticket is what kept it. */
    @GameTest(template = "empty", timeoutTicks = 1400)
    public static void aHeldShipStaysLoadedWithNothingNearIt(GameTestHelper helper) {
        // Before anything here touches ShipTickets. Sable looks the type up by name when it loads its tickets; with no such type
        // it drops them and the ship stays unloaded after a restart. Seen with a real restart; a GameTest cannot do one.
        helper.assertTrue(dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.byName(com.voidreclaimer.VoidReclaimer.id("hold")) != null,
                "the ticket type was not made when the mod started");
        BlockPos pos = new BlockPos(106_000, 200, 3_000);
        String name = "vr_test_" + pos.getX();
        UUID holder = UUID.randomUUID();
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        space.setChunkForced(chunkX, chunkZ, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getX() + " " + pos.getY() + " " + (pos.getZ() + 2) + " minecraft:stone");
            command(server, space, pos, "sable assemble connected " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, pos.south());
                var ship = StationGameTests.shipNamed(space, name);
                helper.assertTrue(ship != null, "the ship was not built");
                BlockPos block = StationAssembly.blocksOf(space, ship).get(0);
                helper.assertTrue(ShipTickets.hold(space, ship, holder), "the ticket was refused");
                helper.assertFalse(ShipTickets.hold(space, ship, holder), "the same holder held it twice");
                space.setChunkForced(chunkX, chunkZ, false);
                helper.runAfterDelay(HELD_TICKS, () -> {
                    var held = StationGameTests.shipNamed(space, name);
                    boolean readable = space.getBlockState(block).is(Blocks.STONE);
                    helper.assertTrue(held != null, "the ship unloaded while it was held");
                    helper.assertTrue(readable, "the ship stayed but its blocks cannot be read");
                    helper.assertTrue(ShipTickets.release(space, held, holder), "the holder did not hold it");
                    helper.assertFalse(ShipTickets.release(space, held, holder), "let go of twice");
                    StationGameTests.whenShip(helper, space, name, false, 80, () -> {
                        space.setChunkForced(chunkX, chunkZ, true);
                        StationGameTests.whenShip(helper, space, name, true, 80, () -> {
                            StationGameTests.remove(server, space, pos, name);
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }
}
