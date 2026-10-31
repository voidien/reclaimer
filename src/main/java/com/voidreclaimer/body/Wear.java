package com.voidreclaimer.body;

import com.voidreclaimer.Config;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.salvage.Salvage;
import net.minecraft.world.item.crafting.RecipeManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Light, repairable wear on body sections. Thrust wears the sections carrying thrusters; using a tool module wears the
 * section holding it. Below {@link #FULL_EFFECT} condition a section (and the thrusters on it) lose effectiveness
 * smoothly, at most {@link #MAX_LOSS}. Sections never break, creative bodies don't wear, and wear can be switched off in
 * the config. Repair is in the Body tab ({@link Bodies#repair}).
 *
 * <p>Wear is gathered per player and written to the section item in 1% steps, so the body isn't re-synced every tick.
 */
public final class Wear {
    /** Condition below which effectiveness starts to drop. */
    public static final float FULL_EFFECT = 0.5f;
    /** Effectiveness lost at condition 0. */
    public static final float MAX_LOSS = 0.25f;
    static final float STEP = 0.01f;

    /** Wear gathered since the last 1% step, per player and socket (server side; lost on restart, which is harmless). */
    private static final Map<UUID, Map<String, Float>> GATHERED = new java.util.concurrent.ConcurrentHashMap<>();

    private Wear() {}

    /** Condition of a section item: 1 when new. */
    public static float condition(ItemStack section) {
        Float value = section.get(ModDataComponents.CONDITION.get());
        return value == null ? 1f : value;
    }

    /** Effectiveness multiplier for a condition: 1 down to {@link #FULL_EFFECT}, then falling to {@code 1 - MAX_LOSS} at 0. */
    public static float efficiency(float condition) {
        if (condition >= FULL_EFFECT) {
            return 1f;
        }
        return 1f - MAX_LOSS * (FULL_EFFECT - Math.max(0f, condition)) / FULL_EFFECT;
    }

    /** A copy of {@code section} at {@code condition} (1 removes the component). */
    /** A colour for how worn a section is: white when new, through amber at half, to red when spent. */
    public static int tint(float condition) {
        float c = Math.max(0f, Math.min(1f, condition));
        float green = c >= FULL_EFFECT ? 0.72f + (1f - 0.72f) * (c - FULL_EFFECT) / (1f - FULL_EFFECT) : 0.72f * c / FULL_EFFECT;
        float blue = c >= FULL_EFFECT ? 0.25f + 0.75f * (c - FULL_EFFECT) / (1f - FULL_EFFECT) : 0.25f * c / FULL_EFFECT;
        return 0xFF000000 | 0xFF0000 | Math.round(green * 255f) << 8 | Math.round(blue * 255f);
    }

    public static ItemStack withCondition(ItemStack section, float condition) {
        ItemStack copy = section.copy();
        if (condition >= 1f) {
            copy.remove(ModDataComponents.CONDITION.get());
        } else {
            copy.set(ModDataComponents.CONDITION.get(), Math.max(0f, condition));
        }
        return copy;
    }

    /**
     * Dust to bring {@code section} back to full condition: {@link Config#REPAIR_COST_FRACTION} of the cheapest
     * Replicator recipe for it at condition 0, less when less worn. Free when it isn't worn or has no recipe.
     */
    public static DustCost repairCost(RecipeManager recipes, ItemStack section) {
        float worn = 1f - condition(section);
        if (worn <= 0f) {
            return DustCost.NONE;
        }
        return Salvage.refundFor(recipes, section.getItem(), Config.REPAIR_COST_FRACTION.get().floatValue() * worn);
    }

    /** One tick of thrust: wears every section carrying a thruster. */
    public static void onThrustTick(ServerPlayer player) {
        float amount = (float) (Config.WEAR_PER_THRUST_MINUTE.get() / 1200.0);
        Set<String> sockets = new HashSet<>();
        Bodies.data(player).modules().forEach((mount, module) -> {
            if (mount.kind() == ModuleKind.THRUSTER) {
                sockets.add(mount.socket());
            }
        });
        for (String socket : sockets) {
            add(player, socket, amount);
        }
    }

    /** A tool module was used: wears the section holding the active tool. */
    public static void onToolUse(ServerPlayer player) {
        BodyData.Mount mount = Bodies.view(player).activeToolMount();
        if (mount != null) {
            add(player, mount.socket(), Config.WEAR_PER_TOOL_USE.get().floatValue());
        }
    }

    /** Gathers wear on {@code socket}'s section, scaled by its wear resistance, and applies it in 1% steps. */
    public static void add(ServerPlayer player, String socket, float amount) {
        if (!Config.WEAR_ENABLED.get() || player.isCreative() || player.isSpectator() || amount <= 0f) {
            return;
        }
        BodyData body = Bodies.data(player);
        ItemStack section = body.section(socket);
        SectionStats stats = Parts.section(section);
        if (stats == null) {
            return;
        }
        Map<String, Float> gathered = GATHERED.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        float total = gathered.getOrDefault(socket, 0f) + amount * (1f - stats.wearResistance());
        if (total < STEP) {
            gathered.put(socket, total);
            return;
        }
        gathered.remove(socket);
        float condition = condition(section);
        if (condition > 0f) {
            Bodies.store(player, body.withSection(socket, withCondition(section, condition - total)));
        }
    }
}
