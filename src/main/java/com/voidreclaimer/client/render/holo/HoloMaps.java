package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.platform.NativeImage;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.display.HoloMapImage;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.ship.Ships;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

/**
 * The textures of map holograms: a fixed pool, because {@code RenderType.text} keeps every id it is given for good, and a
 * texture per place would grow with every place ever shown. A slot holds the server's picture with the dust cloud tinted
 * over it, remade when the picture changes. A map past the last slot in one tick waits for a free one.
 */
final class HoloMaps {
    static final int SLOTS = 16;
    private static final int DUST_ALPHA_MAX = 180;
    private static final int DUST_COLOR = 0x66E6FF;

    private static final class Slot {
        final ResourceLocation id;
        final DynamicTexture texture = new DynamicTexture(HoloMapImage.SIZE, HoloMapImage.SIZE, false);
        /** Weak, so a slot never keeps a level alive. A rebuilt projector is a new object, so it never sees the old picture. */
        WeakReference<HoloProjectorBlockEntity> shown = new WeakReference<>(null);
        int revision;

        Slot(int index) {
            id = VoidReclaimer.id("holo_map/" + index);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
    }

    private static final Slot[] POOL = new Slot[SLOTS];
    private static final long[] USED = new long[SLOTS];

    static {
        Arrays.fill(USED, Long.MIN_VALUE);
    }

    private HoloMaps() {}

    /** The texture for a projector's picture, brought up to date; null when every slot shows another map this tick. */
    @Nullable
    static ResourceLocation texture(HoloProjectorBlockEntity projector, long gameTime) {
        int index = find(projector);
        if (index < 0) {
            index = pick(USED, gameTime);
            if (index < 0) {
                return null;
            }
            if (POOL[index] == null) {
                POOL[index] = new Slot(index);
            }
            POOL[index].shown = new WeakReference<>(projector);
            POOL[index].revision = projector.revision() - 1;
        }
        Slot slot = POOL[index];
        USED[index] = gameTime;
        if (slot.revision != projector.revision()) {
            slot.revision = projector.revision();
            fill(slot.texture, projector);
            slot.texture.upload();
        }
        return slot.id;
    }

    private static int find(HoloProjectorBlockEntity projector) {
        for (int i = 0; i < SLOTS; i++) {
            if (POOL[i] != null && POOL[i].shown.get() == projector) {
                return i;
            }
        }
        return -1;
    }

    /** The slot to take for a new map: the one unused longest, or -1 when all were used this tick. */
    static int pick(long[] used, long now) {
        int best = -1;
        for (int i = 0; i < used.length; i++) {
            if (used[i] != now && (best < 0 || used[i] < used[best])) {
                best = i;
            }
        }
        return best;
    }

    private static void fill(DynamicTexture texture, HoloProjectorBlockEntity projector) {
        Level level = projector.getLevel();
        byte[] map = projector.mapImage();
        NativeImage pixels = texture.getPixels();
        BlockPos center = projector.getBlockPos();
        int radius = projector.mapRadius();
        // A ship's map is its own deck plan, in the coordinates of its plot, where the dust field means nothing.
        DustField dust = Ships.shipAt(level, center) != null ? null : DustField.of(level);
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
