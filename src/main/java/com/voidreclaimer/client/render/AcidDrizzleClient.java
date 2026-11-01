package com.voidreclaimer.client.render;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.EarthAtmosphere;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.registry.ModParticles;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Acid Drizzle as the player sees and hears it: drops falling around them and a quiet loop of rain, thinner as the planet heals. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class AcidDrizzleClient {
    private static final int MAX_DROPS = 14;
    private static final int RANGE = 14;
    /** Drops start this far above the player, or above the ground, so they fall through the view. */
    private static final int HEIGHT = 12;
    private static final int SOUND_TICKS = 60;

    private AcidDrizzleClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || !RuinedEarth.is(level) || !Config.EARTH_ACID_DRIZZLE.get()) {
            return;
        }
        float strength = EarthAtmosphere.drizzle(SectorMap.of(level), player.getBlockX(), player.getBlockZ());
        if (strength <= 0f) {
            return;
        }
        ParticleStatus particles = minecraft.options.particles().get();
        int drops = particles == ParticleStatus.MINIMAL ? 0 : EarthAtmosphere.drops(strength, particles == ParticleStatus.DECREASED ? MAX_DROPS / 2 : MAX_DROPS);
        RandomSource random = level.getRandom();
        for (int i = 0; i < drops; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 2 * RANGE;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 2 * RANGE;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            double y = Math.max(ground, player.getY()) + random.nextDouble() * HEIGHT;
            if (y >= ground) {
                level.addParticle(ModParticles.ACID_DROP.get(), x, y, z, 0, 0, 0);
            }
        }
        if (level.getGameTime() % SOUND_TICKS == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.ACID_DRIZZLE.get(), SoundSource.WEATHER, 0.3f * strength, 1f, false);
        }
    }
}
