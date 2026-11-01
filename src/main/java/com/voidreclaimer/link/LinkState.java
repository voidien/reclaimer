package com.voidreclaimer.link;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Where a player's mind is, saved in their player file. The player file is the one store of record while linked.
 *
 * <p>Bodies are held as the raw tag {@link BodySnapshot#save} wrote, not as a parsed snapshot. An attachment that fails to read
 * is dropped by NeoForge with a log line, and with it a main body; a tag always reads, and {@link BodySnapshot#load} then says
 * whether it is whole.
 *
 * @param link     the shell the player is in, if any
 * @param outboxes shells put back in the world that may not have reached the disk yet, at most one each, see {@link Outbox}
 * @param shells   every shell the player has owned, at most one entry each, see {@link KnownShell}
 */
public record LinkState(Optional<Link> link, List<Outbox> outboxes, List<KnownShell> shells) {
    public static final LinkState NONE = new LinkState(Optional.empty(), List.of(), List.of());

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<ResourceKey<Level>> DIMENSION = Level.RESOURCE_KEY_CODEC;

    /** Where a body was left. */
    public record Place(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
        static final Codec<Place> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DIMENSION.fieldOf("dimension").forGetter(Place::dimension),
                Vec3.CODEC.fieldOf("position").forGetter(Place::position),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Place::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Place::pitch)
        ).apply(instance, Place::new));
    }

    /**
     * A player in a shell.
     *
     * @param shell the shell's entity id, which it keeps when it is put back
     * @param epoch this link's number on the shell: a copy of the shell with an older one is stale
     * @param home  the main body, as a tag
     * @param from  where the main body was left
     * @param seat  the id of the Uplink Seat this link started from, if it started from one
     * @param since the game time the link began, which a seat's charge is worked out from
     */
    public record Link(UUID shell, long epoch, CompoundTag home, Place from, Optional<UUID> seat, long since) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("shell").forGetter(Link::shell),
                Codec.LONG.fieldOf("epoch").forGetter(Link::epoch),
                CompoundTag.CODEC.fieldOf("home").forGetter(Link::home),
                Place.CODEC.fieldOf("from").forGetter(Link::from),
                UUIDUtil.CODEC.optionalFieldOf("seat").forGetter(Link::seat),
                Codec.LONG.optionalFieldOf("since", 0L).forGetter(Link::since)
        ).apply(instance, Link::new));

        public Link(UUID shell, long epoch, CompoundTag home, Place from) {
            this(shell, epoch, home, from, Optional.empty(), 0L);
        }

        public Link(UUID shell, long epoch, CompoundTag home, Place from, Optional<UUID> seat) {
            this(shell, epoch, home, from, seat, 0L);
        }

        /** The same link, with the main body carried to {@code position}. */
        Link carriedTo(Vec3 position) {
            return new Link(shell, epoch, home, new Place(from.dimension(), position, from.yaw(), from.pitch()), seat, since);
        }
    }

    /**
     * A shell put back in the world. Saving players does not save entities: a shell reaches the disk with its chunk, so a crash
     * right after waking would lose it, and the player file no longer holds it. This copy stays until the shell is known to be
     * on disk, and rebuilds it if it is not.
     *
     * @param shell the shell's entity id
     * @param epoch the shell's epoch
     * @param body  the shell's body, as a tag
     * @param at    where the shell was put
     * @param saved whether the shell has been through a save of its level already, see {@link Outboxes}
     */
    public record Outbox(UUID shell, long epoch, CompoundTag body, Place at, boolean saved) {
        static final Codec<Outbox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("shell").forGetter(Outbox::shell),
                Codec.LONG.fieldOf("epoch").forGetter(Outbox::epoch),
                CompoundTag.CODEC.fieldOf("body").forGetter(Outbox::body),
                Place.CODEC.fieldOf("at").forGetter(Outbox::at),
                Codec.BOOL.optionalFieldOf("saved", false).forGetter(Outbox::saved)
        ).apply(instance, Outbox::new));

        /** Through one save, and {@code position} is where the shell was then. */
        Outbox seenInASave(Vec3 position) {
            return new Outbox(shell, epoch, body, new Place(at.dimension(), position, at.yaw(), at.pitch()), true);
        }
    }

    /**
     * A shell as its owner last knew it. An entry outlives the shell's outbox, and the shell: a crash can leave a copy of a shell on
     * disk in a chunk nobody visits for months, and this is what tells that copy from the shell. An outbox only ends once the newest
     * copy is on disk or the shell is destroyed, so with no outbox left, anything older than this entry, or any copy of a shell that
     * is gone, is left over.
     *
     * @param shell   the shell's entity id
     * @param epoch   the newest link number it has had
     * @param at      where it was last seen, updated when its level saves
     * @param wrecked whether it is a wreck
     * @param gone    whether it has been destroyed for good
     */
    public record KnownShell(UUID shell, long epoch, Place at, boolean wrecked, boolean gone, Optional<Place> oldCopy) {
        public KnownShell(UUID shell, long epoch, Place at, boolean wrecked, boolean gone) {
            this(shell, epoch, at, wrecked, gone, Optional.empty());
        }

        static final Codec<KnownShell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("shell").forGetter(KnownShell::shell),
                Codec.LONG.fieldOf("epoch").forGetter(KnownShell::epoch),
                Place.CODEC.fieldOf("at").forGetter(KnownShell::at),
                Codec.BOOL.optionalFieldOf("wrecked", false).forGetter(KnownShell::wrecked),
                Codec.BOOL.optionalFieldOf("gone", false).forGetter(KnownShell::gone),
                Place.CODEC.optionalFieldOf("old_copy").forGetter(KnownShell::oldCopy)
        ).apply(instance, KnownShell::new));

        /** With an older copy seen standing at {@code place}, or none. */
        KnownShell withOldCopy(Optional<Place> place) {
            return new KnownShell(shell, epoch, at, wrecked, gone, place);
        }

        KnownShell destroyed() {
            return new KnownShell(shell, epoch, at, wrecked, true);
        }
    }

    public static final Codec<LinkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Link.CODEC.optionalFieldOf("link").forGetter(LinkState::link),
            eachOnItsOwn(Outbox.CODEC).lenientOptionalFieldOf("outboxes", List.of()).forGetter(LinkState::outboxes),
            eachOnItsOwn(KnownShell.CODEC).lenientOptionalFieldOf("shells", List.of()).forGetter(LinkState::shells)
    ).apply(instance, LinkState::new));

    /** A list where an entry that cannot be read costs that entry alone, and says so: the rest of it may be someone's only copy of a body. */
    private static <T> Codec<List<T>> eachOnItsOwn(Codec<T> entry) {
        Codec<List<T>> whole = entry.listOf();
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<List<T>, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getStream(input).map(items -> {
                    List<T> kept = new ArrayList<>();
                    items.forEach(item -> entry.parse(ops, item)
                            .resultOrPartial(error -> LOGGER.error("An entry of a link state cannot be read and is dropped: {}", error))
                            .ifPresent(kept::add));
                    return Pair.of(List.copyOf(kept), ops.empty());
                });
            }

            @Override
            public <O> DataResult<O> encode(List<T> input, DynamicOps<O> ops, O prefix) {
                return whole.encode(input, ops, prefix);
            }
        };
    }

    public boolean linked() {
        return link.isPresent();
    }

    public LinkState withLink(Link value) {
        return new LinkState(Optional.of(value), outboxes, shells);
    }

    public LinkState withoutLink() {
        return new LinkState(Optional.empty(), outboxes, shells);
    }

    /** The copy of {@code shell} waiting for the disk, if any. */
    public Optional<Outbox> outbox(UUID shell) {
        return outboxes.stream().filter(outbox -> outbox.shell().equals(shell)).findFirst();
    }

    /** Replaces any older copy of the same shell. */
    public LinkState withOutbox(Outbox value) {
        List<Outbox> all = new ArrayList<>(withoutOutbox(value.shell()).outboxes);
        all.add(value);
        return new LinkState(link, List.copyOf(all), shells);
    }

    public LinkState withoutOutbox(UUID shell) {
        return new LinkState(link, outboxes.stream().filter(outbox -> !outbox.shell().equals(shell)).toList(), shells);
    }

    /** What is known of {@code shell}, if anything. */
    public Optional<KnownShell> known(UUID shell) {
        return shells.stream().filter(known -> known.shell().equals(shell)).findFirst();
    }

    /** Replaces what was known of the same shell. Keeps its place in the list, so the list reads in the order the shells were first met. */
    public LinkState withKnown(KnownShell value) {
        List<KnownShell> all = new ArrayList<>(shells);
        int found = all.stream().map(KnownShell::shell).toList().indexOf(value.shell());
        if (found < 0) {
            all.add(value);
        } else {
            all.set(found, value);
        }
        return new LinkState(link, outboxes, List.copyOf(all));
    }
}
