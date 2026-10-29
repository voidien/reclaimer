package com.voidreclaimer.api.event;

import com.voidreclaimer.body.BodyData;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Fired on the game bus (server side) after a body changed: a section or module installed or removed, a new look. */
public class BodyChangedEvent extends LivingEvent {
    private final BodyData previous;
    private final BodyData current;

    public BodyChangedEvent(LivingEntity entity, BodyData previous, BodyData current) {
        super(entity);
        this.previous = previous;
        this.current = current;
    }

    public BodyData getPrevious() {
        return previous;
    }

    public BodyData getCurrent() {
        return current;
    }
}
