package com.voidreclaimer.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Ruined Earth's air is about to wear a body for a second, on the game bus and the server side. Cancel it for a part that filters the air. */
public final class ToxicAirEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;

    public ToxicAirEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
