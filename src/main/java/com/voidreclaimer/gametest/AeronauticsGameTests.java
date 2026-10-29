package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create Aeronautics / Sable inside deep space. Separate from the main GameTests: run with
 * {@code ./gradlew runGameTestAeronautics} (its own namespace {@value #NAMESPACE}), so an upstream problem shows up
 * as its own CI job.
 *
 * <p>Sub-levels are spawned and read through Sable's own commands, so these tests don't compile against Sable.
 */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AeronauticsGameTests {
    public static final String NAMESPACE = VoidReclaimer.MOD_ID + "_aero";

    private static final Pattern POSITION = Pattern.compile("Position: (-?[\\d.]+) (-?[\\d.]+) (-?[\\d.]+)");
    private static final int WATCH_TICKS = 100;
    /** A sub-level that sits still reads within this many blocks of where it started. */
    private static final double STILL = 1.0;

    private AeronauticsGameTests() {}

    /** The shipped dimension_physics file keeps a platform from falling in deep space. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void platformStaysPutInDeepSpace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(78_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            List<String> spawn = command(server, space, pos, "sable spawn platform 2 minecraft:stone");
            helper.runAfterDelay(WATCH_TICKS, () -> finish(helper, space, pos, command(server, space, pos, "sable info @n"), spawn));
        });
    }

    /** Aeronautics propellers need air: in vacuum a ship with a running propeller must not move. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void propellerGivesNoThrustInVacuum(GameTestHelper helper) {
        BlockPos pos = new BlockPos(79_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            log.addAll(command(server, space, pos, "fill " + (x - 1) + " " + y + " " + (z - 1) + " " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:stone"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 1) + " " + z + " create:creative_motor[facing=up]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 2) + " " + z + " aeronautics:andesite_propeller[facing=up]"));
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                helper.runAfterDelay(WATCH_TICKS, () -> finish(helper, space, pos, command(server, space, pos, "sable info @n"), log));
            });
        });
    }

    /** Deep space has asteroids now, and a fresh world may put one where a test wants its sub-level. */
    private static void clearArea(MinecraftServer server, ServerLevel space, BlockPos pos) {
        space.getChunk(pos);
        command(server, space, pos, "fill " + (pos.getX() - 10) + " " + (pos.getY() - 10) + " " + (pos.getZ() - 10) + " "
                + (pos.getX() + 10) + " " + (pos.getY() + 10) + " " + (pos.getZ() + 10) + " minecraft:air");
    }

    private static void finish(GameTestHelper helper, ServerLevel space, BlockPos start, List<String> info, List<String> setup) {
        Vec3 now = position(info);
        if (now == null) {
            helper.fail("no sub-level found near " + start + "; setup: " + setup + "; info: " + info);
            return;
        }
        double drift = now.distanceTo(Vec3.atCenterOf(start));
        if (drift > STILL) {
            helper.fail("sub-level moved " + String.format("%.2f", drift) + " blocks from " + start + " to " + now);
            return;
        }
        helper.succeed();
    }

    private static Vec3 position(List<String> info) {
        for (String line : info) {
            Matcher m = POSITION.matcher(line);
            if (m.find()) {
                return new Vec3(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)));
            }
        }
        return null;
    }

    private static List<String> command(MinecraftServer server, ServerLevel level, BlockPos pos, String command) {
        List<String> out = new ArrayList<>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                out.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = server.createCommandSourceStack()
                .withSource(sink)
                .withLevel(level)
                .withPosition(Vec3.atCenterOf(pos))
                .withPermission(4);
        server.getCommands().performPrefixedCommand(source, command);
        return out;
    }
}
