package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Trail;
import com.voidreclaimer.api.event.BodyChangedEvent;
import com.voidreclaimer.api.event.BodyStatsEvent;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.OrderedRegistry;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of the modular body: the starter body, installing and removing sections and modules (items move
 * between the body and the inventory), and applying the body's stats (scale, max health, reach, energy capacity).
 * Flight, dust and energy read {@link #stats} directly.
 *
 * <p>Reading a body ({@link #data}, {@link #stats}, {@link #view}, {@link #apply}) works for any living entity that
 * has the body attachment, so helper drones (Phase 8) can share it; editing is through a player's inventory.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Bodies {
    private static final ResourceLocation SCALE = VoidReclaimer.id("body_scale");
    private static final ResourceLocation HEALTH = VoidReclaimer.id("body_health");
    private static final ResourceLocation BLOCK_REACH = VoidReclaimer.id("body_block_reach");
    private static final ResourceLocation ENTITY_REACH = VoidReclaimer.id("body_entity_reach");
    private static final float VANILLA_MAX_HEALTH = 20f;

    private Bodies() {}

    /** What every AI starts as: a complete Drone with a Mk I core and one Mk I thruster. */
    public static BodyData starter() {
        Map<String, ItemStack> sections = BodyMigration.basicSections(BuiltinBodies.DRONE);
        return new BodyData("hull", sections, Map.of(
                new BodyData.Mount("hull", ModuleKind.CORE, 0), new ItemStack(ModItems.CORE_MK1.get()),
                new BodyData.Mount("rotor_front_left", ModuleKind.THRUSTER, 0), new ItemStack(ModItems.THRUSTER_MK1.get())),
                0, Cosmetics.DEFAULT, List.of());
    }

    public static BodyData data(LivingEntity entity) {
        return entity.getData(ModAttachments.BODY);
    }

    /** Valid on both sides (the owner's client has the synced body). Cached until the body changes. */
    public static BodyStats stats(LivingEntity entity) {
        BodyData body = data(entity);
        BodyStats cached = body.cachedStats();
        if (cached != null) {
            return cached;
        }
        BodyStats computed = compute(body, entity.registryAccess());
        computed = NeoForge.EVENT_BUS.post(new BodyStatsEvent(entity, body, computed)).getStats();
        body.cacheStats(computed);
        return computed;
    }

    /** The body with its layout and the lookups the rules need. */
    public static BodyView view(LivingEntity entity) {
        RegistryAccess access = entity.registryAccess();
        BodyData body = data(entity);
        return new BodyView(body, chassis(access, body), stats(entity), sizes(access), layouts(access));
    }

    /** The active tool module, or empty. */
    public static ItemStack activeTool(LivingEntity entity) {
        return view(entity).activeTool();
    }

    static BodyStats compute(BodyData body, RegistryAccess access) {
        Chassis chassis = chassis(access, body);
        SizeClass size = sizes(access).apply(chassis.sizeClass());
        if (size == null) {
            size = BuiltinBodies.SIZE_CLASSES.get(BuiltinBodies.DRONE_SIZE);
        }
        List<BodyStats.Installed<SectionStats>> sections = new ArrayList<>();
        for (ItemStack stack : body.sections().values()) {
            SectionStats section = Parts.section(stack);
            if (section != null) {
                sections.add(new BodyStats.Installed<>(section, Wear.efficiency(Wear.condition(stack))));
            }
        }
        List<BodyStats.Installed<ModuleStats>> modules = new ArrayList<>();
        for (Map.Entry<BodyData.Mount, ItemStack> entry : body.modules().entrySet()) {
            ModuleStats module = Parts.module(entry.getValue());
            if (module != null) {
                ItemStack host = body.sections().getOrDefault(entry.getKey().socket(), ItemStack.EMPTY);
                modules.add(new BodyStats.Installed<>(module, Wear.efficiency(Wear.condition(host))));
            }
        }
        return BodyStats.computeWorn(chassis.sizeClass(), size, sections, modules);
    }

    /** The layout named by the core section; the built-in Drone if it is missing or unknown. */
    public static Chassis chassis(RegistryAccess access, BodyData body) {
        SectionStats core = Parts.section(body.core());
        if (core != null && core.chassis().isPresent()) {
            Chassis chassis = layouts(access).apply(core.chassis().get());
            if (chassis != null) {
                return chassis;
            }
        }
        return BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE);
    }

    static Function<ResourceKey<SizeClass>, SizeClass> sizes(RegistryAccess access) {
        return key -> OrderedRegistry.get(access, VoidReclaimerRegistries.SIZE_CLASS, key);
    }

    static Function<ResourceKey<Chassis>, Chassis> layouts(RegistryAccess access) {
        return key -> OrderedRegistry.get(access, VoidReclaimerRegistries.CHASSIS, key);
    }

    public static void store(ServerPlayer player, BodyData body) {
        BodyData previous = data(player);
        player.setData(ModAttachments.BODY, body);
        player.syncData(ModAttachments.BODY);
        apply(player);
        if (!previous.equals(body)) {
            NeoForge.EVENT_BUS.post(new BodyChangedEvent(player, previous, body));
        }
    }

    /** Pushes the body's stats into attributes and, for players, energy capacity. */
    public static void apply(LivingEntity entity) {
        BodyStats stats = stats(entity);
        modifier(entity, Attributes.SCALE, SCALE, stats.scale() - 1.0);
        // Permanent, so it is in the saved attributes: vanilla clamps the saved health to the max health it finds at load,
        // before PlayerLoggedInEvent, and a transient modifier would cut a bigger body's health back to 20 on every relog.
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(
                    new AttributeModifier(HEALTH, stats.maxHealth() - VANILLA_MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE));
        }
        modifier(entity, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH, stats.reach());
        modifier(entity, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH, stats.reach());
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
        if (entity instanceof ServerPlayer player) {
            EnergyData energy = player.getData(ModAttachments.ENERGY);
            if (energy.capacity() != stats.energyCapacity()) {
                Energy.update(player, new EnergyData(Math.min(energy.energy(), stats.energyCapacity()), stats.energyCapacity(),
                        energy.lowPower()), true);
            }
        }
    }

    private static void modifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ---- sections -----------------------------------------------------------------------------------------

    /** Installs the section from inventory slot {@code inventorySlot} into {@code socket}. */
    public static BodyRules.Check installSection(ServerPlayer player, String socket, int inventorySlot) {
        ItemStack source = inventoryStack(player, inventorySlot);
        if (source == null) {
            return reply(player, BodyRules.Check.EMPTY);
        }
        BodyView view = view(player);
        BodyRules.Check check = BodyRules.canInstallSection(view, socket, source);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        BodyRules.Change change = BodyRules.installSection(view, socket, source.copyWithCount(1));
        return commit(player, change, source, inventorySlot);
    }

    /** Takes a section (and the modules on it) off the body into the inventory. */
    public static BodyRules.Check removeSection(ServerPlayer player, String socket) {
        BodyView view = view(player);
        BodyRules.Check check = BodyRules.canRemoveSection(view, socket);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        return commit(player, BodyRules.removeSection(view, socket), null, -1);
    }

    // ---- modules ------------------------------------------------------------------------------------------

    /** Mounts the module from inventory slot {@code inventorySlot} on {@code mount}. */
    public static BodyRules.Check installModule(ServerPlayer player, BodyData.Mount mount, int inventorySlot) {
        ItemStack source = inventoryStack(player, inventorySlot);
        if (source == null) {
            return reply(player, BodyRules.Check.EMPTY);
        }
        BodyView view = view(player);
        BodyRules.Check check = BodyRules.canInstallModule(view, mount, source);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        BodyData body = view.data();
        ItemStack old = body.module(mount);
        List<ItemStack> returned = old.isEmpty() ? List.of() : List.of(old);
        return commit(player, new BodyRules.Change(body.withModule(mount, source.copyWithCount(1)), returned), source, inventorySlot);
    }

    /** Takes a module off the body into the inventory. */
    public static BodyRules.Check removeModule(ServerPlayer player, BodyData.Mount mount) {
        BodyView view = view(player);
        BodyRules.Check check = BodyRules.canRemoveModule(view, mount);
        if (check != BodyRules.Check.OK) {
            return reply(player, check);
        }
        BodyData body = view.data();
        return commit(player, new BodyRules.Change(body.withModule(mount, ItemStack.EMPTY), List.of(body.module(mount))), null, -1);
    }

    /**
     * Applies a change: takes one item from {@code source} (if any), stores the body and hands back what left it.
     * Refused when the inventory can't take the returned items.
     */
    private static BodyRules.Check commit(ServerPlayer player, BodyRules.Change change, ItemStack source, int sourceSlot) {
        // Room is counted under the new body's slot limit: removing a storage module locks its own rows again.
        int newSlots = compute(change.body(), player.registryAccess()).inventorySlots();
        int room = freeSlots(player, newSlots)
                + (source != null && source.getCount() == 1 && !InventoryLock.isLocked(player, sourceSlot, newSlots) ? 1 : 0);
        if (room < change.returned().size()) {
            reply(player, "message.void_reclaimer.body.inventory_full");
            return BodyRules.Check.EMPTY;
        }
        if (source != null) {
            source.shrink(1);
        }
        store(player, change.body());
        for (ItemStack returned : change.returned()) {
            giveBack(player, sourceSlot, returned);
        }
        return BodyRules.Check.OK;
    }

    /** Repairs the section in {@code socket} to full condition, paying {@link Wear#repairCost} from the dust reserve. */
    public static boolean repair(ServerPlayer player, String socket) {
        BodyData body = data(player);
        ItemStack section = body.section(socket);
        if (section.isEmpty() || Wear.condition(section) >= 1f) {
            reply(player, "message.void_reclaimer.body.not_worn");
            return false;
        }
        DustCost cost = Wear.repairCost(player.serverLevel().getRecipeManager(), section);
        DustData dust = player.getData(ModAttachments.DUST);
        if (!cost.canAfford(dust)) {
            reply(player, "message.void_reclaimer.body.repair_dust");
            return false;
        }
        player.setData(ModAttachments.DUST, cost.takeFrom(dust));
        player.syncData(ModAttachments.DUST);
        store(player, body.withSection(socket, Wear.withCondition(section, 1f)));
        return true;
    }

    // ---- looks and tools ----------------------------------------------------------------------------------

    /** Applies a look if its casing and trail are registered and unlocked; otherwise keeps the old one. */
    public static void setCosmetics(ServerPlayer player, Cosmetics cosmetics) {
        RegistryAccess access = player.registryAccess();
        ReplicatorData replicator = Replicator.data(player);
        Casing casing = OrderedRegistry.get(access, VoidReclaimerRegistries.CASING, cosmetics.casing());
        Trail trail = OrderedRegistry.get(access, VoidReclaimerRegistries.TRAIL, cosmetics.trail());
        if (casing == null || trail == null || !Cosmetics.isAvailable(casing, replicator::isUnlocked)
                || !Cosmetics.isAvailable(trail, replicator::isUnlocked)) {
            return;
        }
        for (Cosmetics.SectionPaint paint : cosmetics.sections().values()) {
            if (paint.casing().isPresent()) {
                Casing override = OrderedRegistry.get(access, VoidReclaimerRegistries.CASING, paint.casing().get());
                if (override == null || !Cosmetics.isAvailable(override, replicator::isUnlocked)) {
                    return;
                }
            }
        }
        store(player, data(player).withCosmetics(cosmetics));
    }

    public static void selectTool(ServerPlayer player, int index) {
        if (index >= 0 && index < view(player).toolMounts().size()) {
            store(player, data(player).withActiveTool(index));
        }
    }

    // ---- inventory helpers --------------------------------------------------------------------------------

    private static ItemStack inventoryStack(ServerPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.items.size() || inventory.items.get(slot).isEmpty()) {
            return null;
        }
        return inventory.items.get(slot);
    }

    /** Puts {@code stack} into {@code preferredSlot} if it is empty, otherwise anywhere; drops it if all else fails. */
    private static void giveBack(ServerPlayer player, int preferredSlot, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (preferredSlot >= 0 && preferredSlot < inventory.items.size() && inventory.items.get(preferredSlot).isEmpty()
                && !InventoryLock.isLocked(player, preferredSlot)) {
            inventory.items.set(preferredSlot, stack);
        } else if (!inventory.add(stack)) {
            player.drop(stack, false);
        }
    }

    private static int freeSlots(ServerPlayer player, int slots) {
        Inventory inventory = player.getInventory();
        int free = 0;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty() && !InventoryLock.isLocked(player, i, slots)) {
                free++;
            }
        }
        return free;
    }

    private static BodyRules.Check reply(ServerPlayer player, BodyRules.Check check) {
        String key = BodyRules.messageKey(check);
        if (key != null) {
            reply(player, key);
        }
        return check;
    }

    private static void reply(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- lifecycle ----------------------------------------------------------------------------------------

    /** Fits the body to its layout (datapacks may have changed it) and applies it. */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BodyData body = data(player);
            BodyRules.Change normalized = BodyRules.normalize(body, chassis(player.registryAccess(), body));
            if (!normalized.returned().isEmpty()) {
                store(player, normalized.body().plusPending(normalized.returned()));
            }
            apply(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            apply(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Hands out items that left the body without an inventory at hand (save migration, layout changes). */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !data(player).pending().isEmpty()) {
            BodyData body = data(player);
            store(player, body.withPending(List.of()));
            for (ItemStack stack : body.pending()) {
                giveBack(player, -1, stack.copy());
            }
            player.displayClientMessage(Component.translatable("message.void_reclaimer.body.returned").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
