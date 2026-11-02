package com.voidreclaimer.gametest;

import com.mojang.authlib.GameProfile;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.network.SensorEchoPayload;
import com.voidreclaimer.space.ModDimensions;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Real {@link ServerPlayer}s on a fake connection for GameTests. Unlike vanilla's mock player they use the real game
 * mode, so survival rules apply. Their connection is not ticked by the server: call {@link ServerPlayer#doTick()}
 * to run a player tick (which fires {@code PlayerTickEvent}).
 */
final class TestPlayers {
    private static final int SPAWN_PROTECTION_TICKS = 60;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestPlayers() {}

    /** Joins a player through the normal login path (game test worlds default to creative). */
    static ServerPlayer join(GameTestHelper helper) {
        return join(helper, new GameProfile(UUID.randomUUID(), "vr_test_" + COUNTER.incrementAndGet()));
    }

    /**
     * Logs {@code player} out and back in through the real save: the same account, a new {@code ServerPlayer} read from its
     * player file. The old object must not be used after this.
     */
    static ServerPlayer rejoin(GameTestHelper helper, ServerPlayer player) {
        GameProfile profile = player.getGameProfile();
        remove(player);
        return join(helper, profile);
    }

    /** The same account every time, for a test that spans a restart: its player file is what is being checked. */
    static ServerPlayer joinAs(GameTestHelper helper, UUID id) {
        return join(helper, new GameProfile(id, "vr_test_restart"));
    }

    private static ServerPlayer join(GameTestHelper helper, GameProfile profile) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        // An EmbeddedChannel keeps every outbound message until something reads it, so each packet the server sends a
        // test player (every chunk, every sync) would stay in memory for the whole run; CI ran out of heap that way.
        // Nothing reads them: drop them at the head of the pipeline.
        channel.pipeline().addFirst("drop_outbound", new io.netty.channel.ChannelOutboundHandlerAdapter() {
            @Override
            public void write(io.netty.channel.ChannelHandlerContext context, Object message, io.netty.channel.ChannelPromise promise) {
                io.netty.util.ReferenceCountUtil.release(message);
                promise.setSuccess();
            }
        });
        // The fake connection skips NeoForge's channel negotiation; declare every registered play payload, ours and
        // other mods' (Create sends its own on login). Internal NeoForge API, used only by tests.
        ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(playPayloadIds());
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        // Awakening grants the starter dust. A Dyson tier change checks every online player's milestones, so a player who had
        // not reached it would get dust at a moment set by whichever test changes the tier: that made exact dust checks flaky.
        com.voidreclaimer.replicator.Replicator.store(player,
                com.voidreclaimer.replicator.Replicator.data(player).withUnlocked(com.voidreclaimer.replicator.Milestones.AWAKENING));
        return player;
    }

    /** For a test about the first moments of a game: the player has not reached Awakening yet. Tests start past it, see {@link #join}. */
    static void notYetAwake(ServerPlayer player) {
        com.voidreclaimer.replicator.Replicator.store(player,
                com.voidreclaimer.replicator.Replicator.data(player).withoutUnlocked(com.voidreclaimer.replicator.Milestones.AWAKENING));
    }

    @SuppressWarnings("unchecked")
    private static Set<ResourceLocation> playPayloadIds() {
        try {
            Field field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            field.setAccessible(true);
            Map<ConnectionProtocol, Map<ResourceLocation, ?>> registrations = (Map<ConnectionProtocol, Map<ResourceLocation, ?>>) field.get(null);
            Set<ResourceLocation> ids = new HashSet<>(registrations.getOrDefault(ConnectionProtocol.PLAY, Map.of()).keySet());
            ids.add(SyncAttachmentsPayload.TYPE.id());
            ids.add(SensorEchoPayload.TYPE.id());
            return ids;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Can't read NeoForge payload registrations", e);
        }
    }

    /** A new player can't be hurt for three seconds, and a test doesn't wait that long: runs those ticks of the player's own clock at once. */
    static void endSpawnProtection(ServerPlayer player) {
        for (int tick = 0; tick < SPAWN_PROTECTION_TICKS; tick++) {
            player.tick();
        }
    }

    /** A survival player floating in deep space at the given position. */
    static ServerPlayer survivalInDeepSpace(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer player = join(helper);
        player.teleportTo(deepSpace(helper), x, y, z, 0f, 0f);
        // A real client acknowledges the dimension change; until then the player can't be hurt.
        player.hasChangedDimension();
        // Bypass the game mode event so nothing else reacts; the player is already where the test wants it.
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    /** A survival player standing in the air over ruined Earth at the given position. */
    static ServerPlayer survivalOnRuinedEarth(GameTestHelper helper, double x, double y, double z) {
        ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        if (earth == null) {
            throw new IllegalStateException("ruined_earth dimension is not loaded");
        }
        ServerPlayer player = join(helper);
        earth.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        player.teleportTo(earth, x, y, z, 0f, 0f);
        player.hasChangedDimension();
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    /**
     * Installs a module straight onto the body's {@code index}-th mount of {@code kind} (bypassing the inventory and
     * the rules), e.g. a Dust Sensor or a tool.
     */
    static void installModule(ServerPlayer player, ModuleKind kind, int index, ItemStack module) {
        BodyView view = Bodies.view(player);
        BodyData.Mount mount = view.mounts().stream().filter(m -> m.kind() == kind).toList().get(index);
        Bodies.store(player, view.data().withModule(mount, module));
    }

    static ServerLevel deepSpace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(ModDimensions.DEEP_SPACE);
        if (level == null) {
            throw new IllegalStateException("deep_space dimension is not loaded");
        }
        return level;
    }

    static void remove(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
    }
}
