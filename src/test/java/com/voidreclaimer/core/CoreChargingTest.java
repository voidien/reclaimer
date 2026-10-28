package com.voidreclaimer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class CoreChargingTest {
    @Test
    void sunlightAddsAChargeAfterEnoughLitTicks() {
        CoreCharging.State state = new CoreCharging.State(0, 0);
        int updates = 0;
        while (state.charges() == 0) {
            state = CoreCharging.sunTick(state.charges(), state.progress(), 20, 15, 12_000);
            updates++;
        }
        assertEquals(600, updates, "12000 ticks at full light");
        // Deep space's dim sun (10/15) takes 1.5x as long.
        state = new CoreCharging.State(0, 0);
        updates = 0;
        while (state.charges() == 0) {
            state = CoreCharging.sunTick(state.charges(), state.progress(), 20, 10, 12_000);
            updates++;
        }
        assertEquals(900, updates);
    }

    @Test
    void darknessAndFullCoresDontCharge() {
        assertEquals(new CoreCharging.State(1, 50), CoreCharging.sunTick(1, 50, 20, 0, 12_000));
        assertEquals(new CoreCharging.State(CoreCharging.MAX_CHARGES, 0),
                CoreCharging.sunTick(CoreCharging.MAX_CHARGES, 500, 20, 15, 12_000));
    }

    @Test
    void coreListAddsReplacesRemovesAndValidatesSelection() {
        GlobalPos a = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 2, 3));
        GlobalPos b = GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 5, 6));
        BackupCoreList list = BackupCoreList.EMPTY
                .with(new BackupCoreList.Entry(a, "Alpha", 1))
                .with(new BackupCoreList.Entry(b, "Beta", 0))
                .with(new BackupCoreList.Entry(a, "Alpha", 3));
        assertEquals(2, list.cores().size());
        assertEquals(3, list.get(a).orElseThrow().charges());
        assertFalse(list.isFull());

        GlobalPos unknown = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        assertTrue(list.withSelected(Optional.of(unknown)).selected().isEmpty(), "can't select a core you don't own");
        BackupCoreList selected = list.withSelected(Optional.of(b));
        assertEquals(Optional.of(b), selected.selected());
        assertTrue(selected.without(b).selected().isEmpty(), "removing the selected core clears the choice");
        assertTrue(list.with(new BackupCoreList.Entry(unknown, "Gamma", 0)).isFull());
    }
}
