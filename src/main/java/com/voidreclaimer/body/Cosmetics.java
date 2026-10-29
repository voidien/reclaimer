package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.api.body.Cosmetic;
import com.voidreclaimer.api.body.Trail;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.registry.ModCodecs;
import com.voidreclaimer.registry.OrderedRegistry;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * How the body looks: a Create-style casing material, a free accent tint (lights, trim, pipes) and a thruster trail.
 * Casings and trails are entries of their datapack registries. Each section may override the casing and accent
 * ({@link #sections}, by socket). Chosen in the Body tab; rendered on the model in Phase 5b (trails are drawn already).
 */
public record Cosmetics(ResourceKey<Casing> casing, int accent, ResourceKey<Trail> trail, Map<String, SectionPaint> sections) {
    public static final ResourceKey<Casing> ANDESITE = casingKey("andesite");
    public static final ResourceKey<Casing> BRASS = casingKey("brass");
    public static final ResourceKey<Casing> COPPER = casingKey("copper");

    public static final ResourceKey<Trail> NO_TRAIL = trailKey("none");
    public static final ResourceKey<Trail> STANDARD = trailKey("standard");
    public static final ResourceKey<Trail> EMBER = trailKey("ember");
    public static final ResourceKey<Trail> PLASMA = trailKey("plasma");
    public static final ResourceKey<Trail> ACCENT = trailKey("accent");

    public static final Map<ResourceKey<Casing>, Casing> BUILTIN_CASINGS = builtinCasings();
    public static final Map<ResourceKey<Trail>, Trail> BUILTIN_TRAILS = builtinTrails();

    public static final int DEFAULT_ACCENT = 0x7FD4FF;
    public static final Cosmetics DEFAULT = new Cosmetics(ANDESITE, DEFAULT_ACCENT, STANDARD, Map.of());

    /** A section's own paint; empty parts use the body's. */
    public record SectionPaint(Optional<ResourceKey<Casing>> casing, Optional<Integer> accent) {
        public static final SectionPaint NONE = new SectionPaint(Optional.empty(), Optional.empty());
        public static final Codec<SectionPaint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ModCodecs.key(VoidReclaimerRegistries.CASING).optionalFieldOf("casing").forGetter(SectionPaint::casing),
                Codec.INT.optionalFieldOf("accent").forGetter(SectionPaint::accent)
        ).apply(instance, SectionPaint::new));
        public static final StreamCodec<ByteBuf, SectionPaint> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ResourceKey.streamCodec(VoidReclaimerRegistries.CASING)), SectionPaint::casing,
                ByteBufCodecs.optional(ByteBufCodecs.INT), SectionPaint::accent,
                SectionPaint::new);

        public boolean isEmpty() {
            return casing.isEmpty() && accent.isEmpty();
        }
    }

    public Cosmetics {
        Map<String, SectionPaint> copy = new java.util.TreeMap<>();
        sections.forEach((socket, paint) -> {
            if (!paint.isEmpty()) {
                copy.put(socket, paint);
            }
        });
        sections = java.util.Collections.unmodifiableMap(copy);
    }

    public Cosmetics(ResourceKey<Casing> casing, int accent, ResourceKey<Trail> trail) {
        this(casing, accent, trail, Map.of());
    }

    /** Older saves wrote bare names ("andesite"); {@link ModCodecs#key} reads them as ours. */
    public static final Codec<Cosmetics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModCodecs.key(VoidReclaimerRegistries.CASING).optionalFieldOf("casing", ANDESITE).forGetter(Cosmetics::casing),
            Codec.INT.optionalFieldOf("accent", DEFAULT_ACCENT).forGetter(Cosmetics::accent),
            ModCodecs.key(VoidReclaimerRegistries.TRAIL).optionalFieldOf("trail", STANDARD).forGetter(Cosmetics::trail),
            Codec.unboundedMap(Codec.STRING, SectionPaint.CODEC).lenientOptionalFieldOf("sections", Map.of()).forGetter(Cosmetics::sections)
    ).apply(instance, Cosmetics::new));

    public static final StreamCodec<ByteBuf, Cosmetics> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(VoidReclaimerRegistries.CASING), Cosmetics::casing,
            ByteBufCodecs.INT, Cosmetics::accent,
            ResourceKey.streamCodec(VoidReclaimerRegistries.TRAIL), Cosmetics::trail,
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, SectionPaint.STREAM_CODEC), Cosmetics::sections,
            Cosmetics::new);

    public Cosmetics withCasing(ResourceKey<Casing> value) {
        return new Cosmetics(value, accent, trail, sections);
    }

    public Cosmetics withAccent(int value) {
        return new Cosmetics(casing, value & 0xFFFFFF, trail, sections);
    }

    public Cosmetics withTrail(ResourceKey<Trail> value) {
        return new Cosmetics(casing, accent, value, sections);
    }

    /** The paint override of the section in {@code socket} ({@link SectionPaint#NONE} if none). */
    public SectionPaint paint(String socket) {
        return sections.getOrDefault(socket, SectionPaint.NONE);
    }

    public Cosmetics withPaint(String socket, SectionPaint paint) {
        Map<String, SectionPaint> copy = new java.util.HashMap<>(sections);
        copy.put(socket, paint);
        return new Cosmetics(casing, accent, trail, copy);
    }

    /** The casing the section in {@code socket} shows. */
    public ResourceKey<Casing> casing(String socket) {
        return paint(socket).casing().orElse(casing);
    }

    /** The accent tint the section in {@code socket} shows. */
    public int accent(String socket) {
        return paint(socket).accent().orElse(accent);
    }

    public static List<OrderedRegistry.Entry<Casing>> casings(RegistryAccess access) {
        return OrderedRegistry.sorted(access, VoidReclaimerRegistries.CASING, Casing::order);
    }

    public static List<OrderedRegistry.Entry<Trail>> trails(RegistryAccess access) {
        return OrderedRegistry.sorted(access, VoidReclaimerRegistries.TRAIL, Trail::order);
    }

    /**
     * The entry after {@code current} in the registry's order that the player may pick, wrapping around.
     * Returns {@code current} when nothing else is available.
     */
    public static <T extends Cosmetic> ResourceKey<T> next(RegistryAccess access, ResourceKey<? extends Registry<T>> registry,
                                                           ToIntFunction<T> order, ResourceKey<T> current,
                                                           Predicate<ResourceKey<Milestone>> unlocked) {
        List<OrderedRegistry.Entry<T>> entries = OrderedRegistry.sorted(access, registry, order);
        int start = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).key().equals(current)) {
                start = i + 1;
                break;
            }
        }
        for (int step = 0; step < entries.size(); step++) {
            OrderedRegistry.Entry<T> entry = entries.get((start + step) % entries.size());
            if (isAvailable(entry.value(), unlocked)) {
                return entry.key();
            }
        }
        return current;
    }

    public static boolean isAvailable(Cosmetic cosmetic, Predicate<ResourceKey<Milestone>> unlocked) {
        return cosmetic.requires().map(unlocked::test).orElse(true);
    }

    /** Display name of a casing or trail; the bare id if it's no longer registered. */
    public static <T extends Cosmetic> Component name(RegistryAccess access, ResourceKey<? extends Registry<T>> registry, ResourceKey<T> key) {
        T value = OrderedRegistry.get(access, registry, key);
        return value == null ? Component.literal(key.location().getPath()) : value.displayName(key.location());
    }

    private static ResourceKey<Casing> casingKey(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.CASING, VoidReclaimer.id(name));
    }

    private static ResourceKey<Trail> trailKey(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.TRAIL, VoidReclaimer.id(name));
    }

    private static Map<ResourceKey<Casing>, Casing> builtinCasings() {
        Map<ResourceKey<Casing>, Casing> map = new LinkedHashMap<>();
        map.put(ANDESITE, casing(0, "andesite_casing"));
        map.put(BRASS, casing(1, "brass_casing"));
        map.put(COPPER, casing(2, "copper_casing"));
        return Map.copyOf(map);
    }

    private static Casing casing(int order, String createTexture) {
        return new Casing(order, Optional.empty(), Optional.of(ResourceLocation.fromNamespaceAndPath("create", "block/" + createTexture)),
                Optional.empty());
    }

    private static Map<ResourceKey<Trail>, Trail> builtinTrails() {
        Map<ResourceKey<Trail>, Trail> map = new LinkedHashMap<>();
        map.put(NO_TRAIL, new Trail(Optional.empty(), false, 0, Optional.empty(), Optional.empty()));
        map.put(STANDARD, trail(1, 0xDDE6EE));
        map.put(EMBER, trail(2, 0xFF8A3D));
        map.put(PLASMA, trail(3, 0x7FD4FF));
        map.put(ACCENT, new Trail(Optional.empty(), true, 4, Optional.empty(), Optional.empty()));
        return Map.copyOf(map);
    }

    private static Trail trail(int order, int color) {
        return new Trail(Optional.of(color), true, order, Optional.empty(), Optional.empty());
    }
}
