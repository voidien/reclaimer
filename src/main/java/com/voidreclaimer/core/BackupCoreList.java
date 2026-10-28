package com.voidreclaimer.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's Backup Cores, as last seen (the death screen lists these), and the core picked for the next respawn.
 * The cores themselves (block entities) are the source of truth; this is refreshed from them on login, on death and
 * whenever a core changes while its owner is online.
 *
 * @param cores    owned cores, at most {@link #MAX_CORES}
 * @param selected core chosen on the death screen for the next respawn; empty for the free deep space spawn
 */
public record BackupCoreList(List<Entry> cores, Optional<GlobalPos> selected) {
    public static final int MAX_CORES = 3;
    public static final BackupCoreList EMPTY = new BackupCoreList(List.of(), Optional.empty());

    public static final Codec<BackupCoreList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().lenientOptionalFieldOf("cores", List.of()).forGetter(BackupCoreList::cores),
            GlobalPos.CODEC.lenientOptionalFieldOf("selected").forGetter(BackupCoreList::selected)
    ).apply(instance, BackupCoreList::new));

    public static final StreamCodec<ByteBuf, BackupCoreList> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), BackupCoreList::cores,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), BackupCoreList::selected,
            BackupCoreList::new);

    public BackupCoreList {
        cores = List.copyOf(cores);
    }

    public boolean isFull() {
        return cores.size() >= MAX_CORES;
    }

    public Optional<Entry> get(GlobalPos pos) {
        return cores.stream().filter(entry -> entry.pos().equals(pos)).findFirst();
    }

    /** Adds or replaces the entry for {@code entry.pos()}. */
    public BackupCoreList with(Entry entry) {
        List<Entry> list = new ArrayList<>(cores);
        list.removeIf(e -> e.pos().equals(entry.pos()));
        list.add(entry);
        return new BackupCoreList(list, selected);
    }

    public BackupCoreList without(GlobalPos pos) {
        List<Entry> list = new ArrayList<>(cores);
        list.removeIf(e -> e.pos().equals(pos));
        return new BackupCoreList(list, selected.filter(s -> !s.equals(pos)));
    }

    public BackupCoreList withSelected(Optional<GlobalPos> pos) {
        return new BackupCoreList(cores, pos.filter(p -> get(p).isPresent()));
    }

    /**
     * @param pos     where the core is
     * @param name    the name given in the core UI
     * @param charges respawns stored, 0..{@link CoreCharging#MAX_CHARGES}
     */
    public record Entry(GlobalPos pos, String name, int charges) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name),
                Codec.INT.optionalFieldOf("charges", 0).forGetter(Entry::charges)
        ).apply(instance, Entry::new));

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                ByteBufCodecs.VAR_INT, Entry::charges,
                Entry::new);
    }
}
