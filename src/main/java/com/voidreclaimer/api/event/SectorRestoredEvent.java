package com.voidreclaimer.api.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Fired on the game bus (server side) after a sector of ruined Earth first counts as restored: its Genesis Pulse has
 * finished and the sector map and sky era have moved on. Sectors are numbered {@code row * 4 + column} from the north-west.
 */
public class SectorRestoredEvent extends Event {
    private final ServerLevel level;
    private final int sector;
    private final int restored;

    public SectorRestoredEvent(ServerLevel level, int sector, int restored) {
        this.level = level;
        this.sector = sector;
        this.restored = restored;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public int getSector() {
        return sector;
    }

    /** How many sectors are restored now, this one included. */
    public int getRestored() {
        return restored;
    }
}
