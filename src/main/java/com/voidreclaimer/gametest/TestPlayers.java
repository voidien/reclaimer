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
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestPlayers() {}

    /** Joins a player through the normal login path (game test worlds default to creative). */
    static ServerPlayer join(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "vr_test_" + COUNTER.incrementAndGet()), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // The fake connection skips NeoForge's channel negotiation; declare every registered play payload, ours and
        // other mods' (Create sends its own on login). Internal NeoForge API, used only by tests.
        ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(playPayloadIds());
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
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
