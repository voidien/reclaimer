package com.voidreclaimer.client.render.holo;

import com.voidreclaimer.display.HoloProjectorBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/** What was worked out for a projector, kept until its content changes (its revision moves). Client render thread only. */
final class HoloCaches {
    private record Entry<T>(int revision, T value) {}

    private static final Map<HoloProjectorBlockEntity, Entry<HoloTextLayout>> LAYOUTS = new WeakHashMap<>();

    private HoloCaches() {}

    static HoloTextLayout layout(HoloProjectorBlockEntity projector, Function<HoloProjectorBlockEntity, HoloTextLayout> build) {
        Entry<HoloTextLayout> entry = LAYOUTS.get(projector);
        if (entry == null || entry.revision != projector.revision()) {
            entry = new Entry<>(projector.revision(), build.apply(projector));
            LAYOUTS.put(projector, entry);
        }
        return entry.value;
    }
}
