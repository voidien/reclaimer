package com.voidreclaimer.link;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.ship.ShipTickets;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps each level's {@link RelayList} true. A relay writes its own row while it ticks, and once a second every row is held up against
 * the block. Only a loaded chunk can say a relay has gone: a chunk that is not loaded says nothing, and its row stays. A ship that is
 * removed takes its relays with it, and no block breaks to say so, so Sable tells us.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Relays {
    private static final int CHECK_TICKS = 20;

    private Relays() {}

    /** Called when the mod is constructed, before any level has a ship. */
    public static void init() {
        SableEventPlatform.INSTANCE.onSubLevelContainerReady((level, container) -> {
            if (level instanceof ServerLevel server) {
                container.addObserver(new SubLevelObserver() {
                    @Override
                    public void onSubLevelRemoved(SubLevel ship, SubLevelRemovalReason reason) {
                        if (reason == SubLevelRemovalReason.REMOVED && server.hasData(ModAttachments.RELAYS)) {
                            server.setData(ModAttachments.RELAYS, of(server).withoutShip(ship.getUniqueId()));
                        }
                    }
                });
            }
        });
    }

    public static RelayList of(ServerLevel level) {
        return level.getData(ModAttachments.RELAYS);
    }

    public static void note(ServerLevel level, RelayList.Entry entry) {
        RelayList list = of(level);
        list.find(entry.id()).flatMap(RelayList.Entry::ship).filter(ship -> entry.ship().filter(ship::equals).isEmpty())
                .ifPresent(ship -> letGo(level, ship, entry.id()));
        RelayList updated = list.with(entry);
        if (updated != list) {
            level.setData(ModAttachments.RELAYS, updated);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_TICKS == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                if (level.hasData(ModAttachments.RELAYS)) {
                    forgetGone(level);
                }
            }
        }
    }

    private static void forgetGone(ServerLevel level) {
        RelayList list = of(level);
        RelayList kept = list;
        for (RelayList.Entry entry : list.relays()) {
            if (canCheck(level, entry) && !isThere(level, entry)) {
                kept = kept.without(entry.id());
                entry.ship().ifPresent(ship -> letGo(level, ship, entry.id()));
            }
        }
        if (kept != list) {
            level.setData(ModAttachments.RELAYS, kept);
        }
    }

    /** A relay that has left a ship, or gone, stops holding it. Sable saves tickets, so one left behind would keep the ship loaded for good. */
    private static void letGo(ServerLevel level, UUID shipId, UUID relay) {
        if (SubLevelContainer.getContainer(level).getSubLevel(shipId) instanceof ServerSubLevel ship) {
            ShipTickets.release(level, ship, relay);
        }
    }

    /** A row aboard a ship can only be checked while that ship is there: another ship can have taken over its plot. */
    private static boolean canCheck(ServerLevel level, RelayList.Entry entry) {
        if (!level.isLoaded(entry.block())) {
            return false;
        }
        if (entry.ship().isEmpty()) {
            return true;
        }
        ServerSubLevel ship = Ships.shipAt(level, entry.block());
        return ship != null && ship.getUniqueId().equals(entry.ship().get());
    }

    private static boolean isThere(ServerLevel level, RelayList.Entry entry) {
        return level.getBlockEntity(entry.block()) instanceof RelayBlockEntity relay && relay.id().equals(entry.id());
    }
}
