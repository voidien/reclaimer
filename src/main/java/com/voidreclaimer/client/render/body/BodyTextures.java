package com.voidreclaimer.client.render.body;

import com.mojang.blaze3d.platform.NativeImage;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Body and section textures with a casing material painted into their casing swatch (the top-left 16x16 of each art texture,
 * which the casing bones sample). Built on first use, one dynamic texture per art texture and casing, and thrown away when
 * resources reload.
 */
public final class BodyTextures implements ResourceManagerReloadListener {
    public static final BodyTextures INSTANCE = new BodyTextures();

    private final Map<String, ResourceLocation> cache = new HashMap<>();

    private BodyTextures() {}

    /** Art texture of a body rig or a part: {@code textures/entity/body/NAME.png}. */
    public static ResourceLocation art(String name) {
        return VoidReclaimer.id("textures/entity/body/" + name + ".png");
    }

    /** The accent mask of a part: white where the player's accent colour is applied. */
    public static ResourceLocation accent(String name) {
        return VoidReclaimer.id("textures/entity/body/" + name + "_accent.png");
    }

    /**
     * The art texture {@code name} with the casing's material in its swatch. Falls back to the plain art texture when the
     * casing has no texture or a file can't be read.
     */
    public ResourceLocation painted(String name, RegistryAccess access, ResourceKey<Casing> casingKey) {
        ResourceLocation art = art(name);
        Optional<ResourceLocation> swatch = access.registry(VoidReclaimerRegistries.CASING)
                .flatMap(registry -> registry.getOptional(casingKey)).flatMap(Casing::texture);
        if (swatch.isEmpty()) {
            return art;
        }
        String key = name + "|" + swatch.get();
        ResourceLocation cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        ResourceLocation built = build(art, swatch.get(), key);
        cache.put(key, built);
        return built;
    }

    private ResourceLocation build(ResourceLocation art, ResourceLocation swatchTexture, String key) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ResourceLocation swatchFile = swatchTexture.withPath(path -> "textures/" + path + ".png");
        try (NativeImage base = read(resources, art); NativeImage swatch = read(resources, swatchFile)) {
            int[] out = BodyLook.paintCasing(pixels(base), base.getWidth(), pixels(swatch), swatch.getWidth());
            NativeImage painted = new NativeImage(base.getWidth(), base.getHeight(), false);
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    painted.setPixelRGBA(x, y, out[y * base.getWidth() + x]);
                }
            }
            ResourceLocation id = VoidReclaimer.id("dynamic/body/" + Integer.toHexString(key.hashCode()) + "_" + art.getPath().hashCode());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(painted));
            return id;
        } catch (IOException e) {
            VoidReclaimer.LOGGER.warn("Could not paint casing {} into {}: {}", swatchTexture, art, e.toString());
            return art;
        }
    }

    private static NativeImage read(ResourceManager resources, ResourceLocation file) throws IOException {
        try (InputStream in = resources.getResourceOrThrow(file).open()) {
            return NativeImage.read(in);
        }
    }

    private static int[] pixels(NativeImage image) {
        int[] out = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                out[y * image.getWidth() + x] = image.getPixelRGBA(x, y);
            }
        }
        return out;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        var textures = Minecraft.getInstance().getTextureManager();
        cache.values().forEach(textures::release);
        cache.clear();
    }
}
