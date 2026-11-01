package com.voidreclaimer.link;

import com.mojang.serialization.Codec;
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

/**
 * Where a player's mind is, saved in their player file. The player file is the one store of record while linked.
 *
 * <p>Bodies are held as the raw tag {@link BodySnapshot#save} wrote, not as a parsed snapshot. An attachment that fails to read
 * is dropped by NeoForge with a log line, and with it a main body; a tag always reads, and {@link BodySnapshot#load} then says
 * whether it is whole.
 *
 * @param link     the shell the player is in, if any
 * @param outboxes shells put back in the world that may not have reached the disk yet, at most one each, see {@link Outbox}
 */
public record LinkState(Optional<Link> link, List<Outbox> outboxes) {
    public static final LinkState NONE = new LinkState(Optional.empty(), List.of());

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
     */
    public record Link(UUID shell, long epoch, CompoundTag home, Place from) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("shell").forGetter(Link::shell),
                Codec.LONG.fieldOf("epoch").forGetter(Link::epoch),
                CompoundTag.CODEC.fieldOf("home").forGetter(Link::home),
                Place.CODEC.fieldOf("from").forGetter(Link::from)
        ).apply(instance, Link::new));
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

        Outbox seenInASave() {
            return new Outbox(shell, epoch, body, at, true);
        }
    }

    public static final Codec<LinkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Link.CODEC.optionalFieldOf("link").forGetter(LinkState::link),
            Outbox.CODEC.listOf().lenientOptionalFieldOf("outboxes", List.of()).forGetter(LinkState::outboxes)
    ).apply(instance, LinkState::new));

    public boolean linked() {
        return link.isPresent();
    }

    public LinkState withLink(Link value) {
        return new LinkState(Optional.of(value), outboxes);
    }

    public LinkState withoutLink() {
        return new LinkState(Optional.empty(), outboxes);
    }

    /** The copy of {@code shell} waiting for the disk, if any. */
    public Optional<Outbox> outbox(UUID shell) {
        return outboxes.stream().filter(outbox -> outbox.shell().equals(shell)).findFirst();
    }

    /** Replaces any older copy of the same shell. */
    public LinkState withOutbox(Outbox value) {
        List<Outbox> all = new ArrayList<>(withoutOutbox(value.shell()).outboxes);
        all.add(value);
        return new LinkState(link, List.copyOf(all));
    }

    public LinkState withoutOutbox(UUID shell) {
        return new LinkState(link, outboxes.stream().filter(outbox -> !outbox.shell().equals(shell)).toList());
    }
}
