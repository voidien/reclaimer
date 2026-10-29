package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A saved body design (the {@code void_reclaimer:blueprint} component): which item goes in each socket and on each
 * mount, plus the looks. Only item types are kept, not conditions or other item data.
 */
public record Blueprint(String coreSocket, Map<String, Item> sections, Map<BodyData.Mount, Item> modules, Cosmetics cosmetics) {
    private record ModuleEntry(BodyData.Mount mount, Item item) {
        static final Codec<ModuleEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BodyData.Mount.CODEC.fieldOf("mount").forGetter(ModuleEntry::mount),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ModuleEntry::item)
        ).apply(instance, ModuleEntry::new));
        static final StreamCodec<RegistryFriendlyByteBuf, ModuleEntry> STREAM_CODEC = StreamCodec.composite(
                BodyData.Mount.STREAM_CODEC, ModuleEntry::mount,
                ByteBufCodecs.registry(Registries.ITEM), ModuleEntry::item,
                ModuleEntry::new);
    }

    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("core_socket").forGetter(Blueprint::coreSocket),
            Codec.unboundedMap(Codec.STRING, BuiltInRegistries.ITEM.byNameCodec()).fieldOf("sections").forGetter(Blueprint::sections),
            ModuleEntry.CODEC.listOf().fieldOf("modules").forGetter(Blueprint::moduleEntries),
            Cosmetics.CODEC.optionalFieldOf("cosmetics", Cosmetics.DEFAULT).forGetter(Blueprint::cosmetics)
    ).apply(instance, (core, sections, modules, looks) -> new Blueprint(core, sections, toMap(modules), looks)));

    public static final StreamCodec<RegistryFriendlyByteBuf, Blueprint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Blueprint::coreSocket,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.registry(Registries.ITEM)), Blueprint::sections,
            ModuleEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), Blueprint::moduleEntries,
            Cosmetics.STREAM_CODEC, Blueprint::cosmetics,
            (core, sections, modules, looks) -> new Blueprint(core, sections, toMap(modules), looks));

    public Blueprint {
        sections = Map.copyOf(sections);
        modules = Map.copyOf(modules);
    }

    /** The design of {@code body}. */
    public static Blueprint of(BodyData body) {
        Map<String, Item> sections = new LinkedHashMap<>();
        body.sections().forEach((socket, stack) -> sections.put(socket, stack.getItem()));
        Map<BodyData.Mount, Item> modules = new LinkedHashMap<>();
        body.modules().forEach((mount, stack) -> modules.put(mount, stack.getItem()));
        return new Blueprint(body.coreSocket(), sections, modules, body.cosmetics());
    }

    public Item core() {
        return sections.getOrDefault(coreSocket, net.minecraft.world.item.Items.AIR);
    }

    private List<ModuleEntry> moduleEntries() {
        return modules.entrySet().stream().map(e -> new ModuleEntry(e.getKey(), e.getValue())).toList();
    }

    private static Map<BodyData.Mount, Item> toMap(List<ModuleEntry> entries) {
        Map<BodyData.Mount, Item> map = new LinkedHashMap<>();
        entries.forEach(e -> map.put(e.mount(), e.item()));
        return map;
    }

    /** Whether {@code stack} is the item the design wants. */
    static boolean wants(Item wanted, ItemStack stack) {
        return !stack.isEmpty() && stack.is(wanted);
    }
}
