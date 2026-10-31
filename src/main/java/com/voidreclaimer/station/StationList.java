package com.voidreclaimer.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * The stations of a level, as the server last saw them: a Station Core's name, id and where it is in the world right
 * now (a core built into a moving ship moves with it). A level attachment synced to every player in the level, refreshed
 * once a second by {@link Stations}; the HUD markers and the Replicator's station bonus read it.
 */
public record StationList(List<Entry> stations) {
    public static final StationList EMPTY = new StationList(List.of());

    /** One station: {@code assembled} is false for a core still sitting in the world as plain blocks. */
    public record Entry(String name, UUID id, Vec3 position, boolean assembled) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                Vec3.CODEC.fieldOf("position").forGetter(Entry::position),
                Codec.BOOL.optionalFieldOf("assembled", false).forGetter(Entry::assembled)
        ).apply(instance, Entry::new));
        private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::name,
                UUIDUtil.STREAM_CODEC, Entry::id,
                VEC3, Entry::position,
                ByteBufCodecs.BOOL, Entry::assembled,
                Entry::new);
    }

    public static final Codec<StationList> CODEC = Entry.CODEC.listOf().xmap(StationList::new, StationList::stations);
    public static final StreamCodec<ByteBuf, StationList> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StationList::new, StationList::stations);

    public StationList {
        stations = List.copyOf(stations);
    }

    /** The nearest station to {@code from} within {@code range} blocks, if any. */
    public Optional<Entry> nearest(Vec3 from, double range) {
        return stations.stream()
                .filter(entry -> entry.position().distanceToSqr(from) <= range * range)
                .min(Comparator.comparingDouble(entry -> entry.position().distanceToSqr(from)));
    }

    /** The stations farther than {@code atLeast} blocks from {@code from}, nearest first, at most {@code count}. */
    public List<Entry> farthestFirstNearest(Vec3 from, double atLeast, int count) {
        return stations.stream()
                .filter(entry -> entry.position().distanceToSqr(from) > atLeast * atLeast)
                .sorted(Comparator.comparingDouble(entry -> entry.position().distanceToSqr(from)))
                .limit(count)
                .toList();
    }
}
