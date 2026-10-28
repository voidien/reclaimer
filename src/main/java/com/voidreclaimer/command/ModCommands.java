package com.voidreclaimer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /void_reclaimer energy get|set <player> [amount]}: inspect and set energy (op level 2). */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(VoidReclaimer.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("energy")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> getEnergy(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> setEnergy(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        FloatArgumentType.getFloat(ctx, "amount"))))))));
    }

    private static int getEnergy(CommandSourceStack source, ServerPlayer player) {
        EnergyData data = player.getData(ModAttachments.ENERGY);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.energy.get",
                player.getDisplayName(), format(data.energy()), format(data.capacity()),
                Component.translatable(data.lowPower() ? "commands.void_reclaimer.energy.low_power" : "commands.void_reclaimer.energy.online")), false);
        return Mth.floor(data.energy());
    }

    private static int setEnergy(CommandSourceStack source, ServerPlayer player, float amount) {
        EnergyData old = player.getData(ModAttachments.ENERGY);
        float energy = Math.min(amount, old.capacity());
        player.setData(ModAttachments.ENERGY, new EnergyData(energy, old.capacity(), energy <= 0f || old.lowPower()));
        player.syncData(ModAttachments.ENERGY);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.energy.set",
                player.getDisplayName(), format(energy)), true);
        return Mth.floor(energy);
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
