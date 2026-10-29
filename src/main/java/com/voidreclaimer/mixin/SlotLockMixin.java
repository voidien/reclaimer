package com.voidreclaimer.mixin;

import com.voidreclaimer.body.InventoryLock;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Menus (the inventory screen, chests, shift-clicking) can't put items into a slot the body has locked. */
@Mixin(Slot.class)
public abstract class SlotLockMixin {
    @Shadow
    @Final
    public Container container;

    @Shadow
    public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$lockedSlot(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof Inventory inventory && InventoryLock.isLocked(inventory.player, getContainerSlot())) {
            cir.setReturnValue(false);
        }
    }
}
