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
 * @param sockets         bones to hide when the socket has no section ({@code socket id -> bones}, children go with them)
 * @param poses           animation per pose key ({@code idle}, {@code thrust}, {@code brake}, {@code back}, {@code strafe_left},
 *                        {@code strafe_right}, {@code low_power}, {@code module_work}); a missing pose falls back
 *                        ({@link BodyLook#fallbacks})
 */
public record BodyRigSpec(ResourceLocation model, ResourceLocation texture, ResourceLocation animations, String animationPrefix,
                          boolean grounded, Map<String, List<String>> sockets, Map<String, String> poses) {
    public static final Codec<BodyRigSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(BodyRigSpec::model),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(BodyRigSpec::texture),
            ResourceLocation.CODEC.fieldOf("animations").forGetter(BodyRigSpec::animations),
            Codec.STRING.optionalFieldOf("animation_prefix", "").forGetter(BodyRigSpec::animationPrefix),
            Codec.BOOL.optionalFieldOf("grounded", false).forGetter(BodyRigSpec::grounded),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("sockets", Map.of()).forGetter(BodyRigSpec::sockets),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("poses", Map.of()).forGetter(BodyRigSpec::poses)
    ).apply(instance, BodyRigSpec::new));

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
