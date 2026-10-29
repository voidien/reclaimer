package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.body.ModuleKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The AI's assembled body: section items per socket (the core section in {@link #coreSocket()}), module items per
 * mount, the active tool module, its looks, and items waiting to be handed back. Immutable; every change makes a new
 * instance.
 *
 * <p>The data doesn't depend on the chassis registry: which sockets and mounts are valid is checked against the
 * layout by {@link BodyRules} ({@link BodyRules#normalize} moves misfits to {@link #pending()}).
 *
 * <p>Saves from before body sections (a {@code frame} and {@code parts} per kind) migrate on load
 * ({@link BodyMigration}).
 */
public final class BodyData {
    /** A module position: mount {@code index} of {@code kind} on the section in {@code socket}. */
    public record Mount(String socket, ModuleKind kind, int index) {
        public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("socket").forGetter(Mount::socket),
                ModuleKind.CODEC.fieldOf("kind").forGetter(Mount::kind),
                Codec.INT.fieldOf("index").forGetter(Mount::index)
        ).apply(instance, Mount::new));

        public static final StreamCodec<io.netty.buffer.ByteBuf, Mount> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Mount::socket,
                ModuleKind.STREAM_CODEC, Mount::kind,
                ByteBufCodecs.VAR_INT, Mount::index,
                Mount::new);
    }

    private record SectionEntry(String socket, ItemStack item) {
        static final Codec<SectionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("socket").forGetter(SectionEntry::socket),
                // Lenient per entry: an item from a removed mod empties its socket instead of failing the body.
                ItemStack.OPTIONAL_CODEC.lenientOptionalFieldOf("item", ItemStack.EMPTY).forGetter(SectionEntry::item)
        ).apply(instance, SectionEntry::new));
        static final StreamCodec<RegistryFriendlyByteBuf, SectionEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SectionEntry::socket,
                ItemStack.OPTIONAL_STREAM_CODEC, SectionEntry::item,
                SectionEntry::new);
    }

    private record ModuleEntry(Mount mount, ItemStack item) {
        static final Codec<ModuleEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Mount.CODEC.fieldOf("mount").forGetter(ModuleEntry::mount),
                ItemStack.OPTIONAL_CODEC.lenientOptionalFieldOf("item", ItemStack.EMPTY).forGetter(ModuleEntry::item)
        ).apply(instance, ModuleEntry::new));
        static final StreamCodec<RegistryFriendlyByteBuf, ModuleEntry> STREAM_CODEC = StreamCodec.composite(
                Mount.STREAM_CODEC, ModuleEntry::mount,
                ItemStack.OPTIONAL_STREAM_CODEC, ModuleEntry::item,
                ModuleEntry::new);
    }

    public static final Codec<BodyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("core_socket", "").forGetter(BodyData::coreSocket),
            SectionEntry.CODEC.listOf().lenientOptionalFieldOf("sections", List.of()).forGetter(BodyData::sectionEntries),
            ModuleEntry.CODEC.listOf().lenientOptionalFieldOf("modules", List.of()).forGetter(BodyData::moduleEntries),
            Codec.INT.optionalFieldOf("active_tool", 0).forGetter(BodyData::activeTool),
            Cosmetics.CODEC.lenientOptionalFieldOf("cosmetics", Cosmetics.DEFAULT).forGetter(BodyData::cosmetics),
            ItemStack.OPTIONAL_CODEC.listOf().lenientOptionalFieldOf("pending", List.of()).forGetter(BodyData::pending),
            // Pre-section saves. Never written (defaults are omitted).
            ItemStack.OPTIONAL_CODEC.lenientOptionalFieldOf("frame", ItemStack.EMPTY).forGetter(data -> ItemStack.EMPTY),
            Codec.unboundedMap(ModuleKind.CODEC, ItemStack.OPTIONAL_CODEC.listOf()).lenientOptionalFieldOf("parts", Map.of())
                    .forGetter(data -> Map.of())
    ).apply(instance, BodyData::decode));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BodyData::coreSocket,
            SectionEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), BodyData::sectionEntries,
            ModuleEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), BodyData::moduleEntries,
            ByteBufCodecs.VAR_INT, BodyData::activeTool,
            Cosmetics.STREAM_CODEC, BodyData::cosmetics,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), BodyData::pending,
            (core, sections, modules, tool, looks, pending) -> new BodyData(core, toSectionMap(sections), toModuleMap(modules), tool, looks, pending));

    public static final BodyData EMPTY = new BodyData("", Map.of(), Map.of(), 0, Cosmetics.DEFAULT, List.of());

    private final String coreSocket;
    private final Map<String, ItemStack> sections;
    private final Map<Mount, ItemStack> modules;
    private final int activeTool;
    private final Cosmetics cosmetics;
    private final List<ItemStack> pending;
    @Nullable
    private volatile BodyStats stats;

    public BodyData(String coreSocket, Map<String, ItemStack> sections, Map<Mount, ItemStack> modules, int activeTool,
                    Cosmetics cosmetics, List<ItemStack> pending) {
        this.coreSocket = coreSocket;
        Map<String, ItemStack> s = new LinkedHashMap<>();
        sections.forEach((socket, stack) -> {
            if (!stack.isEmpty()) {
                s.put(socket, stack.copy());
            }
        });
        Map<Mount, ItemStack> m = new LinkedHashMap<>();
        modules.forEach((mount, stack) -> {
            if (!stack.isEmpty()) {
                m.put(mount, stack.copy());
            }
        });
        this.sections = Collections.unmodifiableMap(s);
        this.modules = Collections.unmodifiableMap(m);
        this.activeTool = Math.max(0, activeTool);
        this.cosmetics = cosmetics;
        this.pending = pending.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }

    private static BodyData decode(String coreSocket, List<SectionEntry> sections, List<ModuleEntry> modules, int activeTool,
                                   Cosmetics cosmetics, List<ItemStack> pending, ItemStack legacyFrame,
                                   Map<ModuleKind, List<ItemStack>> legacyParts) {
        if (sections.isEmpty() && !legacyFrame.isEmpty()) {
            return BodyMigration.fromFrame(legacyFrame, legacyParts, activeTool, cosmetics);
        }
        return new BodyData(coreSocket, toSectionMap(sections), toModuleMap(modules), activeTool, cosmetics, pending);
    }

    private static Map<String, ItemStack> toSectionMap(List<SectionEntry> entries) {
        Map<String, ItemStack> map = new LinkedHashMap<>();
        entries.forEach(e -> map.put(e.socket(), e.item()));
        return map;
    }

    private static Map<Mount, ItemStack> toModuleMap(List<ModuleEntry> entries) {
        Map<Mount, ItemStack> map = new LinkedHashMap<>();
        entries.forEach(e -> map.put(e.mount(), e.item()));
        return map;
    }

    private List<SectionEntry> sectionEntries() {
        return sections.entrySet().stream().map(e -> new SectionEntry(e.getKey(), e.getValue())).toList();
    }

    private List<ModuleEntry> moduleEntries() {
        return modules.entrySet().stream().map(e -> new ModuleEntry(e.getKey(), e.getValue())).toList();
    }

    // ---- reading ------------------------------------------------------------------------------------------

    /** Id of the socket holding the core section ("" for an empty body). */
    public String coreSocket() {
        return coreSocket;
    }

    /** The core section (copy), or empty. */
    public ItemStack core() {
        return section(coreSocket);
    }

    /** The section in {@code socket} (copy), or empty. */
    public ItemStack section(String socket) {
        ItemStack stack = sections.get(socket);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    /** Installed sections by socket, without copying (read-only use). */
    public Map<String, ItemStack> sections() {
        return sections;
    }

    /** The module on {@code mount} (copy), or empty. */
    public ItemStack module(Mount mount) {
        ItemStack stack = modules.get(mount);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    /** Installed modules by mount, without copying (read-only use). */
    public Map<Mount, ItemStack> modules() {
        return modules;
    }

    public int activeTool() {
        return activeTool;
    }

    public Cosmetics cosmetics() {
        return cosmetics;
    }

    /** Items that left the body while no inventory was at hand (save migration); handed out on the next tick. */
    public List<ItemStack> pending() {
        return pending;
    }

    public boolean hasInstalled(ModuleKind kind, Predicate<ItemStack> test) {
        for (Map.Entry<Mount, ItemStack> entry : modules.entrySet()) {
            if (entry.getKey().kind() == kind && test.test(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    // ---- stats cache --------------------------------------------------------------------------------------

    @Nullable
    BodyStats cachedStats() {
        return stats;
    }

    void cacheStats(BodyStats value) {
        stats = value;
    }

    // ---- edits --------------------------------------------------------------------------------------------

    public BodyData withSection(String socket, ItemStack stack) {
        Map<String, ItemStack> copy = new LinkedHashMap<>(sections);
        copy.put(socket, stack);
        return new BodyData(coreSocket, copy, modules, activeTool, cosmetics, pending);
    }

    public BodyData withCoreSocket(String socket) {
        return new BodyData(socket, sections, modules, activeTool, cosmetics, pending);
    }

    public BodyData withModule(Mount mount, ItemStack stack) {
        Map<Mount, ItemStack> copy = new LinkedHashMap<>(modules);
        copy.put(mount, stack);
        return new BodyData(coreSocket, sections, copy, activeTool, cosmetics, pending);
    }

    public BodyData withActiveTool(int index) {
        return new BodyData(coreSocket, sections, modules, index, cosmetics, pending);
    }

    public BodyData withCosmetics(Cosmetics value) {
        return new BodyData(coreSocket, sections, modules, activeTool, value, pending);
    }

    public BodyData withPending(List<ItemStack> value) {
        return new BodyData(coreSocket, sections, modules, activeTool, cosmetics, value);
    }

    public BodyData plusPending(List<ItemStack> more) {
        List<ItemStack> all = new ArrayList<>(pending);
        all.addAll(more);
        return withPending(all);
    }

    /** Same content (item stacks compared by item, count and components). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BodyData other)) {
            return false;
        }
        return coreSocket.equals(other.coreSocket) && activeTool == other.activeTool && cosmetics.equals(other.cosmetics)
                && sameStacks(sections, other.sections) && sameStacks(modules, other.modules)
                && ItemStack.listMatches(pending, other.pending);
    }

    private static <K> boolean sameStacks(Map<K, ItemStack> a, Map<K, ItemStack> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Map.Entry<K, ItemStack> entry : a.entrySet()) {
            if (!ItemStack.matches(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return new HashMap<>(Map.of("core", coreSocket, "sections", sections.keySet(), "modules", modules.keySet())).hashCode();
    }
}
