package com.voidreclaimer.debug;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Times whoever flies the {@code speedway} scene and says each split in chat. A debug tool: nothing here is saved. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Speedway {
    private static List<GateTimer.Gate> gates = List.of();
    private static final Map<UUID, GateTimer> RUNS = new HashMap<>();

    private Speedway() {}

    /** Starts timing these gates, forgetting every run so far. */
    static void open(List<GateTimer.Gate> course) {
        gates = List.copyOf(course);
        RUNS.clear();
    }

    /** Stops timing. */
    public static void close() {
        open(List.of());
    }

    /** This player's timer, or null before they have been seen on a course. */
    @Nullable
    public static GateTimer run(ServerPlayer player) {
        return RUNS.get(player.getUUID());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!gates.isEmpty() && event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(ModDimensions.DEEP_SPACE)) {
            GateTimer.Event happened = RUNS.computeIfAbsent(player.getUUID(), id -> new GateTimer(gates))
                    .update(player.level().getGameTime(), player.position());
            if (happened != null) {
                player.sendSystemMessage(Component.literal(text(happened)));
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        close();
    }

    static String text(GateTimer.Event event) {
        return switch (event.kind()) {
            case STARTED -> String.format(Locale.ROOT, "Speedway: go at %.1f m/s", event.speed());
            case GATE -> String.format(Locale.ROOT, "Gate %d of %d: %.2f s, %.1f m/s", event.gate(), event.gates(), event.seconds(), event.speed());
            case MISSED -> String.format(Locale.ROOT, "Missed gate %d of %d at %.2f s", event.gate(), event.gates(), event.seconds());
            case FINISHED -> String.format(Locale.ROOT, "Finish: %.2f s at %.1f m/s, best %.2f s", event.seconds(), event.speed(), event.best());
        };
    }
}
