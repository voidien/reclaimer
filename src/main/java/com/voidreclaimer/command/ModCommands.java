package com.voidreclaimer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustKind;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug commands (op level 2):
 * <ul>
 *   <li>{@code /void_reclaimer energy get|set <player> [amount]}</li>
 *   <li>{@code /void_reclaimer dust get <player>}, {@code dust set <player> <kind> <amount>}</li>
 *   <li>{@code /void_reclaimer dust locate [range]}: nearest cloud from the command position</li>
 * </ul>
 */
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
                                                        FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("dust")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> getDust(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("kind", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(DustKind.values()).map(DustKind::getSerializedName), builder))
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                                                        .executes(ctx -> setDust(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "kind"),
                                                                FloatArgumentType.getFloat(ctx, "amount")))))))
                        .then(Commands.literal("locate")
                                .executes(ctx -> locate(ctx.getSource(), DEFAULT_LOCATE_RANGE))
                                .then(Commands.argument("range", IntegerArgumentType.integer(1, 100_000))
                                        .executes(ctx -> locate(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "range")))))));
    }

    private static final int DEFAULT_LOCATE_RANGE = 4096;

    private static int getDust(CommandSourceStack source, ServerPlayer player) {
        DustData dust = player.getData(ModAttachments.DUST);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.get", player.getDisplayName(),
                format(dust.metallic()), format(dust.silicate()), format(dust.ice()), format(dust.carbon())), false);
        return Mth.floor(dust.metallic() + dust.silicate() + dust.ice() + dust.carbon());
    }

    private static int setDust(CommandSourceStack source, ServerPlayer player, String kindName, float amount) {
        DustKind kind = DustKind.byName(kindName);
        if (kind == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.dust.unknown_kind", kindName));
            return 0;
        }
        float clamped = Math.min(amount, DustData.capacity(player));
        player.setData(ModAttachments.DUST, player.getData(ModAttachments.DUST).with(kind, clamped));
        player.syncData(ModAttachments.DUST);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.set", player.getDisplayName(),
                kind.displayName(), format(clamped)), true);
        return Mth.floor(clamped);
    }

    private static int locate(CommandSourceStack source, int range) {
        DustField field = DustField.of(source.getLevel());
        if (field == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.dust.no_field"));
            return 0;
        }
        Vec3 pos = source.getPosition();
        Optional<DustCloud> cloud = field.nearest(pos, range, false);
        if (cloud.isEmpty()) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.dust.locate.none", range));
            return 0;
        }
        DustCloud c = cloud.get();
        Vec3 center = c.center();
        String centerText = String.format(Locale.ROOT, "%d %d %d", Mth.floor(center.x), Mth.floor(center.y), Mth.floor(center.z));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.locate", c.kind().displayName(),
                centerText, Mth.floor(c.radius()), Mth.floor(Math.max(0, c.surfaceDistance(pos)))), false);
        return Mth.floor(Math.max(0, c.surfaceDistance(pos)));
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
        Energy.update(player, new EnergyData(energy, old.capacity(), energy <= 0f || old.lowPower()), true);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.energy.set",
                player.getDisplayName(), format(energy)), true);
        return Mth.floor(energy);
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
