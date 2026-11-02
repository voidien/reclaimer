package com.voidreclaimer.link;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.tether.TetherData;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * All that belongs to a body and not to the mind running it, so that a link can move a mind from one body to another. Dust,
 * the Replicator, milestones and the like stay with the mind and are not here.
 *
 * <p>Items and effects stay as the game saves them, and {@code Inventory.load} reads them one slot at a time, so one bad item
 * costs that item and not the rest.
 *
 * @param items    as {@code Inventory.save} writes them, armor and offhand included
 * @param selected the selected hotbar slot
 * @param effects  each as {@code MobEffectInstance.save} writes it
 */
public record BodySnapshot(BodyData body, EnergyData energy, TetherData tether, ListTag items, int selected, float health, ListTag effects) {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** With {@code more} waiting among the body's parts, so they stay with it wherever it goes, a wreck or a crash included. */
    public BodySnapshot withPending(List<net.minecraft.world.item.ItemStack> more) {
        List<net.minecraft.world.item.ItemStack> all = new java.util.ArrayList<>(body.pending());
        all.addAll(more);
        return new BodySnapshot(body.withPending(all), energy, tether, items, selected, health, effects);
    }

    public BodySnapshot withEnergy(EnergyData value) {
        return new BodySnapshot(body, value, tether, items, selected, health, effects);
    }

    /**
     * Takes what {@code player} is wearing, ready to move: it closes their screen, so this is not a way to look. Call it before
     * anything moves the player. Nothing is taken away: until a body is put on over it, the player still wears all of this.
     */
    public static BodySnapshot take(ServerPlayer player) {
        tidy(player);
        Inventory inventory = player.getInventory();
        ListTag items = new ListTag();
        inventory.save(items);
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.save());
        }
        return new BodySnapshot(Bodies.data(player), player.getData(ModAttachments.ENERGY), restingTether(player),
                items, inventory.selected, player.getHealth(), effects);
    }

    /**
     * Closes the player's screen with nothing left on the cursor. A stack on the cursor is in no inventory, and closing a screen
     * with no room for it would drop it where nobody may come back for it, so it goes into the inventory or the body's pending
     * items. Those are the player's own, so this costs nothing if the move then stops.
     */
    private static void tidy(ServerPlayer player) {
        ItemStack carried = player.containerMenu.getCarried();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.closeContainer();
        player.getInventory().add(carried);
        if (!carried.isEmpty()) {
            Bodies.store(player, Bodies.data(player).plusPending(List.of(carried)));
        }
    }

    /** What {@code host} is holding now. */
    public static BodySnapshot of(BodyHost host) {
        return new BodySnapshot(Bodies.data(host), host.getData(ModAttachments.ENERGY), restingTether(host),
                host.items(), host.selected(), host.getHealth(), host.effects());
    }

    /** A winch in mid-pull does not carry over: whoever comes back is not holding the key, and nothing would stop it. */
    private static TetherData restingTether(LivingEntity entity) {
        return entity.getData(ModAttachments.TETHER).withRecalling(false);
    }

    /** This body fitted to its layout, which a datapack may have changed while it lay in storage. What no longer fits comes back as pending items. */
    private BodyData fitted(RegistryAccess registries) {
        BodyRules.Change change = BodyRules.normalize(body, Bodies.chassis(registries, body));
        return change.returned().isEmpty() ? body : change.body().plusPending(change.returned());
    }

    /**
     * Puts this body on {@code player}, replacing what they wear: {@link #take} that first, or it is gone. The body goes on after
     * the things that belong to it and before health, because it sets the health limit.
     *
     * @throws IllegalStateException with a screen open, before anything changes: its cursor stack would ride into this body
     */
    public void applyTo(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            throw new IllegalStateException("a body cannot go on over an open screen: " + player.containerMenu);
        }
        Energy.update(player, energy, true);
        player.setData(ModAttachments.TETHER, tether);
        player.syncData(ModAttachments.TETHER);
        Inventory inventory = player.getInventory();
        inventory.load(items);
        inventory.selected = Inventory.isHotbarSlot(selected) ? selected : 0;
        inventory.setChanged();
        player.removeAllEffects();
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = MobEffectInstance.load(effects.getCompound(i));
            if (effect != null) {
                player.addEffect(effect);
            }
        }
        Bodies.store(player, fitted(player.registryAccess()));
        player.setHealth(Mth.clamp(health, 1f, player.getMaxHealth()));
        player.inventoryMenu.sendAllDataToRemote();
        player.connection.send(new ClientboundSetCarriedItemPacket(inventory.selected));
    }

    /** Leaves this body in {@code host}. */
    public void applyTo(BodyHost host) {
        host.setData(ModAttachments.ENERGY, energy);
        host.setData(ModAttachments.TETHER, tether);
        host.wear(fitted(host.registryAccess()));
        host.setItems(items);
        host.setSelected(selected);
        host.setEffects(effects);
        host.setHealth(Mth.clamp(health, 1f, host.getMaxHealth()));
    }

    /**
     * @throws IllegalStateException if a part cannot be written. Nothing has moved yet at that point, so a link can stop; a save
     *                               with a part missing would quietly lose the body later.
     */
    public CompoundTag save(HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        tag.put("body", BodyData.CODEC.encodeStart(ops, body).getOrThrow());
        tag.put("energy", EnergyData.CODEC.encodeStart(ops, energy).getOrThrow());
        tag.put("tether", TetherData.CODEC.encodeStart(ops, tether).getOrThrow());
        tag.put("items", items.copy());
        tag.putInt("selected", selected);
        tag.putFloat("health", health);
        tag.put("effects", effects.copy());
        return tag;
    }

    /**
     * The snapshot in {@code tag}, or empty when its body or items cannot be read. A body may be many hours of work, so it is
     * never replaced by a starter one: the caller keeps the tag, and the player can be told. Energy and tether have safe defaults.
     */
    public static Optional<BodySnapshot> load(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("body") || !tag.contains("items", Tag.TAG_LIST)) {
            return Optional.empty();
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        DataResult<BodyData> body = BodyData.CODEC.parse(ops, tag.get("body"));
        if (body.isError()) {
            LOGGER.error("A saved body could not be read: {}", body);
            return Optional.empty();
        }
        return Optional.of(new BodySnapshot(body.getOrThrow(), read(EnergyData.CODEC, tag, "energy", ops, EnergyData::createDefault),
                read(TetherData.CODEC, tag, "tether", ops, () -> TetherData.NONE), tag.getList("items", Tag.TAG_COMPOUND).copy(),
                tag.getInt("selected"), tag.getFloat("health"), tag.getList("effects", Tag.TAG_COMPOUND).copy()));
    }

    private static <T> T read(Codec<T> codec, CompoundTag tag, String key, RegistryOps<Tag> ops, Supplier<T> fallback) {
        Tag part = tag.get(key);
        return part == null ? fallback.get() : codec.parse(ops, part).resultOrPartial(LOGGER::error).orElseGet(fallback);
    }
}
