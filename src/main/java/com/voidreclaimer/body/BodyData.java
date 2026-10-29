package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The AI's assembled body: its frame and the parts in each slot, the active tool module, and its looks. Immutable;
 * every change makes a new instance. Slot lists are kept exactly as long as the frame allows (empty stacks for empty
 * slots). Stats are derived from the parts' {@link PartStats} components ({@link BodyStats}).
 */
public final class BodyData {
    public static final Codec<BodyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.lenientOptionalFieldOf("frame", ItemStack.EMPTY).forGetter(BodyData::frame),
            Codec.unboundedMap(PartKind.CODEC, ItemStack.OPTIONAL_CODEC.listOf()).lenientOptionalFieldOf("parts", Map.of())
                    .forGetter(data -> data.parts),
            Codec.INT.optionalFieldOf("active_tool", 0).forGetter(BodyData::activeTool),
            Cosmetics.CODEC.lenientOptionalFieldOf("cosmetics", Cosmetics.DEFAULT).forGetter(BodyData::cosmetics)
    ).apply(instance, BodyData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, BodyData::frame,
            ByteBufCodecs.map(HashMap::new, PartKind.STREAM_CODEC, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list())),
            data -> new HashMap<>(data.parts),
            ByteBufCodecs.VAR_INT, BodyData::activeTool,
            Cosmetics.STREAM_CODEC, BodyData::cosmetics,
            BodyData::new);

    private final ItemStack frame;
    private final Map<PartKind, List<ItemStack>> parts;
    private final int activeTool;
    private final Cosmetics cosmetics;
    @Nullable
    private BodyStats stats;

    public BodyData(ItemStack frame, Map<PartKind, List<ItemStack>> parts, int activeTool, Cosmetics cosmetics) {
        this.frame = frame.copy();
        PartStats.Frame spec = frameSpec(this.frame);
        Map<PartKind, List<ItemStack>> sized = new EnumMap<>(PartKind.class);
        for (PartKind kind : PartKind.values()) {
            if (kind == PartKind.FRAME) {
                continue;
            }
            int count = spec == null ? (kind == PartKind.CORE ? 1 : 0) : spec.slots(kind);
            List<ItemStack> given = parts.getOrDefault(kind, List.of());
            List<ItemStack> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(i < given.size() ? given.get(i).copy() : ItemStack.EMPTY);
            }
            sized.put(kind, List.copyOf(list));
        }
        this.parts = sized;
        this.activeTool = activeTool;
        this.cosmetics = cosmetics;
    }

    @Nullable
    private static PartStats.Frame frameSpec(ItemStack frame) {
        PartStats stats = Parts.stats(frame);
        return stats == null ? null : stats.frame().orElse(null);
    }

    public ItemStack frame() {
        return frame.copy();
    }

    /** Parts installed in slots of {@code kind} (copies; empty stacks for empty slots). For FRAME, the frame. */
    public List<ItemStack> parts(PartKind kind) {
        if (kind == PartKind.FRAME) {
            return List.of(frame());
        }
        return parts.getOrDefault(kind, List.of()).stream().map(ItemStack::copy).toList();
    }

    public int slots(PartKind kind) {
        return kind == PartKind.FRAME ? 1 : parts.getOrDefault(kind, List.of()).size();
    }

    public ItemStack get(PartKind kind, int index) {
        if (kind == PartKind.FRAME) {
            return frame();
        }
        List<ItemStack> list = parts.getOrDefault(kind, List.of());
        return index >= 0 && index < list.size() ? list.get(index).copy() : ItemStack.EMPTY;
    }

    /** Every installed part except the frame, without copying (read-only use). */
    List<ItemStack> installedParts() {
        List<ItemStack> all = new ArrayList<>();
        for (List<ItemStack> list : parts.values()) {
            for (ItemStack stack : list) {
                if (!stack.isEmpty()) {
                    all.add(stack);
                }
            }
        }
        return all;
    }

    public int activeTool() {
        return activeTool;
    }

    public Cosmetics cosmetics() {
        return cosmetics;
    }

    public BodyStats stats() {
        BodyStats cached = stats;
        if (cached == null) {
            List<PartStats> partStats = new ArrayList<>();
            for (ItemStack stack : installedParts()) {
                PartStats s = Parts.stats(stack);
                if (s != null) {
                    partStats.add(s);
                }
            }
            cached = BodyStats.compute(Parts.stats(frame), partStats);
            stats = cached;
        }
        return cached;
    }

    // ---- edits --------------------------------------------------------------------------------------------

    public BodyData with(PartKind kind, int index, ItemStack stack) {
        if (kind == PartKind.FRAME) {
            return withFrame(stack);
        }
        Map<PartKind, List<ItemStack>> copy = new EnumMap<>(PartKind.class);
        copy.putAll(parts);
        List<ItemStack> list = new ArrayList<>(copy.getOrDefault(kind, List.of()));
        if (index < 0 || index >= list.size()) {
            return this;
        }
        list.set(index, stack.copy());
        copy.put(kind, list);
        return new BodyData(frame, copy, activeTool, cosmetics);
    }

    /** A new frame; parts are carried over as far as its slots allow (see {@link BodyRules#swapFrame}). */
    public BodyData withFrame(ItemStack newFrame) {
        return new BodyData(newFrame, parts, activeTool, cosmetics);
    }

    public BodyData withActiveTool(int index) {
        return new BodyData(frame, parts, index, cosmetics);
    }

    public BodyData withCosmetics(Cosmetics value) {
        return new BodyData(frame, parts, activeTool, value);
    }

    /** The active tool module, or empty. */
    public ItemStack activeToolStack() {
        return get(PartKind.TOOL, activeTool);
    }

    public boolean hasInstalled(PartKind kind, java.util.function.Predicate<ItemStack> test) {
        for (ItemStack stack : parts.getOrDefault(kind, List.of())) {
            if (!stack.isEmpty() && test.test(stack)) {
                return true;
            }
        }
        return false;
    }
}
