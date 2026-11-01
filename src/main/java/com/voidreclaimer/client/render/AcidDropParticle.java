package com.voidreclaimer.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A yellow-green drop of Acid Drizzle: it falls straight down and is gone when it lands. */
public final class AcidDropParticle extends TextureSheetParticle {
    private static final float FALL_SPEED = -0.7f;

    private AcidDropParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        setColor(0.72f, 0.82f, 0.22f);
        setAlpha(0.8f);
        gravity = 0f;
        yd = FALL_SPEED;
        lifetime = 40;
        quadSize = 0.08f;
    }

    @Override
    public void tick() {
        super.tick();
        yd = FALL_SPEED;
        if (onGround) {
            remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new AcidDropParticle(level, x, y, z, sprites);
        }
    }
}
