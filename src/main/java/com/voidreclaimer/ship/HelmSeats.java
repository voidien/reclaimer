package com.voidreclaimer.ship;

import com.voidreclaimer.VoidReclaimer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives every live Helm seat once per server tick, whatever its chunk is doing. An entity only ticks (and only shows up
 * in entity lookups) while its chunk is entity-ticking, which a chunk may not be for a while after it loads, and a test
 * player never makes it so; the Helm that spawned the seat can be dropped with its ship's plot without a word. The seat
 * still has to follow its Helm, or leave when the Helm is gone, so it is done from here.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class HelmSeats {
    private static final Set<HelmSeat> SEATS = Collections.newSetFromMap(new IdentityHashMap<>());

    private HelmSeats() {}

    /** Server thread only. A seat is forgotten once it is removed. */
    static void track(HelmSeat seat) {
        if (!seat.level().isClientSide()) {
            SEATS.add(seat);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<HelmSeat> it = SEATS.iterator(); it.hasNext();) {
            HelmSeat seat = it.next();
            if (seat.isRemoved()) {
                it.remove();
            } else {
                seat.follow();
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        SEATS.clear();
    }
}
