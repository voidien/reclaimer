package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.milestone.MilestoneContext;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.replicator.ReplicatorJob;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Everything obtainable: every section and module item can be made in survival (a Replicator recipe whose milestone can
 * be reached, at a price the biggest body can store) and installed into a body. Parts are found by their data
 * components, so a new part is covered without touching this class.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ObtainabilityGameTests {
    private ObtainabilityGameTests() {}

    /** Sections and modules: every item of ours that carries a section or module component. */
    private static List<Item> parts() {
        return BuiltInRegistries.ITEM.entrySet().stream()
                .filter(e -> e.getKey().location().getNamespace().equals(VoidReclaimer.MOD_ID))
                .map(Map.Entry::getValue)
                .filter(item -> Parts.section(item.getDefaultInstance()) != null || Parts.module(item.getDefaultInstance()) != null)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).getPath()))
                .toList();
    }

    /** A player state that satisfies every built-in criterion kind, to ask whether a milestone can be reached at all. */
    private static MilestoneContext everythingDone() {
        Set<ResourceLocation> items = BuiltInRegistries.ITEM.keySet();
        return new MilestoneContext(DustData.of(1000, 1000, 1000, 1000), 1000, 100f, 4, items);
    }

    /** The most dust of any kind the biggest body can store: every chassis with Mk II storage pods on all storage mounts. */
    private static float biggestCapacity(ServerPlayer player) {
        return biggestCapacity(player, Integer.MAX_VALUE);
    }

    /** The same among the bodies of a size class below {@code rank}. */
    private static float biggestCapacity(ServerPlayer player, int rank) {
        float best = 0;
        for (ResourceKey<Chassis> key : chassisKeys()) {
            BodyData body = Bodies.preset(key);
            Bodies.store(player, body);
            if (Bodies.stats(player).sizeRank() >= rank) {
                continue;
            }
            for (BodyData.Mount mount : List.copyOf(body.modules().keySet())) {
                if (mount.kind() == ModuleKind.STORAGE) {
                    body = body.withModule(mount, new ItemStack(ModItems.STORAGE_MK2.get()));
                }
            }
            Bodies.store(player, body);
            best = Math.max(best, DustData.capacity(player));
        }
        return best;
    }

    /** A body built from its basic parts flies at full speed: going over budget is what better parts cost, not the starting point. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBasicBodyFitsItsMassBudget(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 0.5, 80, 0.5);
        List<String> over = new ArrayList<>();
        for (ResourceKey<Chassis> key : chassisKeys()) {
            Bodies.store(player, Bodies.preset(key));
            if (Bodies.stats(player).overloaded()) {
                over.add(key.location().getPath() + " " + Bodies.stats(player).mass() + " of " + Bodies.stats(player).massBudget());
            }
        }
        TestPlayers.remove(player);
        helper.assertTrue(over.isEmpty(), "over budget: " + String.join(", ", over));
        helper.succeed();
    }

    /**
     * Dust is paid in full when a job starts, so a body can only buy what it can hold. Each size class's core has to be affordable in
     * a smaller body, or nobody can get there, and so does a kit with the core in it. The smallest class is where a player starts, so
     * it needs no such thing.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyCoreCanBeBoughtInASmallerBody(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 0.5, 80, 0.5);
        List<String> faults = new ArrayList<>();
        for (ResourceKey<Chassis> key : chassisKeys()) {
            BodyData body = Bodies.preset(key);
            Bodies.store(player, body);
            int rank = Bodies.stats(player).sizeRank();
            float capacity = biggestCapacity(player, rank);
            Item core = body.core().getItem();
            if (capacity <= 0f) {
                continue;
            }
            for (RecipeHolder<ReplicatorRecipe> recipe : player.server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.REPLICATOR.get())) {
                float price = recipe.value().cost().amounts().values().stream().max(Float::compare).orElse(0f);
                if (recipe.value().count(core) > 0 && price > capacity) {
                    faults.add(recipe.id().getPath() + " costs " + price + " of one dust, and a smaller body holds " + capacity);
                }
            }
        }
        TestPlayers.remove(player);
        helper.assertTrue(faults.isEmpty(), String.join("; ", faults));
        helper.succeed();
    }

    /** The recipe a player would use for {@code item}: the one with the fewest other parts, then the cheapest. */
    private static Optional<RecipeHolder<ReplicatorRecipe>> recipeFor(ServerPlayer player, Item item) {
        return player.server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.REPLICATOR.get()).stream()
                .filter(holder -> holder.value().count(item) > 0)
                .min(Comparator.<RecipeHolder<ReplicatorRecipe>>comparingInt(h -> h.value().totalCount())
                        .thenComparingDouble(h -> h.value().cost().total()));
    }

    /** Makes the recipe the way a player does: unlock its milestone, pay the dust, wait for the job, take the item. */
    private static String craft(ServerPlayer player, RecipeHolder<ReplicatorRecipe> holder) {
        ReplicatorRecipe recipe = holder.value();
        player.getInventory().clearContent();
        Replicator.store(player, ReplicatorData.EMPTY.withUnlocked(recipe.milestone()));
        DustData dust = DustData.of(0, 0, 0, 0);
        for (Map.Entry<ResourceLocation, Float> cost : recipe.cost().amounts().entrySet()) {
            dust = dust.with(cost.getKey(), cost.getValue());
        }
        player.setData(ModAttachments.DUST, dust);
        ReplicatorLogic.QueueResult queued = Replicator.queue(player, holder.id());
        if (queued != ReplicatorLogic.QueueResult.QUEUED) {
            return "queue refused: " + queued;
        }
        ReplicatorData data = Replicator.data(player);
        ReplicatorJob job = data.jobs().get(0);
        Replicator.store(player, data.withJobs(List.of(new ReplicatorJob(job.recipe(), job.results(), job.paid(), job.items(), job.total() - 1, job.total()))));
        player.doTick();
        player.doTick();
        Replicator.collect(player);
        return null;
    }

    private static int slotOf(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (player.getInventory().items.get(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    /** The built-in chassis in a fixed order: the map's own order differs between runs. */
    private static List<ResourceKey<Chassis>> chassisKeys() {
        return BuiltinBodies.CHASSIS.keySet().stream().sorted(Comparator.comparing(key -> key.location().getPath())).toList();
    }

    /** Installs the crafted section into some chassis that has a socket for it. Returns an error text, or null. */
    private static String installSection(ServerPlayer player, Item item) {
        List<String> tried = new ArrayList<>();
        for (ResourceKey<Chassis> key : chassisKeys()) {
            BodyData body = Bodies.preset(key);
            Chassis chassis = BuiltinBodies.CHASSIS.get(key);
            // The best core, so the tier gate (tested elsewhere) doesn't stand in the way of finding a socket.
            for (BodyData.Mount mount : List.copyOf(body.modules().keySet())) {
                if (mount.kind() == ModuleKind.CORE) {
                    body = body.withModule(mount, new ItemStack(ModItems.CORE_MK4.get()));
                }
            }
            Bodies.store(player, body);
            BodyView view = Bodies.view(player);
            for (Socket socket : chassis.sockets()) {
                int slot = slotOf(player, item);
                if (slot < 0) {
                    return "the crafted item is gone from the inventory";
                }
                BodyRules.Check check = BodyRules.canInstallSection(view, socket.id(), player.getInventory().items.get(slot));
                if (check != BodyRules.Check.OK) {
                    tried.add(key.location().getPath() + "/" + socket.id() + "=" + check);
                    continue;
                }
                BodyRules.Check result = Bodies.installSection(player, socket.id(), slot);
                if (result != BodyRules.Check.OK) {
                    // E.g. a layout switch that hands back more parts than the inventory has room for: try the next place.
                    tried.add(key.location().getPath() + "/" + socket.id() + " install=" + result);
                    continue;
                }
                // A core section switches the layout, so it sits in the new chassis' core socket, not the one it went into.
                boolean core = Parts.section(item.getDefaultInstance()).isCore();
                ItemStack installed = core ? Bodies.data(player).core() : Bodies.data(player).section(socket.id());
                return installed.is(item) ? null : "install said OK but the section is not on the body";
            }
        }
        return "fits no socket of any chassis (" + String.join(", ", tried) + ")";
    }

    /** Installs the crafted module on some mount of some chassis. Returns an error text, or null. */
    private static String installModule(ServerPlayer player, Item item) {
        List<String> tried = new ArrayList<>();
        for (ResourceKey<Chassis> key : chassisKeys()) {
            Bodies.store(player, Bodies.preset(key));
            BodyView view = Bodies.view(player);
            for (BodyData.Mount mount : view.mounts()) {
                int slot = slotOf(player, item);
                if (slot < 0) {
                    return "the crafted item is gone from the inventory";
                }
                BodyRules.Check check = BodyRules.canInstallModule(view, mount, player.getInventory().items.get(slot));
                if (check != BodyRules.Check.OK) {
                    tried.add(key.location().getPath() + "/" + mount.socket() + ":" + mount.kind() + mount.index() + "=" + check);
                    continue;
                }
                BodyRules.Check result = Bodies.installModule(player, mount, slot);
                if (result != BodyRules.Check.OK) {
                    tried.add(key.location().getPath() + "/" + mount.socket() + " install=" + result);
                    continue;
                }
                return Bodies.data(player).module(mount).is(item) ? null : "install said OK but the module is not on the body";
            }
        }
        return "fits no mount of any chassis (" + tried.size() + " tried, e.g. " + tried.stream().limit(3).collect(Collectors.joining(", ")) + ")";
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everySectionAndModuleCanBeMadeAndInstalled(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 94_000.5, 80, 0.5);
        try {
            List<Item> parts = parts();
            helper.assertTrue(parts.size() > 60, "found only " + parts.size() + " parts; the component scan is broken");
            float capacity = biggestCapacity(player);
            MilestoneContext done = everythingDone();
            List<String> problems = new ArrayList<>();
            for (Item item : parts) {
                String name = BuiltInRegistries.ITEM.getKey(item).getPath();
                Optional<RecipeHolder<ReplicatorRecipe>> recipe = recipeFor(player, item);
                if (recipe.isEmpty()) {
                    problems.add(name + ": no Replicator recipe makes it");
                    continue;
                }
                ReplicatorRecipe replicator = recipe.get().value();
                Milestone milestone = Milestones.get(player.registryAccess(), replicator.milestone());
                if (milestone == null) {
                    problems.add(name + ": milestone " + replicator.milestone().location() + " does not exist");
                    continue;
                }
                if (!milestone.isReached(done)) {
                    problems.add(name + ": milestone " + replicator.milestone().location() + " can never be reached");
                    continue;
                }
                for (Map.Entry<ResourceLocation, Float> cost : replicator.cost().amounts().entrySet()) {
                    if (cost.getValue() > capacity) {
                        problems.add(name + ": costs " + cost.getValue() + " " + cost.getKey().getPath() + " but the biggest body stores " + capacity);
                    }
                }
                String error = craft(player, recipe.get());
                if (error == null && slotOf(player, item) < 0) {
                    error = "the finished job did not deliver it";
                }
                if (error == null) {
                    error = Parts.section(item.getDefaultInstance()) != null ? installSection(player, item) : installModule(player, item);
                }
                if (error != null) {
                    problems.add(name + ": " + error);
                }
            }
            helper.assertTrue(problems.isEmpty(), problems.size() + " of " + parts.size() + " parts are not obtainable:\n" + String.join("\n", problems));
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
