package com.voidreclaimer.salvage;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.body.ToolModules;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Breaking a salvageable block with the Salvage Tool module active (empty hand) removes it without drops and refunds a fraction of the dust its
 * Replicator recipe cost (per block, so a recipe making 8 Scaffold refunds 1/8 of its cost per block).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Salvage {
    private Salvage() {}

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(ModBlocks.SALVAGEABLE) || Modules.activeType(player) != ToolModules.SALVAGE.get()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        event.setCanceled(true);
        level.destroyBlock(pos, false, player);

        DustCost refund = refundFor(level.getRecipeManager(), state.getBlock().asItem(), Config.SALVAGE_REFUND.get().floatValue());
        if (!refund.isFree()) {
            DustData dust = player.getData(ModAttachments.DUST);
            player.setData(ModAttachments.DUST, refund.giveTo(dust, DustData.capacity(player)));
            player.syncData(ModAttachments.DUST);
        }
    }

    /** Per-item refund: the cheapest Replicator recipe producing {@code item}, divided by its output count. */
    public static DustCost refundFor(RecipeManager recipes, Item item, float fraction) {
        Optional<ReplicatorRecipe> recipe = recipes.getAllRecipesFor(ModRecipeTypes.REPLICATOR.get()).stream()
                .map(holder -> holder.value())
                .filter(r -> r.result().is(item))
                .min((a, b) -> Float.compare(totalPerItem(a), totalPerItem(b)));
        return recipe.map(r -> r.cost().scale(fraction / r.result().getCount())).orElse(DustCost.NONE);
    }

    private static float totalPerItem(ReplicatorRecipe recipe) {
        return recipe.cost().total() / recipe.result().getCount();
    }
}
