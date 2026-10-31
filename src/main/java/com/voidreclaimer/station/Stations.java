package com.voidreclaimer.station;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps the per-level {@link StationList} up to date: every loaded Station Core registers itself when its chunk loads,
 * and once a second the list of names and world positions is rebuilt and, when it changed, stored on the level, which
 * syncs it to every player there. Also answers the Replicator's "am I at a station" question from that list.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Stations {
    /** Ticks between refreshes of the station lists. */
    public static final int REFRESH_TICKS = 20;
    private static final Set<StationCoreBlockEntity> CORES = Collections.newSetFromMap(new IdentityHashMap<>());

    private Stations() {}

    /** Server thread only; a removed core is forgotten at the next refresh. */
    static void track(StationCoreBlockEntity core) {
        CORES.add(core);
    }

    /** How many Replicator ticks of progress the player makes per game tick: more within range of a station. */
    public static int replicatorSteps(ServerPlayer player) {
        StationList list = player.serverLevel().getData(ModAttachments.STATIONS);
        return list.nearest(player.position(), Config.STATION_RANGE.get()).isPresent() ? Config.STATION_REPLICATOR_SPEED.get() : 1;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % REFRESH_TICKS != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<StationList.Entry> entries = new ArrayList<>();
            for (Iterator<StationCoreBlockEntity> it = CORES.iterator(); it.hasNext();) {
                StationCoreBlockEntity core = it.next();
                if (core.isRemoved()) {
                    it.remove();
                    continue;
                }
                if (core.getLevel() != level) {
                    continue;
                }
                entries.add(new StationList.Entry(core.stationName(), core.id(), core.worldSpot(), core.ship() != null));
            }
            StationList fresh = new StationList(entries);
            if (!fresh.equals(level.getData(ModAttachments.STATIONS))) {
                level.setData(ModAttachments.STATIONS, fresh);
            }
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        CORES.clear();
    }
}
