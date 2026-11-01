package com.voidreclaimer.client.render.body;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.VoidReclaimer;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * A body rig drawn as one whole model with its own bones and animations (an RD model promoted with
 * {@code --promote-as}), instead of the composed rig with sections plugged into sockets. Read from
 * {@code assets/void_reclaimer/body_rigs/<rig>.json}; a rig without a file is composed.
 *
 * @param model           the whole GeckoLib model
 * @param texture         its texture (no casing swatch or accent mask: the art is painted as is)
 * @param animations      its animation file
 * @param animationPrefix put before each name in {@code poses} to get the animation id
 * @param grounded        the model's origin is at its feet (else at its middle)
 * @param offsetY         pixels to move the model up (down when negative), at the body's scale (the probe's middle at the hitbox's)
 * @param yaw             degrees to turn the model so its front faces where the body looks (180 when the model's front is +Z)
 * @param pitches         the body also nose-dives and climbs with the look pitch (ships); else it only turns
 * @param lean            how far the model leans into its motion (a forward lean, a bank when strafing), so one flying animation
 *                        covers every direction; none by default
 * @param sockets         bones to hide, with their children, when the socket has no section ({@code socket id -> bones})
 * @param socketParts     bones to hide when the socket has no section, without their children (a neck that carries the head)
 * @param mounts          where modules sit: {@code rig mount name -> bone and offset}; the name is
 *                        {@code mount_<kind>_<n>} on the core socket and {@code <socket>__mount_<kind>_<n>} elsewhere
 *                        ({@link BodyLook#rigMountBone}). A mount with no entry is not drawn.
 * @param layers          extra animations played on top of the main one, each only moving the bones it keys: {@code layer name -> (pose key ->
 *                        animation)}, in name order (at most {@link #MAX_LAYERS}). A pose that has a layer entry is no longer played by the
 *                        main animation for {@code module_work}, so an arms-only swing can run while the body keeps flying
 * @param poses           animation per pose key ({@code idle}, {@code thrust}, {@code brake}, {@code back}, {@code strafe_left},
 *                        {@code strafe_right}, {@code low_power}, {@code module_work}); a missing pose falls back
 *                        ({@link BodyLook#fallbacks})
 */
public record BodyRigSpec(ResourceLocation model, ResourceLocation texture, ResourceLocation animations, String animationPrefix,
                          boolean grounded, float offsetY, float yaw, boolean pitches, Lean lean, Map<String, List<String>> sockets,
                          Map<String, List<String>> socketParts, Map<String, Mount> mounts, Map<String, String> poses,
                          Map<String, Map<String, String>> layers) {
    public static final int MAX_LAYERS = 3;

    /**
     * A module position on a rig: the bone it follows, an offset from that bone's pivot in pixels (the same axes as the
     * pivots in the {@code .geo.json}), and a turn about the vertical axis in degrees.
     */
    public record Mount(String bone, List<Double> offset, float yaw, float scale) {
        public static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Mount::bone),
                Codec.DOUBLE.listOf().optionalFieldOf("offset", List.of(0.0, 0.0, 0.0)).forGetter(Mount::offset),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Mount::yaw),
                Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Mount::scale)
        ).apply(instance, Mount::new));

        public Mount {
            offset = offset.size() == 3 ? List.copyOf(offset) : List.of(0.0, 0.0, 0.0);
        }

        public float x() {
            return offset.get(0).floatValue();
        }

        public float y() {
            return offset.get(1).floatValue();
        }

        public float z() {
            return offset.get(2).floatValue();
        }
    }

    /**
     * Procedural lean toward the direction of motion: up to {@code pitch} degrees forward (backward when flying back) and
     * {@code roll} degrees banked toward a strafe, reached at {@code referenceSpeed} blocks per tick.
     */
    public record Lean(float pitch, float roll, float referenceSpeed) {
        public static final Lean NONE = new Lean(0f, 0f, 0.4f);
        public static final Codec<Lean> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Lean::pitch),
                Codec.FLOAT.optionalFieldOf("roll", 0f).forGetter(Lean::roll),
                Codec.FLOAT.optionalFieldOf("reference_speed", 0.4f).forGetter(Lean::referenceSpeed)
        ).apply(instance, Lean::new));

        public Lean {
            referenceSpeed = Math.max(1.0E-3f, referenceSpeed);
        }

        public boolean isNone() {
            return pitch == 0f && roll == 0f;
        }
    }

    public static final Codec<BodyRigSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(BodyRigSpec::model),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BodyRigSpec::texture),
            ResourceLocation.CODEC.fieldOf("animations").forGetter(BodyRigSpec::animations),
            Codec.STRING.optionalFieldOf("animation_prefix", "").forGetter(BodyRigSpec::animationPrefix),
            Codec.BOOL.optionalFieldOf("grounded", false).forGetter(BodyRigSpec::grounded),
            Codec.FLOAT.optionalFieldOf("offset_y", 0f).forGetter(BodyRigSpec::offsetY),
            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(BodyRigSpec::yaw),
            Codec.BOOL.optionalFieldOf("pitches", false).forGetter(BodyRigSpec::pitches),
            Lean.CODEC.optionalFieldOf("lean", Lean.NONE).forGetter(BodyRigSpec::lean),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("sockets", Map.of()).forGetter(BodyRigSpec::sockets),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("socket_parts", Map.of()).forGetter(BodyRigSpec::socketParts),
            Codec.unboundedMap(Codec.STRING, Mount.CODEC).optionalFieldOf("mounts", Map.of()).forGetter(BodyRigSpec::mounts),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("poses", Map.of()).forGetter(BodyRigSpec::poses),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING)).optionalFieldOf("layers", Map.of()).forGetter(BodyRigSpec::layers)
    ).apply(instance, BodyRigSpec::new));

    /** The layers in name order, up to {@link #MAX_LAYERS}. */
    public List<Map<String, String>> layerList() {
        return layers.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(MAX_LAYERS).map(Map.Entry::getValue).toList();
    }

    /** The animation id layer {@code index} plays for a pose key, or null. */
    @Nullable
    public String layerAnimation(int index, String poseKey) {
        List<Map<String, String>> list = layerList();
        String name = index < list.size() ? list.get(index).get(poseKey) : null;
        return name == null ? null : animationPrefix + name;
    }

    /** Whether some layer plays an animation for this pose key (so the main animation leaves it alone). */
    public boolean hasLayerFor(String poseKey) {
        return layerList().stream().anyMatch(layer -> layer.containsKey(poseKey));
    }

    /** The animation id for a pose key, or null when the rig has none for it. */
    @Nullable
    public String animation(String poseKey) {
        String name = poses.get(poseKey);
        return name == null ? null : animationPrefix + name;
    }

    /** The loaded specs, by rig name. */
    public static final class Loader implements ResourceManagerReloadListener {
        public static final Loader INSTANCE = new Loader();
        private static volatile Map<String, BodyRigSpec> specs = Map.of();

        private Loader() {}

        @Nullable
        public static BodyRigSpec get(String rig) {
            return specs.get(rig);
        }

        @Override
        public void onResourceManagerReload(ResourceManager resources) {
            Map<String, BodyRigSpec> loaded = new HashMap<>();
            resources.listResources("body_rigs", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                String path = id.getPath();
                String rig = path.substring("body_rigs/".length(), path.length() - ".json".length());
                try (Reader reader = resource.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(error -> VoidReclaimer.LOGGER.warn("Bad body rig {}: {}", id, error))
                            .ifPresent(spec -> loaded.put(rig, spec));
                } catch (Exception e) {
                    VoidReclaimer.LOGGER.warn("Could not read body rig {}: {}", id, e.toString());
                }
            });
            specs = Map.copyOf(loaded);
        }
    }
}
