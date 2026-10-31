package com.voidreclaimer.replicator;

import com.voidreclaimer.api.event.MilestoneReachedEvent;
import com.voidreclaimer.api.event.ReplicatorJobFinishedEvent;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.milestone.MilestoneContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForge;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of the AI's built-in Replicator. Dust is paid when a job is queued; the head of the queue is worked on
 * every tick while the AI has power, drawing energy per second of work; finished items go straight to the inventory,
 * or into the output buffer if it is full. Milestones are checked here too.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Replicator {
    private static final int MILESTONE_CHECK_TICKS = 20;
    /**
     * No checks right after login or respawn: the client must have its initial copy of the data first, or a
     * milestone reached while offline (e.g. one added by an update) would arrive in that copy and never be toasted.
     */
    private static final int MILESTONE_GRACE_TICKS = 40;

    private Replicator() {}

    @Nullable
    public static ReplicatorRecipe recipe(ServerPlayer player, ResourceLocation id) {
        Optional<RecipeHolder<?>> holder = player.server.getRecipeManager().byKey(id);
        return holder.filter(h -> h.value().getType() == ModRecipeTypes.REPLICATOR.get())
                .map(h -> (ReplicatorRecipe) h.value())
                .orElse(null);
    }

    public static ReplicatorLogic.QueueResult queue(ServerPlayer player, ResourceLocation id) {
        ReplicatorLogic.Queued queued = ReplicatorLogic.queue(
                data(player), player.getData(ModAttachments.DUST), id, recipe(player, id));
        if (queued.result() == ReplicatorLogic.QueueResult.QUEUED) {
            store(player, queued.data());
            player.setData(ModAttachments.DUST, queued.dust());
            player.syncData(ModAttachments.DUST);
        } else {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.replicator."
                    + queued.result().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY), true);
        }
        return queued.result();
    }

    public static boolean cancel(ServerPlayer player, int index) {
        ReplicatorLogic.Queued cancelled = ReplicatorLogic.cancel(
                data(player), player.getData(ModAttachments.DUST), index, DustData.capacity(player));
        if (cancelled.result() != ReplicatorLogic.QueueResult.QUEUED) {
            return false;
        }
        store(player, cancelled.data());
        player.setData(ModAttachments.DUST, cancelled.dust());
        player.syncData(ModAttachments.DUST);
        return true;
    }

    /** Moves queued job {@code from} to {@code to} (see {@link ReplicatorLogic#reorder}). Returns whether the queue changed. */
    public static boolean move(ServerPlayer player, int from, int to) {
        ReplicatorData data = data(player);
        ReplicatorData moved = ReplicatorLogic.reorder(data, from, to);
        if (moved == data) {
            return false;
        }
        store(player, moved);
        return true;
    }

    /** Moves as much of the output buffer as fits into the inventory. */
    public static void collect(ServerPlayer player) {
        ReplicatorData data = data(player);
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : data.buffer()) {
            ItemStack copy = stack.copy();
            player.getInventory().add(copy);
            if (!copy.isEmpty()) {
                remaining.add(copy);
            }
        }
        store(player, data.withBuffer(remaining));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (player.tickCount >= MILESTONE_GRACE_TICKS && player.tickCount % MILESTONE_CHECK_TICKS == 0) {
            checkMilestones(player);
        }
        ReplicatorData data = data(player);
        if (data.jobs().isEmpty()) {
            return;
        }
        ReplicatorJob job = data.current();
        ReplicatorLogic.Tick tick = ReplicatorLogic.tick(data, Energy.canPowerSystems(player));
        if (tick.worked()) {
            Energy.consume(player, (float) (Config.REPLICATOR_ENERGY.get() / 20.0));
        }
        ReplicatorData updated = tick.data();
        if (!tick.finished().isEmpty() && job != null) {
            Component name = tick.finished().get(0).getHoverName();
            ReplicatorJobFinishedEvent finished = NeoForge.EVENT_BUS.post(
                    new ReplicatorJobFinishedEvent(player, job.recipe(), tick.finished()));
            for (ItemStack output : finished.getOutputs()) {
                player.getInventory().add(output); // shrinks output to whatever didn't fit
                updated = ReplicatorLogic.toBuffer(updated, output);
            }
            player.displayClientMessage(Component.translatable("message.void_reclaimer.replicator.done", name)
                    .withStyle(ChatFormatting.AQUA), true);
        }
        if (!updated.equals(data)) {
            // Progress is shown live in the open screen, so sync every change; it's one small packet per tick.
            store(player, updated);
        }
    }

    public static void checkMilestones(ServerPlayer player) {
        ReplicatorData data = trackItems(player, data(player));
        MilestoneContext context = new MilestoneContext(player.getData(ModAttachments.DUST), player.getData(ModAttachments.DUST_COLLECTED), data.blocksPlaced(), DustData.capacity(player),
                DysonSphereData.of(player.serverLevel()).tier(), java.util.Set.copyOf(data.itemsMade()), java.util.Set.copyOf(data.dimensionsVisited()));
        ReplicatorData updated = data;
        List<ResourceKey<Milestone>> reached = new ArrayList<>();
        for (Milestones.Entry entry : Milestones.all(player.registryAccess())) {
            if (!updated.isUnlocked(entry.key()) && entry.milestone().isReached(context)) {
                updated = updated.withUnlocked(entry.key());
                reached.add(entry.key());
            }
        }
        if (updated != data) {
            store(player, updated);
        }
        for (ResourceKey<Milestone> milestone : reached) {
            NeoForge.EVENT_BUS.post(new MilestoneReachedEvent(player, milestone));
        }
    }

    /** Records the items some unreached milestone asks for that the player is holding right now (inventory, armor, offhand). */
    private static ReplicatorData trackItems(ServerPlayer player, ReplicatorData data) {
        java.util.Set<net.minecraft.resources.ResourceLocation> wanted = Milestones.itemsWanted(player.registryAccess(), data::isUnlocked);
        wanted.removeAll(data.itemsMade());
        if (wanted.isEmpty()) {
            return data;
        }
        ReplicatorData updated = data;
        for (ItemStack stack : player.getInventory().items) {
            updated = noteItem(stack, wanted, updated);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            updated = noteItem(stack, wanted, updated);
        }
        for (ItemStack stack : player.getInventory().armor) {
            updated = noteItem(stack, wanted, updated);
        }
        for (ItemStack stack : data.buffer()) {
            updated = noteItem(stack, wanted, updated);
        }
        if (updated != data) {
            store(player, updated);
        }
        return updated;
    }

    private static ReplicatorData noteItem(ItemStack stack, java.util.Set<net.minecraft.resources.ResourceLocation> wanted, ReplicatorData data) {
        if (stack.isEmpty()) {
            return data;
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return wanted.contains(id) ? data.withItemMade(id) : data;
    }

    /**
     * Records the dimension the player stands in, for milestones about setting foot somewhere. Only records: the
     * periodic check on the player tick grants the milestone, after the login grace period like every other one.
     */
    public static void noteDimension(ServerPlayer player) {
        ReplicatorData data = data(player);
        ReplicatorData updated = data.withDimensionVisited(player.level().dimension().location());
        if (updated != data) {
            store(player, updated);
        }
    }

    @SubscribeEvent
    static void onChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            noteDimension(player);
        }
    }

    @SubscribeEvent
    static void onLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            noteDimension(player);
        }
    }

    /** Counts a block the player placed, for block-placing milestones. Stops once every such milestone is satisfied. */
    public static void countBlockPlaced(ServerPlayer player) {
        ReplicatorData data = data(player);
        if (data.blocksPlaced() < Milestones.blocksNeeded(player.registryAccess())) {
            store(player, data.withBlocksPlaced(data.blocksPlaced() + 1));
        }
    }

    @SubscribeEvent
    static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            countBlockPlaced(player);
        }
    }

    public static ReplicatorData data(ServerPlayer player) {
        return player.getData(ModAttachments.REPLICATOR);
    }

    public static void store(ServerPlayer player, ReplicatorData data) {
        player.setData(ModAttachments.REPLICATOR, data);
        player.syncData(ModAttachments.REPLICATOR);
    }
}
