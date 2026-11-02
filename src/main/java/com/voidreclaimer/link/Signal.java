package com.voidreclaimer.link;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.ShellLinkEvent;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
        RelayGraph.Reading reading = new RelayGraph(relays(server), List.of()).signal(node, new RelayGraph.Place(target.dimension(), target.position()), band);
        return reading.reachable() ? underStorm(server, target, reading) : reading;
    }

    /** A storm where the far end is thins the signal, and a link it takes down to nothing is lost, with the usual warning first. */
    private static RelayGraph.Reading underStorm(MinecraftServer server, LinkState.Place target, RelayGraph.Reading reading) {
        ServerLevel level = server.getLevel(target.dimension());
        double loss = level == null ? 0.0 : com.voidreclaimer.weather.SpaceWeatherTicker.signalLoss(level);
        double strength = SignalMath.underStorm(reading.strength(), loss);
        return loss <= 0.0 ? reading : strength > 0.0 ? new RelayGraph.Reading(true, strength, reading.hops()) : RelayGraph.Reading.NONE;
    }

    /** How far a relay reaches now, in blocks. */
    public static double relayRange(MinecraftServer server) {
        return SignalMath.fromConfig().relayRange(DysonSphereData.of(server).tier());
    }

    /** Every relay with power, in every level. One that is not loaded counts as it was last seen. */
    private static List<RelayGraph.Node> relays(MinecraftServer server) {
        double range = relayRange(server);
        List<RelayGraph.Node> nodes = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.hasData(ModAttachments.RELAYS)) {
                for (RelayList.Entry entry : Relays.of(level).relays()) {
                    if (entry.lit()) {
                        nodes.add(new RelayGraph.Node(new RelayGraph.Place(level.dimension(), entry.position()), range));
                    }
                }
            }
        }
        return nodes;
    }

    /** What a player at a seat can see of their shells: everything on their list that has not gone. */
    public static List<ShellRow> rows(ServerPlayer player) {
        LinkState.Place here = Links.place(player);
        List<ShellRow> rows = new ArrayList<>();
        int number = 0;
        for (LinkState.KnownShell known : Links.state(player).shells()) {
            number++;
            if (!known.gone()) {
                rows.add(ShellRow.of(known, number, here, read(player.server, here, known.at())));
            }
        }
        return rows;
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
            losing(player, 0);
            return;
        }
        RelayGraph.Reading reading = read(player.server, link.get().from(), Links.place(player));
        if (reading.reachable()) {
            show(player, (float) reading.strength());
            losing(player, 0);
            return;
        }
        show(player, 0f);
        int left = SignalMath.countdown(player.getData(ModAttachments.LINK_LOSING), Config.LINK_SIGNAL_GRACE.get());
        losing(player, left);
        if (left == 0) {
            Links.requestWake(player, ShellLinkEvent.Reason.SIGNAL_LOST, result -> {
                if (result == Links.Result.WOKEN) {
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.link.signal_lost"), false);
                }
            });
        }
    }

    private static void losing(ServerPlayer player, int seconds) {
        if (player.getData(ModAttachments.LINK_LOSING) != seconds) {
            player.setData(ModAttachments.LINK_LOSING, seconds);
            player.syncData(ModAttachments.LINK_LOSING);
        }
    }

    private static void show(ServerPlayer player, float strength) {
        if (Math.abs(player.getData(ModAttachments.LINK_SIGNAL) - strength) >= WORTH_SENDING) {
            player.setData(ModAttachments.LINK_SIGNAL, strength);
            player.syncData(ModAttachments.LINK_SIGNAL);
        }
    }
}
