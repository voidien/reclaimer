package com.voidreclaimer.api.event;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyStats;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Fired on the game bus on both sides when a body's stats are computed: after any change to the body, and once per
 * session. Replace the stats to change them; the result is cached until the body changes.
 */
public class BodyStatsEvent extends LivingEvent {
    private final BodyData body;
    private BodyStats stats;

    public BodyStatsEvent(LivingEntity entity, BodyData body, BodyStats stats) {
        super(entity);
        this.body = body;
        this.stats = stats;
    }

    public BodyData getBody() {
        return body;
    }

    public BodyStats getStats() {
        return stats;
    }

    public void setStats(BodyStats stats) {
        this.stats = stats;
    }
}
