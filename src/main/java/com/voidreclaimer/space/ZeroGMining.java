package com.voidreclaimer.space;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Vanilla divides a player's mining speed by 5 when they are not standing on something. In zero-G nobody ever is, so
 * every block took five times as long: a Scaffold about six seconds and a Tether Anchor eleven by hand. Where there is
 * no gravity the penalty is undone, so mining is as fast as on the ground.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ZeroGMining {
    /** Vanilla's divisor for a player who is not on the ground. */
    public static final float AIRBORNE_PENALTY = 5f;

    private ZeroGMining() {}

    /** High priority, so the tool modules and the asteroid rules (which set or zero the speed) see the corrected value. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.onGround() && event.getPosition().filter(pos -> ZeroGravity.isZeroG(player.level(), pos)).isPresent()) {
            event.setNewSpeed(event.getNewSpeed() * AIRBORNE_PENALTY);
        }
    }
}
