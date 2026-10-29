package com.voidreclaimer.replicator;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

/** The built-in milestones and lookups in the {@code void_reclaimer:milestone} registry. */
public final class Milestones {
    /** Granted on first login: the AI comes online. */
    public static final ResourceKey<Milestone> AWAKENING = key("awakening");
    /** Any dust collected. */
    public static final ResourceKey<Milestone> FIRST_DUST = key("first_dust");
    /** Placed {@link #STRUCTURE_BLOCKS} blocks. Unlocks the Tether Anchor. */
    public static final ResourceKey<Milestone> FIRST_STRUCTURE = key("first_structure");
    /** Holding three different kinds of dust at once. Unlocks the Backup Core. */
    public static final ResourceKey<Milestone> DIVERSIFIED = key("diversified");
    /** Any one dust kind filled to capacity. Unlocks Mk II parts and the Mech frame. */
    public static final ResourceKey<Milestone> EXPANSION = key("expansion");

    public static final int STRUCTURE_BLOCKS = 10;

    /** The built-in entries, exactly as datagen writes them. */
    public static final Map<ResourceKey<Milestone>, Milestone> BUILTIN = builtin();

    private static final Comparator<Entry> ORDER =
            Comparator.comparingInt((Entry e) -> e.milestone().order()).thenComparing(e -> e.key().location());

    private Milestones() {}

    public record Entry(ResourceKey<Milestone> key, Milestone milestone) {}

    /** Registered milestones in checking and log order. */
    public static List<Entry> all(RegistryAccess access) {
        return access.registry(VoidReclaimerRegistries.MILESTONE)
                .map(registry -> registry.entrySet().stream()
                        .map(e -> new Entry(e.getKey(), e.getValue()))
                        .sorted(ORDER)
                        .toList())
                .orElse(List.of());
    }

    @Nullable
    public static Milestone get(RegistryAccess access, ResourceKey<Milestone> key) {
        return access.registry(VoidReclaimerRegistries.MILESTONE).map(registry -> registry.get(key)).orElse(null);
    }

    public static Component title(RegistryAccess access, ResourceKey<Milestone> key) {
        return text(access, key, Milestone::title);
    }

    public static Component hint(RegistryAccess access, ResourceKey<Milestone> key) {
        return text(access, key, Milestone::hint);
    }

    public static Component logTitle(RegistryAccess access, ResourceKey<Milestone> key) {
        return text(access, key, Milestone::logTitle);
    }

    public static Component logText(RegistryAccess access, ResourceKey<Milestone> key) {
        return text(access, key, Milestone::logText);
    }

    /** The most blocks any registered milestone asks for; placements are counted up to this. */
    public static int blocksNeeded(RegistryAccess access) {
        int needed = 0;
        for (Entry entry : all(access)) {
            if (entry.milestone().criterion() instanceof MilestoneCriteria.BlocksPlaced placed) {
                needed = Math.max(needed, placed.count());
            }
        }
        return needed;
    }

    private interface Text {
        Component of(Milestone milestone, net.minecraft.resources.ResourceLocation id);
    }

    private static Component text(RegistryAccess access, ResourceKey<Milestone> key, Text text) {
        Milestone milestone = get(access, key);
        // A milestone a datapack removed still shows its translated name.
        return text.of(milestone == null ? new Milestone(MilestoneCriteria.Always.INSTANCE, 0) : milestone, key.location());
    }

    private static ResourceKey<Milestone> key(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.MILESTONE, VoidReclaimer.id(name));
    }

    private static Map<ResourceKey<Milestone>, Milestone> builtin() {
        Map<ResourceKey<Milestone>, Milestone> map = new LinkedHashMap<>();
        map.put(AWAKENING, new Milestone(MilestoneCriteria.Always.INSTANCE, 0));
        map.put(FIRST_DUST, new Milestone(new MilestoneCriteria.DustKindsHeld(1), 1));
        map.put(FIRST_STRUCTURE, new Milestone(new MilestoneCriteria.BlocksPlaced(STRUCTURE_BLOCKS), 2));
        map.put(DIVERSIFIED, new Milestone(new MilestoneCriteria.DustKindsHeld(3), 3));
        map.put(EXPANSION, new Milestone(new MilestoneCriteria.DustFull(Optional.empty()), 4));
        return Map.copyOf(map);
    }
}
