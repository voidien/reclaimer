package com.voidreclaimer.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloGlobes;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.registry.ModRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

/** {@code /void_reclaimer holo <pos> line|scale|source|billboard|model|body|globe|clear}: writes to a Holo Projector without a computer, for checks. */
final class HoloCommand {
    private static final SimpleCommandExceptionType NOT_A_PROJECTOR =
            new SimpleCommandExceptionType(Component.translatable("commands.void_reclaimer.holo.not_a_projector"));

    private HoloCommand() {}

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("holo").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.literal("line").then(Commands.argument("line", IntegerArgumentType.integer(1, HoloDisplay.MAX_LINES))
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                            projector(ctx).setLine(IntegerArgumentType.getInteger(ctx, "line") - 1, StringArgumentType.getString(ctx, "text"), HoloDisplay.NO_COLOR);
                            return Command.SINGLE_SUCCESS;
                        }))))
                .then(Commands.literal("scale").then(Commands.argument("scale", FloatArgumentType.floatArg(HoloDisplay.MIN_SCALE, HoloDisplay.MAX_SCALE)).executes(ctx -> {
                    projector(ctx).setScale(FloatArgumentType.getFloat(ctx, "scale"));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("source").then(Commands.argument("source", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ModRegistries.HOLO_READOUTS.keySet(), builder))
                        .executes(ctx -> {
                            projector(ctx).setSource(ResourceLocationArgument.getId(ctx, "source"));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("billboard").then(Commands.argument("on", BoolArgumentType.bool()).executes(ctx -> {
                    projector(ctx).setBillboard(BoolArgumentType.getBool(ctx, "on"));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("model").then(Commands.argument("id", ResourceLocationArgument.id()).executes(ctx -> {
                    projector(ctx).setModel(ResourceLocationArgument.getId(ctx, "id"));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("body").executes(ctx -> {
                    projector(ctx).setBody();
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("globe").then(Commands.argument("globe", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(HoloGlobes.NAMES, builder))
                        .executes(ctx -> projector(ctx).setGlobe(StringArgumentType.getString(ctx, "globe")) ? Command.SINGLE_SUCCESS : 0)))
                .then(Commands.literal("clear").executes(ctx -> {
                    projector(ctx).clearLines();
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static HoloProjectorBlockEntity projector(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ctx.getSource().getLevel().getBlockEntity(BlockPosArgument.getBlockPos(ctx, "pos")) instanceof HoloProjectorBlockEntity projector) {
            return projector;
        }
        throw NOT_A_PROJECTOR.create();
    }
}
