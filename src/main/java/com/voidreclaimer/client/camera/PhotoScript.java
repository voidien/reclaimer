package com.voidreclaimer.client.camera;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.client.render.body.BodyBounds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Photo mode by command, for repeatable views: {@code /vrphoto}. The camera goes to exact spots, and each shot is taken a few frames
 * later, from the finished frame, with its facts beside it ({@link PhotoDebug#capture}). The commands work from play or, through
 * photo mode's chat (T), from inside it.
 *
 * <ul>
 * <li>{@code at x y z yaw pitch [fov]}: put the camera there</li>
 * <li>{@code focus crosshair|body|<x y z>}: what {@code orbit} and {@code sheet} look at; starts as the body</li>
 * <li>{@code orbit yaw pitch distance}: circle the focus</li>
 * <li>{@code debug true|false}: the debug layer</li>
 * <li>{@code labels true|false}: the body's socket and mount names, and their positions in the text file</li>
 * <li>{@code shot name}: {@code screenshots/name.png} and {@code .txt}</li>
 * <li>{@code sheet name [distance]}: four views of the focus ({@link PhotoSheet}), {@code name_front} and so on; distance 0 or none
 * fits the body</li>
 * <li>{@code exit}: leave photo mode</li>
 * </ul>
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class PhotoScript {
    /** Frames between moving the camera and taking the shot, so the panel and the picture are of the new view. */
    private static final int SETTLE_FRAMES = 15;
    /** How many body radii away a fitted sheet stands. */
    private static final double FIT = 2.6;
    private static final double MIN_DISTANCE = 0.7;

    private record Shot(@Nullable Vec3 focus, float yaw, float pitch, double distance, String name) {}

    private static final Deque<Shot> SHOTS = new ArrayDeque<>();
    private static int settle;
    @Nullable
    private static Vec3 focus;

    private PhotoScript() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("vrphoto")
                .then(Commands.literal("at").then(position(Commands.argument("yaw", FloatArgumentType.floatArg())
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90f, 90f))
                                .executes(c -> at(c, -1f))
                                .then(Commands.argument("fov", FloatArgumentType.floatArg(1f, 170f))
                                        .executes(c -> at(c, FloatArgumentType.getFloat(c, "fov"))))))))
                .then(Commands.literal("focus")
                        .then(Commands.literal("crosshair").executes(c -> focusCrosshair()))
                        .then(Commands.literal("body").executes(c -> {
                            focus = null;
                            return 1;
                        }))
                        .then(position(null)))
                .then(Commands.literal("orbit").then(Commands.argument("yaw", FloatArgumentType.floatArg())
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90f, 90f))
                                .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0.1, PhotoState.MAX_RANGE))
                                        .executes(PhotoScript::orbit)))))
                .then(Commands.literal("debug").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    boolean on = BoolArgumentType.getBool(c, "on");
                    later(mc -> PhotoDebug.set(mc, on));
                    return 1;
                })))
                .then(Commands.literal("labels").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    boolean on = BoolArgumentType.getBool(c, "on");
                    later(mc -> PhotoDebug.setLabels(on));
                    return 1;
                })))
                .then(Commands.literal("shot").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name");
                    later(mc -> queue(new Shot(null, 0f, 0f, 0.0, name)));
                    return 1;
                })))
                .then(Commands.literal("sheet").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> sheet(c, 0.0))
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0.0, PhotoState.MAX_RANGE))
                                .executes(c -> sheet(c, DoubleArgumentType.getDouble(c, "distance"))))))
                .then(Commands.literal("exit").executes(c -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.tell(() -> {
                        if (PhotoMode.active()) {
                            // Ends photo mode first: chat opened inside it would bring the control screen back when it closes.
                            PhotoMode.exit(mc);
                            mc.setScreen(null);
                        }
                    });
                    return 1;
                })));
    }

    /** {@code x y z}, then {@code next}; with no next step the three numbers set the focus. */
    private static ArgumentBuilder<CommandSourceStack, ?> position(@Nullable ArgumentBuilder<CommandSourceStack, ?> next) {
        var z = Commands.argument("z", DoubleArgumentType.doubleArg());
        if (next != null) {
            z.then(next);
        } else {
            z.executes(c -> {
                focus = new Vec3(DoubleArgumentType.getDouble(c, "x"), DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z"));
                return 1;
            });
        }
        return Commands.argument("x", DoubleArgumentType.doubleArg()).then(Commands.argument("y", DoubleArgumentType.doubleArg()).then(z));
    }

    /** Runs after the chat screen has closed, inside photo mode. */
    private static void later(Consumer<Minecraft> action) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            if (!PhotoMode.active()) {
                PhotoMode.enter(mc);
            }
            action.accept(mc);
        });
    }

    private static int at(CommandContext<CommandSourceStack> context, float fov) {
        Vec3 at = new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"));
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        later(mc -> {
            PhotoMode.state().place(at, yaw, pitch);
            if (fov > 0f) {
                PhotoMode.state().setFov(fov);
            }
        });
        return 1;
    }

    private static int focusCrosshair() {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            focus = hit.getLocation();
        }
        return 1;
    }

    private static Vec3 focusPoint(LocalPlayer player) {
        return focus != null ? focus : player.position().add(0.0, player.getBbHeight() / 2.0, 0.0);
    }

    private static int orbit(CommandContext<CommandSourceStack> context) {
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        double distance = DoubleArgumentType.getDouble(context, "distance");
        later(mc -> PhotoMode.state().orbit(focusPoint(mc.player), yaw, pitch, distance));
        return 1;
    }

    private static int sheet(CommandContext<CommandSourceStack> context, double distance) {
        String name = StringArgumentType.getString(context, "name");
        later(mc -> {
            LocalPlayer player = mc.player;
            double away = distance > 0.0 ? distance : Math.max(MIN_DISTANCE, BodyBounds.radius(Bodies.view(player)) * player.getScale() * FIT);
            float front = PhotoSheet.frontYaw(com.voidreclaimer.client.render.body.BodyRenderer.facing(player));
            for (PhotoSheet.View view : PhotoSheet.VIEWS) {
                queue(new Shot(focusPoint(player), front + view.yaw(), view.pitch(), away, name + "_" + view.suffix()));
            }
        });
        return 1;
    }

    private static void queue(Shot shot) {
        boolean first = SHOTS.isEmpty();
        SHOTS.add(shot);
        if (first) {
            place(shot);
        }
    }

    private static void place(Shot shot) {
        if (shot.focus() != null) {
            PhotoMode.state().orbit(shot.focus(), shot.yaw(), shot.pitch(), shot.distance());
        }
        settle = SETTLE_FRAMES;
    }

    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Post event) {
        if (SHOTS.isEmpty() || !PhotoMode.active()) {
            SHOTS.clear();
            return;
        }
        if (settle-- > 0) {
            return;
        }
        PhotoDebug.capture(Minecraft.getInstance(), SHOTS.poll().name());
        if (!SHOTS.isEmpty()) {
            place(SHOTS.peek());
        }
    }
}
