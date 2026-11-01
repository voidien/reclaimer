package com.voidreclaimer.link;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What is left of a destroyed shell. It keeps the shell's look, but everything it carried or was built from, sections and modules
 * too, is one list of stacks that a player takes out by hand. Nothing goes on the floor, because a stack on the floor despawns.
 */
public final class Wrecks {
    private Wrecks() {}

    /** Turns {@code shell} into a wreck in place: the same entity, so the same id and the same chunk. */
    static void make(BodyHost shell) {
        HolderLookup.Provider registries = shell.registryAccess();
        ListTag contents = shell.items();
        BodyData body = shell.getData(ModAttachments.BODY);
        body.sections().values().forEach(stack -> add(contents, stack, registries));
        body.modules().values().forEach(stack -> add(contents, stack, registries));
        body.pending().forEach(stack -> add(contents, stack, registries));
        shell.setItems(contents);
        shell.setEffects(new ListTag());
        shell.removeAllEffects();
        shell.setRole(HostRole.WRECK);
        shell.setHealth(shell.getMaxHealth());
        shell.setDeltaMovement(Vec3.ZERO);
    }

    private static void add(ListTag contents, ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            contents.add(stack.save(registries));
        }
    }

    /** Moves what fits into {@code player}'s inventory. An emptied wreck goes, and takes its outbox with it, or a crash would bring it back full. */
    static void salvage(ServerPlayer player, BodyHost wreck) {
        HolderLookup.Provider registries = player.registryAccess();
        Inventory inventory = player.getInventory();
        ListTag contents = wreck.items();
        ListTag left = new ListTag();
        int taken = 0;
        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, contents.getCompound(i));
            if (stack.isEmpty()) {
                continue;
            }
            int before = stack.getCount();
            inventory.add(stack);
            if (stack.getCount() < before) {
                taken++;
            }
            if (!stack.isEmpty()) {
                left.add(stack.save(registries));
            }
        }
        wreck.setItems(left);
        if (left.isEmpty()) {
            Links.destroyed(wreck);
            wreck.discard();
            player.displayClientMessage(Component.translatable("message.void_reclaimer.wreck.empty", taken), true);
        } else {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.wreck.left", taken, left.size()), true);
        }
        player.getServer().getPlayerList().saveAll();
    }
}
