package com.voidreclaimer.link;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.registry.ModAttachments;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The live half of signal: reads the world for {@link RelayGraph} and acts on what it says. Only a link from a seat has a signal; a
 * link made any other way is a debug tool's. A seat's place is the link's {@code from}, which a ship keeps current.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Signal {
    private static final int EVERY_TICKS = 20;
    private static final float CLEAN = 1f;
    /** The HUD need not hear of less. */
    private static final float WORTH_SENDING = 0.01f;

    private Signal() {}

    /** How far a seat at {@code seat} reaches now, in blocks. */
    public static double range(MinecraftServer server, LinkState.Place seat) {
        return SignalMath.fromConfig().seatRange(DysonSphereData.of(server).tier(), Links.atStation(server, seat));
    }

    /** The signal from a seat at {@code seat} at {@code target}. */
    public static RelayGraph.Reading read(MinecraftServer server, LinkState.Place seat, LinkState.Place target) {
        RelayGraph.Node node = new RelayGraph.Node(new RelayGraph.Place(seat.dimension(), seat.position()), range(server, seat));
        double band = SignalMath.fromConfig().staticBand(Links.atStation(server, seat));
        return new RelayGraph(List.of(), List.of()).signal(node, new RelayGraph.Place(target.dimension(), target.position()), band);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % EVERY_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                check(player);
            }
        }
    }

    private static void check(ServerPlayer player) {
        Optional<LinkState.Link> link = Links.state(player).link().filter(found -> found.seat().isPresent());
        if (link.isEmpty()) {
            show(player, CLEAN);
            return;
        }
        RelayGraph.Reading reading = read(player.server, link.get().from(), Links.place(player));
        if (reading.reachable()) {
            show(player, (float) reading.strength());
        } else {
            show(player, CLEAN);
            Links.requestWake(player, result -> {
                if (result == Links.Result.WOKEN) {
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.link.signal_lost"), false);
                }
            });
        }
    }

    private static void show(ServerPlayer player, float strength) {
        if (Math.abs(player.getData(ModAttachments.LINK_SIGNAL) - strength) >= WORTH_SENDING) {
            player.setData(ModAttachments.LINK_SIGNAL, strength);
            player.syncData(ModAttachments.LINK_SIGNAL);
        }
    }
}
