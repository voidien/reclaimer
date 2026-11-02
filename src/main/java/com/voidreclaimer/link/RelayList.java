package com.voidreclaimer.link;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

/**
 * The relays of a level, as the server last saw them. It is saved with the level, so a relay in an unloaded chunk still counts: nothing
 * ticks it, so nothing about it can have changed. {@link Relays} keeps it current. Only the server has it.
 */
public record RelayList(List<Entry> relays) {
    public static final RelayList EMPTY = new RelayList(List.of());

    /**
     * One relay.
     *
     * @param id       the id it keeps for good, wherever a ship takes it
     * @param block    where its block is, on a ship in the ship's own coordinates
     * @param position where it is in the world, which moves with a ship
     * @param lit      whether it had power when last seen
     * @param ship     the ship it is aboard, if any
     */
    public record Entry(UUID id, BlockPos block, Vec3 position, boolean lit, Optional<UUID> ship) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                BlockPos.CODEC.fieldOf("block").forGetter(Entry::block),
                Vec3.CODEC.fieldOf("position").forGetter(Entry::position),
                Codec.BOOL.optionalFieldOf("lit", false).forGetter(Entry::lit),
                UUIDUtil.CODEC.lenientOptionalFieldOf("ship").forGetter(Entry::ship)
        ).apply(instance, Entry::new));
    }

    /** A row that cannot be read costs that row alone, and the relay writes it again when it next ticks. */
    public static final Codec<RelayList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModCodecs.eachOnItsOwn(Entry.CODEC, "a relay").lenientOptionalFieldOf("relays", List.of()).forGetter(RelayList::relays)
    ).apply(instance, RelayList::new));

    public RelayList {
        relays = List.copyOf(relays);
    }

    public Optional<Entry> find(UUID id) {
        return relays.stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    /** This list with {@code entry} in place of any older row for that relay, or this same list if that changes nothing. */
    public RelayList with(Entry entry) {
        List<Entry> updated = new ArrayList<>(relays.size() + 1);
        boolean found = false;
        for (Entry old : relays) {
            if (old.id().equals(entry.id())) {
                if (old.equals(entry)) {
                    return this;
                }
                updated.add(entry);
                found = true;
            } else {
                updated.add(old);
            }
        }
        if (!found) {
            updated.add(entry);
        }
        return new RelayList(updated);
    }

    /** This list without the relays aboard {@code ship}, or this same list if it had none. */
    public RelayList withoutShip(UUID ship) {
        List<Entry> kept = relays.stream().filter(entry -> entry.ship().filter(ship::equals).isEmpty()).toList();
        return kept.size() == relays.size() ? this : new RelayList(kept);
    }

    /** This list without the relay {@code id}, or this same list if it was not there. */
    public RelayList without(UUID id) {
        if (find(id).isEmpty()) {
            return this;
        }
        return new RelayList(relays.stream().filter(entry -> !entry.id().equals(id)).toList());
    }
}
