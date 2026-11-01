package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.platform.NativeImage;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.display.HoloMapImage;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.dust.DustField;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

/**
 * The textures of map holograms. A texture is the server's picture with the dust cloud tinted over it, remade when the
 * picture changes. Only a few are kept, so a removed projector cannot leak one.
 */
final class HoloMaps {
    private static final int KEPT = 16;
    private static final int DUST_ALPHA_MAX = 180;
    private static final int DUST_COLOR = 0x66E6FF;

    private static final class Entry {
        final ResourceLocation id;
        final DynamicTexture texture = new DynamicTexture(HoloMapImage.SIZE, HoloMapImage.SIZE, false);
        int revision = -1;

        Entry(long key) {
            id = VoidReclaimer.id("holo_map/" + Long.toHexString(key));
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
    }

    private static final Map<Long, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Entry> eldest) {
            if (size() > KEPT) {
                Minecraft.getInstance().getTextureManager().release(eldest.getValue().id);
                return true;
            }
            return false;
        }
    };

    private HoloMaps() {}

    /** The texture for a projector's picture, brought up to date. */
    static ResourceLocation texture(HoloProjectorBlockEntity projector) {
        Entry entry = ENTRIES.computeIfAbsent(projector.getBlockPos().asLong(), Entry::new);
        if (entry.revision != projector.revision()) {
            entry.revision = projector.revision();
            fill(entry.texture, projector);
            entry.texture.upload();
        }
        return entry.id;
    }

    private static void fill(DynamicTexture texture, HoloProjectorBlockEntity projector) {
        Level level = projector.getLevel();
        byte[] map = projector.mapImage();
        NativeImage pixels = texture.getPixels();
        BlockPos center = projector.getBlockPos();
        int radius = projector.mapRadius();
        DustField dust = DustField.of(level);
        for (int y = 0; y < HoloMapImage.SIZE; y++) {
            for (int x = 0; x < HoloMapImage.SIZE; x++) {
                int packed = map.length == HoloMapImage.SIZE * HoloMapImage.SIZE ? map[y * HoloMapImage.SIZE + x] & 0xFF : 0;
                // getColorFromPackedId gives ABGR, which is what NativeImage holds; 0 is clear.
                int color = packed == 0 ? 0 : MapColor.getColorFromPackedId(packed);
                double density = dust == null ? 0 : dust.sample(new Vec3(center.getX() + HoloMapImage.offset(x, radius), center.getY(),
                        center.getZ() + HoloMapImage.offset(y, radius))).density();
                pixels.setPixelRGBA(x, y, over(color, density));
            }
        }
    }

    /** Lays the cloud's tint over a pixel, more where the cloud is denser. */
    static int over(int abgr, double density) {
        if (density <= 0) {
            return abgr;
        }
        int b = DUST_COLOR & 0xFF;
        int g = DUST_COLOR >> 8 & 0xFF;
        int r = DUST_COLOR >> 16 & 0xFF;
        int alpha = (int) (Math.min(1, density) * DUST_ALPHA_MAX);
        if ((abgr >>> 24) == 0) {
            return alpha << 24 | b << 16 | g << 8 | r;
        }
        int inv = 255 - alpha;
        int outB = ((abgr >> 16 & 0xFF) * inv + b * alpha) / 255;
        int outG = ((abgr >> 8 & 0xFF) * inv + g * alpha) / 255;
        int outR = ((abgr & 0xFF) * inv + r * alpha) / 255;
        return 0xFF000000 | outB << 16 | outG << 8 | outR;
    }
}
