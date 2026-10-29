package com.voidreclaimer.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A recipe condition that reads one of our config toggles ({@link Config#TOGGLES}), so a chain or machine recipe can be
 * switched off without a datapack: {@code {"type": "void_reclaimer:config", "key": "iceChain"}}. Conditions are checked when
 * recipes load, so a changed toggle takes effect after a restart or {@code /reload}.
 *
 * @param key name of the toggle
 */
public record ConfigCondition(String key) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key)
    ).apply(instance, ConfigCondition::new));

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, VoidReclaimer.MOD_ID);

    static {
        CONDITIONS.register("config", () -> CODEC);
    }

    @Override
    public boolean test(IContext context) {
        return Config.isEnabled(key);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
