package com.voidreclaimer.gametest;

import static com.voidreclaimer.gametest.AeronauticsGameTests.clearArea;
import static com.voidreclaimer.gametest.AeronauticsGameTests.command;

import com.voidreclaimer.earth.AtmosphericPortals;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.ship.HelmBlockEntity;
import com.voidreclaimer.ship.HelmSeat;
import com.voidreclaimer.ship.Ships;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.station.StationAssembly;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

/**
 * Ships through the Atmospheric Portal (Aeronautics suite). A lit 12 x 6 frame stands across X in deep space; a ship
 * north of it is shoved south through the pane.
 */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ShipPortalGameTests {
    private static final int WIDTH = 12, HEIGHT = 6;
    private static final int CROSS_TICKS = 80;

    private ShipPortalGameTests() {}

    /** A small ship with its pilot flies south into the pane and comes out on ruined Earth, still moving, pilot seated. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aShipFliesThroughThePortalWithItsPilotAndKeepsGoing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(110_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        force(space, origin);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, origin);
            buildFrame(space, origin);
            helper.assertTrue(AtmosphericPortals.tryLight(space, origin.below()), "the frame did not light");
            // The ship: a bar of three stones with a Helm on top, four blocks north of the pane, centred on the gate.
            BlockPos ship = origin.east(WIDTH / 2).above(1).north(5);
            int x = ship.getX(), y = ship.getY(), z = ship.getZ();
            command(server, space, ship, "fill " + x + " " + y + " " + (z - 1) + " " + x + " " + y + " " + (z + 1) + " minecraft:stone");
            command(server, space, ship, "setblock " + x + " " + (y + 1) + " " + z + " void_reclaimer:helm[facing=south]");
            command(server, space, ship, "sable assemble connected " + x + " " + y + " " + z);
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, ship);
                String name = "vr_test_" + ship.getX();
                ServerSubLevel body = StationGameTests.shipNamed(space, name);
                helper.assertTrue(body != null, "no ship");
                HelmBlockEntity helm = helmOf(space, body);
                helper.assertTrue(helm != null, "no Helm aboard");
                Vec3 where = body.logicalPose().transformPosition(Vec3.atBottomCenterOf(helm.getBlockPos()));
                ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, where.x, where.y + 1, where.z);
                ServerPlayer pilot = server.getPlayerList().getPlayer(player.getUUID());
                helper.assertTrue(pilot != null && helm.seat(pilot), "the pilot could not sit");
                RigidBodyHandle.of(body).addLinearAndAngularVelocity(new Vector3d(0, 0, 3), new Vector3d());
                helper.runAfterDelay(CROSS_TICKS, () -> {
                    ServerSubLevel left = StationGameTests.shipNamed(space, name);
                    ServerSubLevel arrived = StationGameTests.shipNamed(earth, name);
                    ServerPlayer now = server.getPlayerList().getPlayer(player.getUUID());
                    int stones = arrived == null ? 0 : (int) StationAssembly.blocksOf(earth, arrived).stream().filter(p -> earth.getBlockState(p).is(Blocks.STONE)).count();
                    Vector3d velocity = arrived == null ? new Vector3d() : RigidBodyHandle.of(arrived).getLinearVelocity(new Vector3d());
                    boolean pilotAboard = now != null && now.level() == earth && now.getVehicle() instanceof HelmSeat;
                    boolean helmAboard = arrived != null && helmOf(earth, arrived) != null;
                    String blocksAboard = arrived == null ? "" : StationAssembly.blocksOf(earth, arrived).stream()
                            .map(p -> p.toShortString() + ":" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(earth.getBlockState(p).getBlock()).getPath()
                                    + (earth.getBlockEntity(p) == null ? "" : "+be")).toList().toString();
                    String report = "left=" + (left != null) + " arrived=" + (arrived != null) + " stones=" + stones + " v=" + velocity + " pilot in "
                            + (now == null ? "nowhere" : now.level().dimension().location() + " riding " + now.getVehicle())
                            + " seat helm " + (now != null && now.getVehicle() instanceof HelmSeat s ? s.helmPos().toShortString() : "-") + " blocks " + blocksAboard;
                    TestPlayers.remove(player);
                    if (arrived != null) {
                        command(server, earth, BlockPos.containing(arrived.logicalPose().position().x(), arrived.logicalPose().position().y(), arrived.logicalPose().position().z()), "sable remove @e[name=" + name + ",limit=1]");
                    }
                    if (left != null) {
                        command(server, space, ship, "sable remove @e[name=" + name + ",limit=1]");
                    }
                    helper.assertTrue(left == null && arrived != null, "the ship did not cross: " + report);
                    helper.assertTrue(stones == 3 && helmAboard, "the ship did not arrive whole: " + report);
                    helper.assertTrue(pilotAboard, "the pilot did not come along seated: " + report);
                    helper.succeed();
                });
            });
        });
    }

    /** A ship wider than the gate bumps and stays in deep space. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aShipTooWideForTheGateStaysBehind(GameTestHelper helper) {
        BlockPos origin = new BlockPos(112_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        force(space, origin);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, origin);
            buildFrame(space, origin);
            helper.assertTrue(AtmosphericPortals.tryLight(space, origin.below()), "the frame did not light");
            // A bar of WIDTH + 4 stones across X, five blocks north of the pane.
            BlockPos ship = origin.east(WIDTH / 2).above(1).north(5);
            int x = ship.getX(), y = ship.getY(), z = ship.getZ();
            command(server, space, ship, "fill " + (x - WIDTH / 2 - 2) + " " + y + " " + z + " " + (x + WIDTH / 2 + 1) + " " + y + " " + z + " minecraft:stone");
            command(server, space, ship, "sable assemble connected " + x + " " + y + " " + z);
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, ship);
                String name = "vr_test_" + ship.getX();
                ServerSubLevel body = StationGameTests.shipNamed(space, name);
                helper.assertTrue(body != null, "no ship");
                RigidBodyHandle.of(body).addLinearAndAngularVelocity(new Vector3d(0, 0, 3), new Vector3d());
                helper.runAfterDelay(CROSS_TICKS, () -> {
                    ServerSubLevel left = StationGameTests.shipNamed(space, name);
                    ServerSubLevel arrived = StationGameTests.shipNamed(earth, name);
                    if (arrived != null) {
                        command(server, earth, BlockPos.containing(arrived.logicalPose().position().x(), arrived.logicalPose().position().y(), arrived.logicalPose().position().z()), "sable remove @e[name=" + name + ",limit=1]");
                    }
                    if (left != null) {
                        command(server, space, ship, "sable remove @e[name=" + name + ",limit=1]");
                    }
                    helper.assertTrue(left != null && arrived == null, "a ship too wide for the gate crossed: left=" + (left != null) + " arrived=" + (arrived != null));
                    helper.succeed();
                });
            });
        });
    }

    /** Runs {@code then} once the chunk at {@code pos} is loaded, polling each tick up to {@code budget} ticks (then fails). */
    private static void whenLoaded(GameTestHelper helper, ServerLevel level, BlockPos pos, int budget, Runnable then) {
        if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.isLoaded(pos)) {
            helper.runAfterDelay(2, then);
        } else if (budget <= 0) {
            helper.fail("the chunk at " + pos + " never loaded in " + level.dimension().location());
        } else {
            helper.runAfterDelay(1, () -> whenLoaded(helper, level, pos, budget - 1, then));
        }
    }

    private static void force(ServerLevel space, BlockPos origin) {
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                space.setChunkForced((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz, true);
            }
        }
    }

    /** A ring of portal frame around a WIDTH x HEIGHT opening whose bottom-left pane is {@code origin}, width along X. */
    private static void buildFrame(ServerLevel level, BlockPos origin) {
        for (int across = -1; across <= WIDTH; across++) {
            for (int up = -1; up <= HEIGHT; up++) {
                boolean interior = across >= 0 && across < WIDTH && up >= 0 && up < HEIGHT;
                boolean corner = (across == -1 || across == WIDTH) && (up == -1 || up == HEIGHT);
                if (!interior && !corner) {
                    level.setBlock(origin.east(across).above(up), ModBlocks.PORTAL_FRAME.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static HelmBlockEntity helmOf(ServerLevel level, ServerSubLevel ship) {
        for (BlockPos p : StationAssembly.blocksOf(level, ship)) {
            if (level.getBlockEntity(p) instanceof HelmBlockEntity helm) {
                return helm;
            }
        }
        return null;
    }

    /** A ship on Earth flying back out through the gate comes out in deep space still moving (no gravity to stop it there). */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aShipFlyingBackOutKeepsItsVelocityInSpace(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        BlockPos exitSpot = new BlockPos(120_000, 200, 3_000);
        force(space, exitSpot);
        com.voidreclaimer.earth.PortalShape.Rect gate = com.voidreclaimer.earth.PortalTravel.ensureGate(earth, WIDTH, HEIGHT);
        BlockPos gateBase = BlockPos.containing(gate.centerBottom());
        force(earth, gateBase);
        // Earth's chunks take longer to come up than the void's: build only once the gate's chunk is really loaded, and
        // well after the other tests' once-per-run ship clean-ups and the outward test's arrival at this same gate.
        BlockPos ship = gateBase.above(3).south(4);
        helper.runAfterDelay(200, () -> whenLoaded(helper, earth, ship, 200, () -> {
            clearArea(server, space, exitSpot);
            // The way back out is this test's own exit: set it only now, after the outward test's crossing has written
            // its exit to the same planet.
            earth.setData(com.voidreclaimer.registry.ModAttachments.PORTAL_LINKS, earth.getData(com.voidreclaimer.registry.ModAttachments.PORTAL_LINKS)
                    .withExit(new com.voidreclaimer.earth.PortalLinks.Exit(ModDimensions.DEEP_SPACE, Vec3.atBottomCenterOf(exitSpot), net.minecraft.core.Direction.NORTH)));
            // A bar of three stones south of the gate, fast enough north to cross before gravity drops it below the opening.
            int x = ship.getX(), y = ship.getY(), z = ship.getZ();
            java.util.List<String> log = new java.util.ArrayList<>();
            log.addAll(command(server, earth, ship, "fill " + (x - 1) + " " + y + " " + z + " " + (x + 1) + " " + y + " " + z + " minecraft:stone"));
            log.addAll(command(server, earth, ship, "sable assemble connected " + x + " " + y + " " + z));
            helper.runAfterDelay(2, () -> {
                String name = "vr_test_" + ship.getX();
                try {
                    AeronauticsGameTests.nameShip(server, earth, ship);
                } catch (IllegalStateException e) {
                    helper.fail(e.getMessage() + "; setup: " + log);
                    return;
                }
                ServerSubLevel body = StationGameTests.shipNamed(earth, name);
                helper.assertTrue(body != null, "no ship on Earth: " + log);
                RigidBodyHandle.of(body).addLinearAndAngularVelocity(new Vector3d(0, 0, -10), new Vector3d());
                helper.runAfterDelay(CROSS_TICKS, () -> {
                    ServerSubLevel left = StationGameTests.shipNamed(earth, name);
                    ServerSubLevel arrived = StationGameTests.shipNamed(space, name);
                    Vector3d velocity = arrived == null ? new Vector3d() : RigidBodyHandle.of(arrived).getLinearVelocity(new Vector3d());
                    double distance = arrived == null ? -1 : Vec3.atBottomCenterOf(exitSpot).distanceTo(
                            new Vec3(arrived.logicalPose().position().x(), arrived.logicalPose().position().y(), arrived.logicalPose().position().z()));
                    if (arrived != null) {
                        command(server, space, exitSpot, "sable remove @e[name=" + name + ",limit=1]");
                    }
                    if (left != null) {
                        command(server, earth, ship, "sable remove @e[name=" + name + ",limit=1]");
                    }
                    helper.assertTrue(left == null && arrived != null, "the ship did not come back out: left=" + (left != null) + " arrived=" + (arrived != null));
                    helper.assertTrue(velocity.z < -3.0, "the ship lost its speed north: " + velocity);
                    helper.assertTrue(distance < 64, "the ship came out far from the exit: " + distance);
                    helper.succeed();
                });
            });
        }));
    }
}
