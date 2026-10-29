package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Encased Fan freezing: air blown through a powder snow block freezes the items it carries, by {@link FreezingRecipe}.
 * Registered in Create's fan processing registry. Checked before washing (priority 400), since powder snow is not water.
 */
public final class FreezingFanType implements FanProcessingType {
    private static final int PRIORITY = 450;
    private static final int COLOR = 0xD0F0FF;

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return Config.isEnabled("fanFreezing") && level.getBlockState(pos).is(Blocks.POWDER_SNOW);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    private static Optional<RecipeHolder<FreezingRecipe>> recipeFor(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor(ModRecipeTypes.FREEZING.get(), new SingleRecipeInput(stack), level);
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return Config.isEnabled("fanFreezing") && recipeFor(stack, level).isPresent();
    }

    @Override
    public List<ItemStack> process(ItemStack stack, Level level) {
        Optional<RecipeHolder<FreezingRecipe>> recipe = recipeFor(stack, level);
        if (recipe.isEmpty()) {
            return null;
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack result : recipe.get().value().results()) {
            ItemStack scaled = result.copy();
            scaled.setCount(result.getCount() * stack.getCount());
            out.add(scaled);
        }
        return out;
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.x + (level.random.nextFloat() - 0.5f) * 0.5f, pos.y + 0.25,
                    pos.z + (level.random.nextFloat() - 0.5f) * 0.5f, 0, 0.02, 0);
        }
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess particles, RandomSource random) {
        particles.setColor(COLOR);
        particles.setAlpha(1f);
        if (random.nextFloat() < 1 / 32f) {
            particles.spawnExtraParticle(ParticleTypes.SNOWFLAKE, .125f);
        }
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        // Items are the point; living things are left alone.
    }
}
