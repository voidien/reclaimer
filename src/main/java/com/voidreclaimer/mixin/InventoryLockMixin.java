package com.voidreclaimer.mixin;

import com.voidreclaimer.body.InventoryLock;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Picking up, giving and adding items never fills a slot the body has locked ({@link InventoryLock}). There is no
 * NeoForge event for where {@code Inventory#add} puts items, so the two slot lookups it uses are replaced here with
 * versions that skip locked slots.
 */
@Mixin(Inventory.class)
public abstract class InventoryLockMixin {
    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Shadow
    public int selected;

    @Shadow
    protected abstract boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin);

    @Shadow
    public abstract ItemStack getItem(int index);

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$freeUnlockedSlot(CallbackInfoReturnable<Integer> cir) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty() && !InventoryLock.isLocked(player, i)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$unlockedSlotWithSpace(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (hasRemainingSpaceForItem(getItem(selected), stack)) {
            cir.setReturnValue(selected);
            return;
        }
        if (hasRemainingSpaceForItem(getItem(Inventory.SLOT_OFFHAND), stack)) {
            cir.setReturnValue(Inventory.SLOT_OFFHAND);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!InventoryLock.isLocked(player, i) && hasRemainingSpaceForItem(items.get(i), stack)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }
}
