package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server side of the modular body: the starter body, installing and removing parts (items move between the body and
 * the inventory), and applying the body's stats (scale, max health, reach, energy capacity). Flight, dust and energy
 * read {@link #stats(Player)} directly.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Bodies {
    private static final ResourceLocation SCALE = VoidReclaimer.id("body_scale");
    private static final ResourceLocation HEALTH = VoidReclaimer.id("body_health");
    private static final ResourceLocation BLOCK_REACH = VoidReclaimer.id("body_block_reach");
    private static final ResourceLocation ENTITY_REACH = VoidReclaimer.id("body_entity_reach");
    private static final float VANILLA_MAX_HEALTH = 20f;

    private Bodies() {}

    /** What every AI starts as (and what existing players get when this phase first loads). */
    public static BodyData starter() {
        return new BodyData(new ItemStack(ModItems.DRONE_FRAME.get()),
                Map.of(PartKind.CORE, List.of(new ItemStack(ModItems.CORE_MK1.get())),
                        PartKind.THRUSTER, List.of(new ItemStack(ModItems.THRUSTER_MK1.get()))),
                0, Cosmetics.DEFAULT);
    }

    public static BodyData data(Player player) {
        return player.getData(ModAttachments.BODY);
    }

    /** Valid on both sides (the owner's client has the synced body). */
    public static BodyStats stats(Player player) {
        return data(player).stats();
    }

    public static void store(ServerPlayer player, BodyData body) {
        player.setData(ModAttachments.BODY, body);
        player.syncData(ModAttachments.BODY);
        apply(player);
    }

    /** Pushes the body's stats into attributes and energy capacity. */
    public static void apply(ServerPlayer player) {
        BodyStats stats = stats(player);
        modifier(player, Attributes.SCALE, SCALE, stats.size().scale() - 1.0);
        modifier(player, Attributes.MAX_HEALTH, HEALTH, stats.maxHealth() - VANILLA_MAX_HEALTH);
        modifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH, stats.reach());
        modifier(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH, stats.reach());
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        EnergyData energy = player.getData(ModAttachments.ENERGY);
        if (energy.capacity() != stats.energyCapacity()) {
            Energy.update(player, new EnergyData(Math.min(energy.energy(), stats.energyCapacity()), stats.energyCapacity(),
                    energy.lowPower()), true);
        }
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ---- install / remove ---------------------------------------------------------------------------------

    /** Installs the part from inventory slot {@code inventorySlot} into body slot {@code index} of {@code kind}. */
    public static BodyRules.Check install(ServerPlayer player, PartKind kind, int index, int inventorySlot) {
        Inventory inventory = player.getInventory();
        if (inventorySlot < 0 || inventorySlot >= inventory.items.size()) {
            return reply(player, BodyRules.Check.BAD_SLOT);
        }
        ItemStack source = inventory.items.get(inventorySlot);
        BodyData body = data(player);
        BodyRules.Check check = BodyRules.canInstall(body, kind, index, source);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        ItemStack part = source.split(1);
        if (kind == PartKind.FRAME) {
            BodyRules.Swap swap = BodyRules.swapFrame(body, part);
            if (freeSlots(player) + (source.isEmpty() ? 1 : 0) < swap.returned().size()) {
                source.grow(1);
                return reply(player, "message.void_reclaimer.body.inventory_full");
            }
            store(player, swap.body());
            for (ItemStack returned : swap.returned()) {
                giveBack(player, inventorySlot, returned);
            }
        } else {
            ItemStack old = body.get(kind, index);
            store(player, body.with(kind, index, part));
            if (!old.isEmpty()) {
                giveBack(player, inventorySlot, old);
            }
        }
        return BodyRules.Check.OK;
    }

    /** Moves a part from the body back to the inventory. */
    public static BodyRules.Check remove(ServerPlayer player, PartKind kind, int index) {
        BodyData body = data(player);
        BodyRules.Check check = BodyRules.canRemove(body, kind, index);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        if (freeSlots(player) < 1) {
            reply(player, "message.void_reclaimer.body.inventory_full");
            return BodyRules.Check.BAD_SLOT;
        }
        ItemStack part = body.get(kind, index);
        store(player, body.with(kind, index, ItemStack.EMPTY));
        player.getInventory().add(part);
        return BodyRules.Check.OK;
    }

    public static void setCosmetics(ServerPlayer player, Cosmetics cosmetics) {
        store(player, data(player).withCosmetics(cosmetics));
    }

    public static void selectTool(ServerPlayer player, int index) {
        BodyData body = data(player);
        if (index >= 0 && index < body.slots(PartKind.TOOL)) {
            store(player, body.withActiveTool(index));
        }
    }

    /** Puts {@code stack} into {@code preferredSlot} if it is empty, otherwise anywhere; drops it if all else fails. */
    private static void giveBack(ServerPlayer player, int preferredSlot, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (inventory.items.get(preferredSlot).isEmpty()) {
            inventory.items.set(preferredSlot, stack);
        } else if (!inventory.add(stack)) {
            player.drop(stack, false);
        }
    }

    private static int freeSlots(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int free = 0;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty() && !InventoryLock.isLocked(player, i)) {
                free++;
            }
        }
        return free;
    }

    private static BodyRules.Check reply(ServerPlayer player, BodyRules.Check check) {
        String key = BodyRules.messageKey(check);
        if (key != null) {
            reply(player, key);
        }
        return check;
    }

    private static BodyRules.Check reply(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
        return BodyRules.Check.BAD_SLOT;
    }

    // ---- lifecycle ----------------------------------------------------------------------------------------

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
            player.setHealth(player.getMaxHealth());
        }
    }
}
