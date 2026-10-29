package com.voidreclaimer.dust;

import com.mojang.serialization.Codec;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.dust.DustKind;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Looks up registered {@link DustKind}s. Kinds are referred to by id everywhere else (saves, costs, clouds). */
public final class DustKinds {
    public static final ResourceLocation METALLIC = VoidReclaimer.id("metallic");
    public static final ResourceLocation SILICATE = VoidReclaimer.id("silicate");
    public static final ResourceLocation ICE = VoidReclaimer.id("ice");
    public static final ResourceLocation CARBON = VoidReclaimer.id("carbon");

    /** The built-in kinds, exactly as datagen writes them; also used by unit tests. */
    public static final Map<ResourceLocation, DustKind> BUILTIN = builtin();

    /** A kind id; a bare name like {@code "metallic"} means {@code void_reclaimer:metallic} (older saves and recipes). */
    public static final Codec<ResourceLocation> ID_CODEC = Codec.STRING.comapFlatMap(
            s -> ResourceLocation.read(s.indexOf(':') >= 0 ? s : VoidReclaimer.MOD_ID + ":" + s),
            ResourceLocation::toString);

    private static final int UNKNOWN_COLOR = 0x808080;

    private DustKinds() {}

    /** A registered kind with its id. */
    public record Entry(ResourceLocation id, DustKind kind) {}

    /** Sort order shared by client and server: {@link DustKind#order()}, then id. */
    public static final Comparator<Entry> ORDER =
            Comparator.comparingInt((Entry e) -> e.kind().order()).thenComparing(Entry::id);

    /** All registered kinds in {@link #ORDER}. */
    public static List<Entry> all(RegistryAccess access) {
        return access.registry(VoidReclaimerRegistries.DUST_KIND)
                .map(registry -> registry.entrySet().stream()
                        .map(e -> new Entry(e.getKey().location(), e.getValue()))
                        .sorted(ORDER)
                        .toList())
                .orElse(List.of());
    }

    /** The built-in kinds as entries, in {@link #ORDER}. */
    public static List<Entry> builtinEntries() {
        return BUILTIN.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .sorted(ORDER)
                .toList();
    }

    /** Picks a kind from a uniform value in [0, 1) by cloud weight; null if no kind can appear in clouds. */
    @Nullable
    public static ResourceLocation pick(List<Entry> kinds, double roll) {
        double total = 0;
        for (Entry entry : kinds) {
            total += entry.kind().cloudWeight();
        }
        if (total <= 0) {
            return null;
        }
        double target = roll * total;
        ResourceLocation last = null;
        for (Entry entry : kinds) {
            if (entry.kind().cloudWeight() <= 0) {
                continue;
            }
            last = entry.id();
            target -= entry.kind().cloudWeight();
            if (target < 0) {
                return entry.id();
            }
        }
        return last;
    }

    @Nullable
    public static DustKind get(RegistryAccess access, ResourceLocation id) {
        return access.registry(VoidReclaimerRegistries.DUST_KIND).map(registry -> registry.get(id)).orElse(null);
    }

    /** HUD color of a kind; grey for ids that are no longer registered. */
    public static int color(RegistryAccess access, ResourceLocation id) {
        DustKind kind = get(access, id);
        return kind == null ? UNKNOWN_COLOR : kind.color();
    }

    public static Component name(RegistryAccess access, ResourceLocation id) {
        DustKind kind = get(access, id);
        return kind == null ? Component.literal(id.toString()) : kind.displayName(id);
    }

    private static Map<ResourceLocation, DustKind> builtin() {
        Map<ResourceLocation, DustKind> map = new LinkedHashMap<>();
        // Order and weights match the Phase 3 enum, so existing worlds keep their cloud kinds.
        map.put(METALLIC, new DustKind(0xB8C2CC, 0.30, 0));
        map.put(SILICATE, new DustKind(0xD9B38C, 0.35, 1));
        map.put(ICE, new DustKind(0x9FE3FF, 0.20, 2));
        map.put(CARBON, new DustKind(0x6E6A80, 0.15, 3));
        return Map.copyOf(map);
    }
}
