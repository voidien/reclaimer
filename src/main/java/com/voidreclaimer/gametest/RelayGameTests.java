package com.voidreclaimer.gametest;

import static com.voidreclaimer.gametest.AeronauticsGameTests.clearArea;
import static com.voidreclaimer.gametest.AeronauticsGameTests.command;
import static com.voidreclaimer.gametest.AeronauticsGameTests.nameShip;
import static com.voidreclaimer.gametest.AeronauticsGameTests.removeShip;
import static com.voidreclaimer.gametest.AeronauticsGameTests.shipSelector;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.RelayBlock;
import com.voidreclaimer.link.RelayBlockEntity;
import com.voidreclaimer.link.RelayList;
import com.voidreclaimer.link.Relays;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.space.ModDimensions;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A Relay's power, and the list of relays a level keeps. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RelayGameTests {
    private RelayGameTests() {}

    private static RelayBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.RELAY.get().defaultBlockState(), 3);
        return (RelayBlockEntity) level.getBlockEntity(pos);
    }

    private static Optional<RelayList.Entry> row(ServerLevel level, UUID id) {
        return Relays.of(level).find(id);
    }

    private static String readout(RelayBlockEntity relay) {
        return ((TranslatableContents) relay.readout().getContents()).getKey();
    }

    /** Through the same capability any energy mod uses: nothing runs on one tick's energy, and it goes dark when it runs out. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRelayRunsOnForgeEnergyAndGoesDarkWithoutIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(176_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            RelayBlockEntity relay = place(level, pos);
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            helper.assertTrue(energy != null, "a Relay takes no Forge Energy");
            int perTick = Config.LINK_RELAY_FE_PER_TICK.get();
            int buffer = Config.LINK_RELAY_FE_BUFFER.get();
            int[] before = new int[1];
            helper.startSequence()
                    .thenExecuteAfter(3, () -> {
                        helper.assertTrue(!relay.isLit(), "a Relay with no energy is lit");
                        helper.assertTrue(readout(relay).endsWith(".dark"), "a dark Relay reads " + readout(relay));
                        helper.assertTrue(level.getBlockState(pos).getLightEmission() == 0, "a dark Relay gives light");
                        energy.receiveEnergy(perTick, false);
                    })
                    .thenExecuteAfter(3, () -> {
                        helper.assertTrue(!relay.isLit(), "one tick of energy lit a dark Relay");
                        helper.assertTrue(relay.storedEnergy() == perTick, "a dark Relay spent energy: " + relay.storedEnergy());
                        int taken = energy.receiveEnergy(Integer.MAX_VALUE, false);
                        helper.assertTrue(taken == buffer - perTick, "the buffer took " + taken);
                        helper.assertTrue(energy.extractEnergy(1, false) == 0, "energy came back out of a Relay");
                    })
                    .thenExecuteAfter(2, () -> {
                        helper.assertTrue(relay.isLit(), "a full Relay is dark");
                        helper.assertTrue(readout(relay).endsWith(".online"), "a lit Relay reads " + readout(relay));
                        helper.assertTrue(level.getBlockState(pos).getLightEmission() > 0, "a lit Relay gives no light");
                        helper.assertTrue(row(level, relay.id()).filter(RelayList.Entry::lit).isPresent(), "the list does not say the Relay lit up");
                        before[0] = relay.storedEnergy();
                    })
                    .thenExecuteAfter(5, () -> helper.assertTrue(before[0] - relay.storedEnergy() == 5 * perTick,
                            "five ticks took " + (before[0] - relay.storedEnergy()) + " FE"))
                    .thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(pos).getValue(RelayBlock.LIT), "the Relay ran out and stayed lit"))
                    .thenExecute(() -> {
                        helper.assertTrue(relay.storedEnergy() < perTick, "it went dark with " + relay.storedEnergy() + " FE left");
                        helper.assertTrue(row(level, relay.id()).filter(entry -> !entry.lit()).isPresent(), "the list does not say the Relay went dark");
                        level.removeBlock(pos, false);
                    })
                    .thenSucceed();
        });
    }

    /**
     * A shell past a seat's reach and inside a relay's: the lit relay carries the signal, a dark one and a relay in another dimension do
     * not, and a relay does nothing for a place past its own reach. The rows are written by hand: what makes a row lit has its own test.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aLitRelayCarriesTheSignal(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        MinecraftServer server = space.getServer();
        UUID id = new UUID(177_800L, 1L);
        UUID elsewhereId = new UUID(177_800L, 2L);
        var seat = new com.voidreclaimer.link.LinkState.Place(space.dimension(), new Vec3(177_800.5, 80.5, 0.5), 0f, 0f);
        double seatRange = com.voidreclaimer.link.Signal.range(server, seat);
        double relayRange = com.voidreclaimer.link.Signal.relayRange(server);
        Vec3 relaySpot = seat.position().add(seatRange * 0.9, 0.0, 0.0);
        var shell = new com.voidreclaimer.link.LinkState.Place(space.dimension(), relaySpot.add(relayRange * 0.5, 0.0, 0.0), 0f, 0f);
        var tooFar = new com.voidreclaimer.link.LinkState.Place(space.dimension(), relaySpot.add(relayRange * 1.1, 0.0, 0.0), 0f, 0f);
        BlockPos unloaded = new BlockPos(177_800, 80, 900_000);
        helper.assertTrue(!com.voidreclaimer.link.Signal.read(server, seat, shell).reachable(), "the seat reaches the shell by itself");

        Relays.note(space, new RelayList.Entry(id, unloaded, relaySpot, true, Optional.empty()));
        var carried = com.voidreclaimer.link.Signal.read(server, seat, shell);
        helper.assertTrue(carried.reachable() && carried.hops() == 1, "a lit relay did not carry the signal: " + carried);
        helper.assertTrue(!com.voidreclaimer.link.Signal.read(server, seat, tooFar).reachable(), "a relay reached past its own range");

        Relays.note(space, new RelayList.Entry(id, unloaded, relaySpot, false, Optional.empty()));
        helper.assertTrue(!com.voidreclaimer.link.Signal.read(server, seat, shell).reachable(), "a dark relay carried the signal");

        var earth = server.getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        Relays.note(earth, new RelayList.Entry(elsewhereId, unloaded, relaySpot, true, Optional.empty()));
        helper.assertTrue(!com.voidreclaimer.link.Signal.read(server, seat, shell).reachable(), "a relay in another dimension carried the signal");

        space.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, Relays.of(space).without(id));
        earth.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, Relays.of(earth).without(elsewhereId));
        helper.succeed();
    }

    /** A Relay is on the list as soon as it has ticked for a second, and off it a second after its block has gone. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theListFollowsTheBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(176_300, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            RelayBlockEntity relay = place(level, pos);
            UUID id = relay.id();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(row(level, id).isPresent(), "a placed Relay is not on the list"))
                    .thenExecute(() -> {
                        RelayList.Entry entry = row(level, id).orElseThrow();
                        helper.assertTrue(entry.block().equals(pos), "the row's block is " + entry.block());
                        helper.assertTrue(entry.position().equals(Vec3.atCenterOf(pos)), "the row's place is " + entry.position());
                        helper.assertTrue(relay.relaysInLevel() >= 1, "the Relay counts " + relay.relaysInLevel() + " relays");
                        level.removeBlock(pos, false);
                    })
                    .thenWaitUntil(() -> helper.assertTrue(row(level, id).isEmpty(), "a broken Relay stayed on the list"))
                    .thenSucceed();
        });
    }

    /** A row is only dropped on the word of a loaded chunk, and only the relay with that id can answer for it. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void onlyALoadedChunkCanForgetARelay(GameTestHelper helper) {
        BlockPos pos = new BlockPos(176_600, 80, 0);
        BlockPos unloaded = new BlockPos(176_600, 80, 900_000);
        UUID far = new UUID(176_600L, 1L);
        UUID stranger = new UUID(176_600L, 2L);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            helper.assertTrue(!level.isLoaded(unloaded), "the far chunk is loaded, so this says nothing");
            RelayBlockEntity relay = place(level, pos);
            Relays.note(level, new RelayList.Entry(far, unloaded, Vec3.atCenterOf(unloaded), true, Optional.empty()));
            Relays.note(level, new RelayList.Entry(stranger, pos, Vec3.atCenterOf(pos), true, Optional.empty()));
            helper.startSequence()
                    .thenExecuteAfter(45, () -> {
                        helper.assertTrue(row(level, far).filter(RelayList.Entry::lit).isPresent(), "an unloaded Relay was forgotten");
                        helper.assertTrue(!level.isLoaded(unloaded), "checking a row loaded its chunk");
                        helper.assertTrue(row(level, stranger).isEmpty(), "a row with another Relay's block stayed");
                        helper.assertTrue(row(level, relay.id()).isPresent(), "the real Relay went with the stranger");
                        level.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, Relays.of(level).without(far));
                        level.removeBlock(pos, false);
                    })
                    .thenSucceed();
        });
    }

    /** A ship that is removed takes its relays with it: nothing breaks their blocks, and a row left behind would count for good. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "relay_on_a_ship")
    public static void aRelayGoesWithItsShip(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        BlockPos pos = new BlockPos(177_200, 230, 3_000);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        UUID[] id = new UUID[1];
        RelayBlockEntity[] aboard = new RelayBlockEntity[1];
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + (pos.getX() - 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() - 1) + " " + (pos.getX() + 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() + 1) + " minecraft:iron_block");
            id[0] = place(space, pos).id();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).isPresent(), "the Relay is not on the list"))
                    .thenExecute(() -> command(server, space, pos, "sable assemble connected " + pos.getX() + " " + (pos.getY() - 1) + " " + pos.getZ()))
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).filter(entry -> !entry.block().equals(pos)).isPresent(), "the row did not go aboard: " + row(space, id[0])))
                    .thenExecute(() -> {
                        aboard[0] = (RelayBlockEntity) space.getBlockEntity(row(space, id[0]).orElseThrow().block());
                        nameShip(server, space, pos);
                        removeShip(server, space, pos);
                    })
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).isEmpty(), "a Relay removed with its ship stayed on the list: " + row(space, id[0])))
                    // A removed ship's plot can linger, and a block still ticking in it must not write the row back. Here the plot's ticking
                    // has stopped, so the tick is given by hand, for long enough to cover a second's note.
                    .thenExecuteFor(25, () -> {
                        BlockState state = aboard[0].getBlockState();
                        ModBlocks.RELAY.get().getTicker(space, state, ModBlockEntities.RELAY.get()).tick(space, aboard[0].getBlockPos(), state, aboard[0]);
                    })
                    .thenExecute(() -> {
                        helper.assertTrue(row(space, id[0]).isEmpty(), "the ship's old plot put the Relay back on the list: " + row(space, id[0]));
                        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                    })
                    .thenSucceed();
        });
    }

    /** A ship that only unloads comes back, so its relays stay listed: nothing about them can have changed. */
    @GameTest(template = "empty", timeoutTicks = 400, batch = "relay_on_a_ship")
    public static void aShipThatUnloadsKeepsItsRelays(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        BlockPos pos = new BlockPos(177_500, 230, 3_000);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        UUID[] id = new UUID[1];
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + (pos.getX() - 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() - 1) + " " + (pos.getX() + 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() + 1) + " minecraft:iron_block");
            id[0] = place(space, pos).id();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).isPresent(), "the Relay is not on the list"))
                    .thenExecute(() -> command(server, space, pos, "sable assemble connected " + pos.getX() + " " + (pos.getY() - 1) + " " + pos.getZ()))
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).filter(entry -> entry.ship().isPresent()).isPresent(), "the row did not go aboard: " + row(space, id[0])))
                    .thenExecute(() -> {
                        var ship = com.voidreclaimer.ship.Ships.shipAt(space, row(space, id[0]).orElseThrow().block());
                        helper.assertTrue(ship != null, "no ship at the row's block");
                        dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).removeSubLevel(ship, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.UNLOADED);
                    })
                    .thenExecuteAfter(25, () -> {
                        helper.assertTrue(row(space, id[0]).isPresent(), "a Relay left the list when its ship unloaded");
                        space.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, Relays.of(space).without(id[0]));
                        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                    })
                    .thenSucceed();
        });
    }

    /** Aboard a ship the row keeps the block in the ship's coordinates and the place in the world, and it follows the ship. */
    @GameTest(template = "empty", timeoutTicks = 900, batch = "relay_on_a_ship")
    public static void aRelayAboardAShipIsWhereTheShipIs(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        BlockPos pos = new BlockPos(176_900, 230, 3_000);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        UUID[] id = new UUID[1];
        String[] moved = new String[1];
        UUID otherShips = new UUID(176_900L, 1L);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + (pos.getX() - 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() - 1) + " " + (pos.getX() + 1) + " " + (pos.getY() - 1) + " " + (pos.getZ() + 1) + " minecraft:iron_block");
            id[0] = place(space, pos).id();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).isPresent(), "the Relay is not on the list"))
                    .thenExecute(() -> command(server, space, pos, "sable assemble connected " + pos.getX() + " " + (pos.getY() - 1) + " " + pos.getZ()))
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).filter(entry -> !entry.block().equals(pos)).isPresent(), "the row did not go aboard: " + row(space, id[0])))
                    .thenExecute(() -> {
                        RelayList.Entry aboard = row(space, id[0]).orElseThrow();
                        helper.assertTrue(aboard.position().distanceTo(Vec3.atCenterOf(pos)) < 2.0, "the row's place is " + aboard.position() + ", not in the world by " + pos);
                        helper.assertTrue(aboard.ship().isPresent(), "the row does not know its ship");
                        // Another ship's row in this plot: this ship cannot answer for it.
                        Relays.note(space, new RelayList.Entry(otherShips, aboard.block(), aboard.position(), true, Optional.of(otherShips)));
                        nameShip(server, space, pos);
                        // Straight up, to stay in the one chunk this test holds: a ship outside it stops ticking its blocks.
                        moved[0] = command(server, space, pos, "sable teleport " + shipSelector(pos) + " " + pos.getX() + " " + (pos.getY() + 20) + " " + pos.getZ() + " 0 0").toString();
                    })
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).orElseThrow().position().distanceTo(Vec3.atCenterOf(pos)) > 15.0, "the row did not follow the ship: " + row(space, id[0]) + ", the teleport said " + moved[0]))
                    .thenExecuteAfter(25, () -> {
                        helper.assertTrue(row(space, otherShips).isPresent(), "a ship answered for another ship's relay");
                        space.setData(com.voidreclaimer.registry.ModAttachments.RELAYS, Relays.of(space).without(otherShips));
                        space.setBlock(row(space, id[0]).orElseThrow().block(), Blocks.AIR.defaultBlockState(), 3);
                    })
                    .thenWaitUntil(() -> helper.assertTrue(row(space, id[0]).isEmpty(), "a Relay broken aboard stayed on the list"))
                    .thenExecute(() -> {
                        removeShip(server, space, pos);
                        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                    })
                    .thenSucceed();
        });
    }
}
