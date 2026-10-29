package com.voidreclaimer.dyson;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.space.ModDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Changes Dyson Sphere progress. Launchers (the Kinetic Mass Driver, later) call {@link #launchSatellites} and
 * {@link #launchTrusses}; a tier change re-checks milestones for everyone so the AI log entries appear.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Dyson {
    private Dyson() {}

    /** Adds launched Dyson Swarm Satellite Payloads. Returns the new data, or the current data in a world without deep space. */
    public static DysonSphereData launchSatellites(MinecraftServer server, long count) {
        DysonSphereData data = DysonSphereData.of(server);
        return set(server, data.withSatellites(data.satellites() + count));
    }

    /** Adds launched Dyson Shell Truss Modules. */
    public static DysonSphereData launchTrusses(MinecraftServer server, long count) {
        DysonSphereData data = DysonSphereData.of(server);
        return set(server, data.withTrusses(data.trusses() + count));
    }

    /** Replaces both counters (commands, tests) and refreshes the thresholds from the config. */
    public static DysonSphereData set(MinecraftServer server, DysonSphereData data) {
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        DysonSphereData before = DysonSphereData.of(server);
        DysonSphereData updated = data.withThresholds(Config.DYSON_LATTICE_START.get(), Config.DYSON_SHELL_TOTAL.get());
        if (space == null) {
            return updated;
        }
        space.setData(ModAttachments.DYSON_SPHERE, updated);
        if (updated.tier() != before.tier()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Replicator.checkMilestones(player);
            }
        }
        return updated;
    }

    /** The saved thresholds can be stale after a config edit; bring them up to date whenever the level loads. */
    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && ModDimensions.isDeepSpace(level)) {
            DysonSphereData data = level.getData(ModAttachments.DYSON_SPHERE);
            level.setData(ModAttachments.DYSON_SPHERE,
                    data.withThresholds(Config.DYSON_LATTICE_START.get(), Config.DYSON_SHELL_TOTAL.get()));
        }
    }
}
