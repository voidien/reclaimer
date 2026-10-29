package com.voidreclaimer.body;

import com.voidreclaimer.energy.Energy;
import net.minecraft.world.entity.player.Player;

/**
 * Main-inventory rows unlocked by the body: the hotbar always, then one row per frame base row and storage row
 * ({@link BodyStats#rows()}). Locked slots can't take new items (they can still be emptied, so nothing is ever
 * trapped). Creative and spectator players are not limited. Enforced by {@code InventoryLockMixin} and
 * {@code SlotLockMixin}.
 */
public final class InventoryLock {
    /** Hotbar slots 0..8 are always usable. */
    private static final int HOTBAR = 9;
    /** Player main inventory size (hotbar + 3 rows). */
    private static final int MAIN = 36;

    private InventoryLock() {}

    /** Whether main-inventory slot {@code index} (0..35) is locked. Other indices (armor, offhand) never are. */
    public static boolean isLocked(Player player, int index) {
        if (index < HOTBAR || index >= MAIN || Energy.isExempt(player)) {
            return false;
        }
        return index >= Bodies.stats(player).inventorySlots();
    }
}
