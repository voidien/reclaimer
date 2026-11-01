package com.voidreclaimer.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * A recipe condition that reads one of our config choices, so several recipes can stand for one item and the config picks between
 * them: {@code {"type": "void_reclaimer:config_choice", "key": "shellPrices", "value": "parts"}}. Checked when recipes load, so a
 * changed choice takes effect after a restart or {@code /reload}, like {@link ConfigCondition}.
 *
 * @param key   name of the choice
 * @param value the setting this recipe stands for, in lower case
 */
public record ConfigChoiceCondition(String key, String value) implements ICondition {
    public static final MapCodec<ConfigChoiceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("key").forGetter(ConfigChoiceCondition::key),
            com.mojang.serialization.Codec.STRING.fieldOf("value").forGetter(ConfigChoiceCondition::value)
    ).apply(instance, ConfigChoiceCondition::new));

    @Override
    public boolean test(IContext context) {
        return value.equals(Config.choice(key));
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
