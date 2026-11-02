package com.voidreclaimer.flight;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runs each body's Afterburner on the server, which owns the clock; the client only says the key is held, and reads the result for its top speed
 * through {@link FlightStats#of}. A burn needs thrust held and energy to spare.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Afterburners {
    private Afterburners() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    /** One tick of {@code player}'s Afterburner. */
    public static void tick(ServerPlayer player) {
        AfterburnerState before = player.getData(ModAttachments.AFTERBURNER);
        ModuleStats.Afterburner spec = best(player);
        if (spec == null && before.equals(AfterburnerState.COLD)) {
            return;
        }
        FlightInput input = player.getData(ModAttachments.FLIGHT_INPUT);
        boolean wanted = ThrusterFlight.isActive(player) && input.boost() && input.isThrusting() && Energy.canPowerSystems(player);
        AfterburnerState after = before.tick(wanted, spec);
        if (after.burning()) {
            Energy.consume(player, spec.energyPerSecond() / (float) FlightStats.TICKS_PER_SECOND);
        }
        if (!after.equals(before)) {
            player.setData(ModAttachments.AFTERBURNER, after);
        }
    }

    /** The strongest Afterburner on the body; two don't add up. */
    private static ModuleStats.Afterburner best(ServerPlayer player) {
        ModuleStats.Afterburner best = null;
        for (ItemStack stack : Bodies.data(player).modules().values()) {
            ModuleStats module = Parts.module(stack);
            if (module != null && module.afterburner().isPresent() && (best == null || module.afterburner().get().speedFactor() > best.speedFactor())) {
                best = module.afterburner().get();
            }
        }
        return best;
    }
}
