package com.voidreclaimer.ship;

import com.voidreclaimer.VoidReclaimer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

/**
 * Keeps a ship loaded with nobody near it, as long as something holds it. A ship left alone goes into storage and can no longer
 * be read, which something that depends on a ship from far away cannot allow. Sable saves the tickets itself.
 */
public final class ShipTickets {
    /** Sable finds this by name when it loads its tickets, so it has to exist by then: see {@link #init}. */
    private static final SubLevelLoadingTicketType<UUID> HOLD = SubLevelLoadingTicketType.create(VoidReclaimer.id("hold"), UUIDUtil.CODEC);

    private ShipTickets() {}

    /** Called when the mod is constructed, before any world loads: this class creating its ticket type is the point. */
    public static void init() {}

    /** Holds {@code ship} for {@code holder}, a stable id of whatever needs it. True if it was not held by them already. */
    public static boolean hold(ServerLevel level, ServerSubLevel ship, UUID holder) {
        return container(level).addForceLoadTicket(ship, HOLD, holder);
    }

    /** Lets go of {@code ship}. True if {@code holder} held it. */
    public static boolean release(ServerLevel level, ServerSubLevel ship, UUID holder) {
        return container(level).removeForceLoadTicket(ship, HOLD, holder);
    }

    private static ServerSubLevelContainer container(ServerLevel level) {
        return (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
    }
}
