package com.voidreclaimer.api.event;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * A player's main body was destroyed and has been left as a wreck, on the game bus and the server side. Everything it carried is in the
 * wreck, and the player already wears a new starter body. {@link #getWreck} is the wreck's entity id, which is also its id in the player's
 * list of shells.
 */
public final class MainBodyWreckedEvent extends Event {
    private final ServerPlayer player;
    private final UUID wreck;

    public MainBodyWreckedEvent(ServerPlayer player, UUID wreck) {
        this.player = player;
        this.wreck = wreck;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public UUID getWreck() {
        return wreck;
    }
}
