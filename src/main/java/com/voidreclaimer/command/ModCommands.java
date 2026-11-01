package com.voidreclaimer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.core.BackupCores;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.asteroid.AsteroidBody;
import com.voidreclaimer.asteroid.AsteroidChunkGenerator;
import com.voidreclaimer.asteroid.AsteroidField;
import com.voidreclaimer.dust.DustField;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
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
 *   <li>{@code /void_reclaimer milestone grant|revoke <player> <milestone>} (a revoked milestone whose condition
 *       still holds is granted again at the next check)</li>
 *   <li>{@code /void_reclaimer core charge <player>}: fill every Backup Core the player owns</li>
 *   <li>{@code /void_reclaimer asteroid locate [small|large|giant]}: nearest asteroid from the command position (deep space)</li>
 *   <li>{@code /void_reclaimer sky era get|set <ruined|healing|restored>}: the ruined Earth sky era</li>
 *   <li>{@code /void_reclaimer dyson get}, {@code dyson set satellites|trusses <count>}: Dyson Sphere counters and tier</li>
 *   <li>{@code /void_reclaimer body get|reset <player>}: body summary, or back to the starter drone</li>
 *   <li>{@code /void_reclaimer wear <player> <condition>}: sets every section's condition, 1 new and 0 spent</li>
 *   <li>{@code /void_reclaimer link shells <player>}: every shell the player has owned, where it was last seen and how it stands</li>
 *   <li>{@code /void_reclaimer link reach <player> <shell id>}: links to a shell on the player's list, loading its chunks first</li>
 *   <li>{@code /void_reclaimer scene <name>}: builds a ready-made test setup in deep space and moves you to it ({@link com.voidreclaimer.debug.TestScenes})</li>
 *   <li>{@code /void_reclaimer starship spawn}: a placeholder Test Starship 12 blocks ahead of the command source</li>
 * </ul>
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ModCommands {
    private ModCommands() {}

    private static int spawnStarship(CommandSourceStack source) {
        Vec3 eye = source.getEntity() != null ? source.getEntity().getEyePosition() : source.getPosition();
        Vec3 pos = com.voidreclaimer.starship.TestStarshipItem.ahead(eye, Vec3.directionFromRotation(source.getRotation()));
        com.voidreclaimer.starship.TestStarship.spawn(source.getLevel(), pos);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.starship.spawned",
                String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z)), true);
        return 1;
    }

    /** Sets every section on a body to a condition, 1 new and 0 spent, to look at wear without waiting for it. */
    private static int wear(CommandSourceStack source, ServerPlayer player, float condition) {
        com.voidreclaimer.body.BodyData body = com.voidreclaimer.body.Bodies.data(player);
        for (String socket : java.util.List.copyOf(body.sections().keySet())) {
            body = body.withSection(socket, com.voidreclaimer.body.Wear.withCondition(body.section(socket), condition));
        }
        com.voidreclaimer.body.Bodies.store(player, body);
        return 1;
    }

    /** Builds a test scene in deep space and puts the player where it can be seen. */
    private static int linkStatus(CommandSourceStack source, ServerPlayer player) {
        var state = com.voidreclaimer.link.Links.state(player);
        source.sendSuccess(() -> state.link().map(link -> Component.translatable("commands.void_reclaimer.link.status.linked",
                link.shell().toString(), link.epoch(), link.from().dimension().location().toString(), link.from().position().toString()))
                .orElse(Component.translatable("commands.void_reclaimer.link.status.home")), false);
        return state.linked() ? 1 : 0;
    }

    /** Every shell the player has owned, as their player file has it. The result is how many are still in use: gone ones do not count. */
    private static int linkShells(CommandSourceStack source, ServerPlayer player) {
        var state = com.voidreclaimer.link.Links.state(player);
        if (state.shells().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.link.shells.none"), false);
            return 0;
        }
        int count = 0;
        for (var known : state.shells()) {
            boolean inUse = state.link().filter(link -> link.shell().equals(known.shell())).isPresent();
            String how = inUse ? "in_use" : known.gone() ? "gone" : known.wrecked() ? "wrecked" : "idle";
            count += known.gone() ? 0 : 1;
            var at = known.at().position();
            source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.link.shells.entry", known.shell().toString(), known.epoch(),
                    known.at().dimension().location().toString(), String.format(java.util.Locale.ROOT, "%.0f %.0f %.0f", at.x, at.y, at.z),
                    Component.translatable("commands.void_reclaimer.link.shells." + how)), false);
        }
        return count;
    }

    /** Debug: a link with no seat or signal. The move runs at the end of the tick, so a command's result only says it was asked for. */
    private static int linkForce(CommandSourceStack source, ServerPlayer player, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof com.voidreclaimer.link.BodyHost shell)) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.link.not_a_shell"));
            return 0;
        }
        com.voidreclaimer.link.Links.requestLink(player, shell, result -> reportLink(source, result));
        return 1;
    }

    /** Debug: {@link #linkForce} for a shell by its id, wherever it is. The shell's chunks load first, so the report comes later. */
    private static int linkReach(CommandSourceStack source, ServerPlayer player, java.util.UUID shell) {
        com.voidreclaimer.link.Links.requestLink(player, shell, result -> reportLink(source, result));
        return 1;
    }

    private static int linkWake(CommandSourceStack source, ServerPlayer player) {
        com.voidreclaimer.link.Links.requestWake(player, result -> reportLink(source, result));
        return 1;
    }

    private static void reportLink(CommandSourceStack source, com.voidreclaimer.link.Links.Result result) {
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.link.result", result.name()), false);
    }

    private static final java.util.List<net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis>> SHELL_BODIES = java.util.List.of(
            com.voidreclaimer.body.BuiltinBodies.DRONE, com.voidreclaimer.body.BuiltinBodies.MECH, com.voidreclaimer.body.BuiltinBodies.SHIP,
            com.voidreclaimer.body.BuiltinBodies.SHUTTLE, com.voidreclaimer.body.BuiltinBodies.TITAN);

    private static final java.util.Map<java.util.UUID, com.voidreclaimer.debug.ItemCensus> LAST_CENSUS = new java.util.HashMap<>();
    private static final double CENSUS_RANGE = 32.0;

    /** Counts everything the player carries and the items lying near them, and says what changed since the last count. The result is the total. */
    private static int census(CommandSourceStack source, ServerPlayer player) {
        var carried = com.voidreclaimer.debug.ItemCensus.of(player);
        var nearby = com.voidreclaimer.debug.ItemCensus.nearby(player, CENSUS_RANGE);
        var total = carried.plus(nearby);
        var last = LAST_CENSUS.put(player.getUUID(), total);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.link.census", carried.total(), nearby.total()), false);
        if (last != null) {
            var changes = total.since(last);
            source.sendSuccess(() -> changes.isEmpty() ? Component.translatable("commands.void_reclaimer.link.census.same")
                    : Component.translatable("commands.void_reclaimer.link.census.changed", changes.toString()), false);
        }
        return total.total();
    }

    /** Debug: nothing in survival makes a shell yet. */
    private static int spawnShell(CommandSourceStack source, String name, boolean full) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var chassis = SHELL_BODIES.stream().filter(key -> key.location().getPath().equals(name)).findFirst();
        if (chassis.isEmpty()) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.link.unknown", name));
            return 0;
        }
        com.voidreclaimer.link.BodyHost.shellAhead(source.getPlayerOrException(), com.voidreclaimer.body.Bodies.preset(chassis.get(), full));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.link.spawned", name), true);
        return 1;
    }

    private static int chunks(CommandSourceStack source, net.minecraft.world.level.ChunkPos pos) {
        com.voidreclaimer.debug.ChunkReadout.of(source.getLevel(), pos).forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return 1;
    }

    private static int scene(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var scene = java.util.Arrays.stream(com.voidreclaimer.debug.TestScenes.Scene.values()).filter(s -> s.id().equals(name)).findFirst();
        if (scene.isEmpty()) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.scene.unknown", name));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        net.minecraft.server.level.ServerLevel space = source.getServer().getLevel(com.voidreclaimer.space.ModDimensions.DEEP_SPACE);
        if (space == null) {
            return 0;
        }
        var view = com.voidreclaimer.debug.TestScenes.build(space, scene.get(), player.getUUID());
        player.teleportTo(space, view.at().x, view.at().y, view.at().z, view.yaw(), view.pitch());
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.scene.built", name), true);
        return 1;
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(VoidReclaimer.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("wear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("condition", FloatArgumentType.floatArg(0f, 1f))
                                        .executes(ctx -> wear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "condition"))))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("chunks")
                                .executes(ctx -> chunks(ctx.getSource(), new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition()))))
                                .then(Commands.argument("where", net.minecraft.commands.arguments.coordinates.ColumnPosArgument.columnPos())
                                        .executes(ctx -> chunks(ctx.getSource(), net.minecraft.commands.arguments.coordinates.ColumnPosArgument.getColumnPos(ctx, "where").toChunkPos())))))
                .then(Commands.literal("link")
                        .then(Commands.literal("census")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> census(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("body", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                SHELL_BODIES.stream().map(key -> key.location().getPath()), builder))
                                        .executes(ctx -> spawnShell(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "body"), false))
                                        .then(Commands.literal("full")
                                                .executes(ctx -> spawnShell(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "body"), true)))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> linkStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("shells")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> linkShells(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("reach")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shell", net.minecraft.commands.arguments.UuidArgument.uuid())
                                                .executes(ctx -> linkReach(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        net.minecraft.commands.arguments.UuidArgument.getUuid(ctx, "shell"))))))
                        .then(Commands.literal("force")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shell", EntityArgument.entity())
                                                .executes(ctx -> linkForce(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), EntityArgument.getEntity(ctx, "shell"))))))
                        .then(Commands.literal("wake")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> linkWake(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("scene")
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(com.voidreclaimer.debug.TestScenes.Scene.values()).map(com.voidreclaimer.debug.TestScenes.Scene::id), builder))
                                .executes(ctx -> scene(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name")))))
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
                                        .then(Commands.argument("kind", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        DustKinds.all(ctx.getSource().registryAccess()).stream().map(DustKinds.Entry::id), builder))
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                                                        .executes(ctx -> setDust(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                ResourceLocationArgument.getId(ctx, "kind"),
                                                                FloatArgumentType.getFloat(ctx, "amount")))))))
                        .then(Commands.literal("locate")
                                .executes(ctx -> locate(ctx.getSource(), DEFAULT_LOCATE_RANGE))
                                .then(Commands.argument("range", IntegerArgumentType.integer(1, 100_000))
                                        .executes(ctx -> locate(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "range"))))))
                .then(Commands.literal("asteroid")
                        .then(Commands.literal("locate")
                                .executes(ctx -> locateAsteroid(ctx.getSource(), null))
                                .then(Commands.argument("size", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(AsteroidBody.Size.values()).map(s -> s.name().toLowerCase(Locale.ROOT)), builder))
                                        .executes(ctx -> locateAsteroid(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "size").toUpperCase(Locale.ROOT))))))
                .then(HoloCommand.node())
                .then(Commands.literal("dyson")
                        .then(Commands.literal("get").executes(ctx -> getDyson(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.literal("satellites")
                                        .then(Commands.argument("count", LongArgumentType.longArg(0))
                                                .executes(ctx -> setDyson(ctx.getSource(), true, LongArgumentType.getLong(ctx, "count")))))
                                .then(Commands.literal("trusses")
                                        .then(Commands.argument("count", LongArgumentType.longArg(0))
                                                .executes(ctx -> setDyson(ctx.getSource(), false, LongArgumentType.getLong(ctx, "count")))))))
                .then(Commands.literal("sky")
                        .then(Commands.literal("era")
                                .then(Commands.literal("get").executes(ctx -> getSkyEra(ctx.getSource())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("era", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(com.voidreclaimer.earth.SkyEra.values()).map(com.voidreclaimer.earth.SkyEra::getSerializedName), builder))
                                                .executes(ctx -> setSkyEra(ctx.getSource(), StringArgumentType.getString(ctx, "era"))))))
                        .then(Commands.literal("sectors")
                                .executes(ctx -> getSectors(ctx.getSource()))
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, com.voidreclaimer.earth.Sectors.COUNT))
                                        .executes(ctx -> setSectors(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("starship")
                        .then(Commands.literal("spawn").executes(ctx -> spawnStarship(ctx.getSource()))))
                .then(Commands.literal("milestone")
                        .then(milestoneAction("grant", true))
                        .then(milestoneAction("revoke", false)))
                .then(Commands.literal("body")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("preset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("drone").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.DRONE, false))
                                                .then(Commands.literal("full").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.DRONE, true))))
                                        .then(Commands.literal("mech").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.MECH, false))
                                                .then(Commands.literal("full").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.MECH, true))))
                                        .then(Commands.literal("ship").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.SHIP, false))
                                                .then(Commands.literal("full").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.SHIP, true))))
                                        .then(Commands.literal("shuttle").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.SHUTTLE, false))
                                                .then(Commands.literal("full").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.SHUTTLE, true))))
                                        .then(Commands.literal("titan").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.TITAN, false))
                                                .then(Commands.literal("full").executes(ctx -> presetBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.voidreclaimer.body.BuiltinBodies.TITAN, true))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("socket", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(ctx -> removeSection(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "socket"))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> getBody(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("core")
                        .then(Commands.literal("charge")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> chargeCores(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> milestoneAction(String name, boolean grant) {
        return Commands.literal(name)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("milestone", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        Milestones.all(ctx.getSource().registryAccess()).stream().map(e -> e.key().location()), builder))
                                .executes(ctx -> setMilestone(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        ResourceLocationArgument.getId(ctx, "milestone"), grant))));
    }

    /** Cells searched around the command position, in rings, so the nearest cell wins: 6 rings is about 6000 blocks. */
    private static final int ASTEROID_SEARCH_CELLS = 6;

    private static int locateAsteroid(CommandSourceStack source, String sizeName) {
        if (!com.voidreclaimer.space.ModDimensions.isDeepSpace(source.getLevel())) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.asteroid.no_field"));
            return 0;
        }
        AsteroidBody.Size size = null;
        if (sizeName != null) {
            try {
                size = AsteroidBody.Size.valueOf(sizeName);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.translatable("commands.void_reclaimer.asteroid.unknown_size", sizeName.toLowerCase(Locale.ROOT)));
                return 0;
            }
        }
        net.minecraft.world.phys.Vec3 spawn = new Vec3(com.voidreclaimer.Config.SPAWN_X.get() + 0.5, com.voidreclaimer.Config.SPAWN_Y.get(),
                com.voidreclaimer.Config.SPAWN_Z.get() + 0.5);
        AsteroidField field = new AsteroidField(source.getLevel().getSeed(), spawn, AsteroidChunkGenerator.SPAWN_CLEARANCE);
        Vec3 pos = source.getPosition();
        int cx0 = Mth.floor(pos.x / AsteroidField.CELL);
        int cy0 = Mth.floor(pos.y / AsteroidField.CELL);
        int cz0 = Mth.floor(pos.z / AsteroidField.CELL);
        AsteroidBody best = null;
        for (int dx = -ASTEROID_SEARCH_CELLS; dx <= ASTEROID_SEARCH_CELLS; dx++) {
            for (int dy = -ASTEROID_SEARCH_CELLS; dy <= ASTEROID_SEARCH_CELLS; dy++) {
                for (int dz = -ASTEROID_SEARCH_CELLS; dz <= ASTEROID_SEARCH_CELLS; dz++) {
                    for (AsteroidBody body : field.bodiesIn(cx0 + dx, cy0 + dy, cz0 + dz)) {
                        if ((size == null || body.size() == size) && (best == null || body.center().distanceToSqr(pos) < best.center().distanceToSqr(pos))) {
                            best = body;
                        }
                    }
                }
            }
        }
        if (best == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.asteroid.locate.none", ASTEROID_SEARCH_CELLS * AsteroidField.CELL));
            return 0;
        }
        AsteroidBody found = best;
        int distance = Mth.floor(found.center().distanceTo(pos));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.asteroid.locate",
                found.size().name().toLowerCase(Locale.ROOT), DustKinds.name(source.registryAccess(), found.kind()),
                Mth.floor(found.center().x), Mth.floor(found.center().y), Mth.floor(found.center().z),
                Mth.floor(2 * Math.max(found.rx(), Math.max(found.ry(), found.rz()))), distance), false);
        return distance;
    }

    private static int getDyson(CommandSourceStack source) {
        DysonSphereData data = DysonSphereData.of(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dyson.get", data.satellites(), data.trusses(),
                data.shellTotal(), data.tier()), false);
        return data.tier();
    }

    private static int setDyson(CommandSourceStack source, boolean satellites, long count) {
        DysonSphereData data = DysonSphereData.of(source.getServer());
        DysonSphereData updated = Dyson.set(source.getServer(), satellites ? data.withSatellites(count) : data.withTrusses(count));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dyson.set", updated.satellites(), updated.trusses(),
                updated.tier()), true);
        return updated.tier();
    }

    private static int getSkyEra(CommandSourceStack source) {
        com.voidreclaimer.earth.SkyEra era = com.voidreclaimer.earth.EarthSky.get(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.sky.era.get", era.getSerializedName()), false);
        return era.ordinal();
    }

    private static int getSectors(CommandSourceStack source) {
        int count = com.voidreclaimer.earth.EarthRestoration.map(source.getServer()).count();
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.sky.sectors", count), false);
        return count;
    }

    /** Marks the first {@code count} sectors restored and no others; the ground does not change. */
    private static int setSectors(CommandSourceStack source, int count) {
        net.minecraft.server.level.ServerLevel earth = source.getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        if (earth == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.sky.era.no_dimension"));
            return 0;
        }
        com.voidreclaimer.earth.EarthRestoration.set(earth, new com.voidreclaimer.earth.SectorMap((1 << count) - 1));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.sky.sectors", count), true);
        return count + 1;
    }

    private static int setSkyEra(CommandSourceStack source, String name) {
        com.voidreclaimer.earth.SkyEra era = Arrays.stream(com.voidreclaimer.earth.SkyEra.values())
                .filter(candidate -> candidate.getSerializedName().equals(name)).findFirst().orElse(null);
        if (era == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.sky.era.unknown", name));
            return 0;
        }
        if (!com.voidreclaimer.earth.EarthSky.set(source.getServer(), era)) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.sky.era.no_dimension"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.sky.era.set", era.getSerializedName()), true);
        return era.ordinal() + 1;
    }

    private static int setMilestone(CommandSourceStack source, ServerPlayer player, ResourceLocation id, boolean grant) {
        // "awakening" parses as minecraft:awakening; fall back to ours.
        ResourceKey<Milestone> key = ResourceKey.create(VoidReclaimerRegistries.MILESTONE, id);
        if (Milestones.get(source.registryAccess(), key) == null && id.getNamespace().equals("minecraft")) {
            key = ResourceKey.create(VoidReclaimerRegistries.MILESTONE, VoidReclaimer.id(id.getPath()));
        }
        ResourceKey<Milestone> milestone = key;
        if (Milestones.get(source.registryAccess(), milestone) == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.milestone.unknown", id.toString()));
            return 0;
        }
        ReplicatorData data = Replicator.data(player);
        Replicator.store(player, grant ? data.withUnlocked(milestone) : data.withoutUnlocked(milestone));
        source.sendSuccess(() -> Component.translatable(grant ? "commands.void_reclaimer.milestone.granted"
                : "commands.void_reclaimer.milestone.revoked", Milestones.title(source.registryAccess(), milestone), player.getDisplayName()), true);
        return 1;
    }

    /** Back to the starter drone. Installed parts are discarded (it's a debug command). */
    private static int resetBody(CommandSourceStack source, ServerPlayer player) {
        Bodies.store(player, Bodies.starter());
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.body.reset", player.getDisplayName()), true);
        return 1;
    }

    private static int presetBody(CommandSourceStack source, ServerPlayer player, net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> chassis, boolean full) {
        Bodies.store(player, Bodies.preset(chassis, full));
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.body.preset", player.getDisplayName(), chassis.location().getPath()), true);
        return 1;
    }

    /** Takes a section off a socket (its mounted modules and child sections go with it), to try the body without it. */
    private static int removeSection(CommandSourceStack source, ServerPlayer player, String socket) {
        com.voidreclaimer.body.BodyRules.Check check = Bodies.removeSection(player, socket);
        source.sendSuccess(() -> Component.literal(socket + ": " + check), true);
        return check == com.voidreclaimer.body.BodyRules.Check.OK ? 1 : 0;
    }

    private static int getBody(CommandSourceStack source, ServerPlayer player) {
        BodyStats stats = Bodies.stats(player);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.body.get", player.getDisplayName(),
                Component.translatable("body." + stats.size().location().getNamespace() + ".size." + stats.size().location().getPath()),
                format(stats.mass()), format(stats.massBudget()), stats.inventorySlots(),
                format((float) (stats.flight().maxSpeed() * 20))), false);
        return stats.inventorySlots();
    }

    private static int chargeCores(CommandSourceStack source, ServerPlayer player) {
        int count = BackupCores.fillAll(player);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.core.charged", count, player.getDisplayName()), true);
        return count;
    }

    private static final int DEFAULT_LOCATE_RANGE = 4096;

    private static int getDust(CommandSourceStack source, ServerPlayer player) {
        DustData dust = player.getData(ModAttachments.DUST);
        MutableComponent list = Component.empty();
        for (DustKinds.Entry entry : DustKinds.all(source.registryAccess())) {
            if (!list.getSiblings().isEmpty()) {
                list.append(", ");
            }
            list.append(Component.translatable("commands.void_reclaimer.dust.amount",
                    entry.kind().displayName(entry.id()), format(dust.get(entry.id()))));
        }
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.get", player.getDisplayName(), list), false);
        return Mth.floor(dust.total());
    }

    private static int setDust(CommandSourceStack source, ServerPlayer player, ResourceLocation kindId, float amount) {
        ResourceLocation kind = kindId.getNamespace().equals("minecraft") && DustKinds.get(source.registryAccess(), kindId) == null
                ? VoidReclaimer.id(kindId.getPath()) : kindId;
        if (DustKinds.get(source.registryAccess(), kind) == null) {
            source.sendFailure(Component.translatable("commands.void_reclaimer.dust.unknown_kind", kindId.toString()));
            return 0;
        }
        float clamped = Math.min(amount, DustData.capacity(player));
        player.setData(ModAttachments.DUST, player.getData(ModAttachments.DUST).with(kind, clamped));
        // A setup command stands in for having gathered it, so milestones still unlock in test worlds.
        com.voidreclaimer.dust.DustLedger.raiseTo(player, kind, clamped);
        player.syncData(ModAttachments.DUST);
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.set", player.getDisplayName(),
                DustKinds.name(source.registryAccess(), kind), format(clamped)), true);
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
        source.sendSuccess(() -> Component.translatable("commands.void_reclaimer.dust.locate", DustKinds.name(source.registryAccess(), c.kind()),
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
