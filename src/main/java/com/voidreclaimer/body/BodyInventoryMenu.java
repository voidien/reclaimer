package com.voidreclaimer.body;

import com.voidreclaimer.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The body's inventory, opened with E in place of the vanilla inventory: the three main rows and the hotbar and nothing
 * else (no armor, offhand or crafting slots). Rows the body has not unlocked stay in the menu but cannot take items
 * ({@link InventoryLock}); the screen greys them out. Menu slots 0..26 are the main rows (inventory slots 9..35), 27..35 the hotbar.
 */
public class BodyInventoryMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9;
    public static final int MAIN_SLOTS = 27;
    public static final int HOTBAR_SLOTS = 9;
    /** Where the slot grid starts inside the panel, and the gap between the main rows and the hotbar. */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 44;
    public static final int HOTBAR_GAP = 6;

    private final Player owner;

    public BodyInventoryMenu(int containerId, Inventory inventory) {
        super(ModMenus.BODY_INVENTORY.get(), containerId);
        this.owner = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inventory, 9 + row * COLUMNS + col, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inventory, col, GRID_X + col * 18, GRID_Y + 3 * 18 + HOTBAR_GAP));
        }
    }

    /** Opens the body inventory for a player; creative and spectator players keep the vanilla screens. Returns whether it opened. */
    public static boolean open(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BodyInventoryMenu(id, inventory),
                Component.translatable("menu.void_reclaimer.inventory")));
        return player.containerMenu instanceof BodyInventoryMenu;
    }

    /** Whether the main-inventory slot behind a menu slot is locked by the body. */
    public boolean isLocked(Slot slot) {
        return InventoryLock.isLocked(owner, slot.getContainerSlot());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < MAIN_SLOTS
                ? moveItemStackTo(stack, MAIN_SLOTS, MAIN_SLOTS + HOTBAR_SLOTS, false)
                : moveItemStackTo(stack, 0, MAIN_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
