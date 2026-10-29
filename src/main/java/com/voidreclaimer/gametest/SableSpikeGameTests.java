package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.ZeroGravity;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Spike only (branch claude/aeronautics-spike, never merged): does a Sable sub-level stay put with zero
 * physics gravity? Each test spawns a 5x5 platform through Sable's own command and logs its pose every
 * 10 ticks. Run with -PspikeNoZeroG=true to switch our zero-G systems off.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SableSpikeGameTests {
    private static final Pattern POSITION = Pattern.compile("Position: (-?[\\d.]+) (-?[\\d.]+) (-?[\\d.]+)");
    private static final int WATCH_TICKS = 200;

    private SableSpikeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableOverworld(GameTestHelper helper) {
        run(helper, Level.OVERWORLD, new BlockPos(3_000, 200, 3_000));
    }

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableDeepSpace(GameTestHelper helper) {
        run(helper, ModDimensions.DEEP_SPACE, new BlockPos(3_000, 200, 3_000));
    }

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableShortDimension(GameTestHelper helper) {
        run(helper, ModDimensions.SPIKE_SHORT, new BlockPos(3_000, 200, 3_000));
    }

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableDeepSpacePlayerOnTop(GameTestHelper helper) {
        runWithPlayer(helper, ModDimensions.DEEP_SPACE, new BlockPos(3_100, 200, 3_000), 1.6);
    }

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableDeepSpacePlayerInside(GameTestHelper helper) {
        runWithPlayer(helper, ModDimensions.DEEP_SPACE, new BlockPos(3_200, 200, 3_000), 0.0);
    }

    @GameTest(template = "empty", timeoutTicks = 400, required = false)
    public static void sableShortPlayerInside(GameTestHelper helper) {
        runWithPlayer(helper, ModDimensions.SPIKE_SHORT, new BlockPos(3_200, 200, 3_000), 0.0);
    }

    /** As {@link #run}, with a survival test player ticking at the platform (dy above its centre). */
    private static void runWithPlayer(GameTestHelper helper, ResourceKey<Level> key, BlockPos pos, double dy) {
        ServerLevel level = helper.getLevel().getServer().getLevel(key);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + dy, pos.getZ() + 0.5);
        if (level != null && player.level() != level) {
            player.teleportTo(level, pos.getX() + 0.5, pos.getY() + dy, pos.getZ() + 0.5, 0f, 0f);
            player.hasChangedDimension();
        }
        helper.onEachTick(player::doTick);
        helper.runAfterDelay(5 + WATCH_TICKS - 1, () -> {
            VoidReclaimer.LOGGER.info("[SableSpike player] {} at {} after watch", key.location(), player.position());
            TestPlayers.remove(player);
        });
        run(helper, key, pos);
    }

    private static void run(GameTestHelper helper, ResourceKey<Level> key, BlockPos pos) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.getLevel(key);
        String tag = "[SableSpike " + key.location() + (ZeroGravity.SPIKE_NO_ZERO_G ? " noZeroG" : "") + "]";
        if (level == null) {
            helper.fail(tag + " dimension not loaded");
            return;
        }
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            List<String> spawn = command(server, level, pos, "sable spawn platform 2 minecraft:stone");
            VoidReclaimer.LOGGER.info("{} spawn: {}", tag, spawn);
            List<Vec3> track = new ArrayList<>();
            for (int t = 10; t <= WATCH_TICKS; t += 10) {
                int tick = t;
                helper.runAfterDelay(tick, () -> {
                    List<String> info = command(server, level, pos, "sable info @n");
                    VoidReclaimer.LOGGER.info("{} t={} info: {}", tag, tick, String.join(" | ", info));
                    track.add(position(info));
                    if (tick == WATCH_TICKS) {
                        finish(helper, tag, pos, track);
                    }
                });
            }
        });
    }

    private static void finish(GameTestHelper helper, String tag, BlockPos pos, List<Vec3> track) {
        Vec3 start = Vec3.atCenterOf(pos);
        Vec3 last = track.getLast();
        long missing = track.stream().filter(p -> p == null).count();
        String summary = tag + " samples=" + track.size() + " missing=" + missing + " last=" + last
                + (last == null ? "" : " drift=" + String.format("%.2f", last.distanceTo(start)));
        VoidReclaimer.LOGGER.info("{} RESULT {}", tag, summary);
        if (last == null) {
            helper.fail(summary + " (sub-level gone)");
        } else if (last.distanceTo(start) > 4) {
            helper.fail(summary + " (sub-level moved)");
        } else {
            helper.succeed();
        }
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
