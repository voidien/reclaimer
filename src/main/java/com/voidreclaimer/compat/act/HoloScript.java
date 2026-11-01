package com.voidreclaimer.compat.act;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.display.HoloText;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.Interpreter;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.ScriptCallable;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.ScriptList;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.ScriptMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The {@code holo} module of a CogScript program, which lets it write to Holo Projectors within {@link #RANGE} blocks of its
 * computer. A projector's address is its position as {@code "x,y,z"}, which {@code holo.list()} and {@code holo.find()} give.
 * Why this exists and how it is wired in: {@code docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md}.
 *
 * <p>Scripts run on the computer's own thread and the world may only be touched on the server's, so every function goes
 * through {@code onMain}, the same hop the addon's own modules make.
 */
public final class HoloScript {
    public static final int RANGE = 16;

    private HoloScript() {}

    /** Hands a world access to the server thread and waits for it; a script that is already there runs it directly. */
    @FunctionalInterface
    public interface OnMain {
        Object call(Supplier<Object> work);
    }

    /** Defines the module in a new interpreter. */
    public static void install(Interpreter interpreter, BlockEntity computer, OnMain main) {
        interpreter.defineValue("holo", module(computer, main));
    }

    private static ScriptMap module(BlockEntity computer, OnMain main) {
        return new ScriptMap()
                .fn("list", (interp, args) -> main.call(() -> {
                    ScriptList out = new ScriptList();
                    for (HoloProjectorBlockEntity projector : inReach(computer)) {
                        out.add(address(projector.getBlockPos()));
                    }
                    return out;
                }))
                .fn("find", (interp, args) -> main.call(() -> {
                    List<HoloProjectorBlockEntity> found = inReach(computer);
                    return found.isEmpty() ? null : address(found.getFirst().getBlockPos());
                }))
                .fn("set", write(computer, main, (projector, args) -> {
                    Object color = args.has(3) ? args.get(3) : null;
                    Integer rgb = color == null ? Integer.valueOf(HoloDisplay.NO_COLOR) : HoloText.color(color);
                    if (rgb == null) {
                        throw args.error("not a colour: " + color);
                    }
                    return projector.setLine(args.integer(1) - 1, args.string(2), rgb);
                }))
                .fn("lines", write(computer, main, (projector, args) -> {
                    List<String> lines = new ArrayList<>();
                    for (Object line : args.list(1)) {
                        lines.add(String.valueOf(line));
                    }
                    projector.setLines(lines);
                    return true;
                }))
                .fn("clear", write(computer, main, (projector, args) -> {
                    projector.clearLines();
                    return true;
                }))
                .fn("color", write(computer, main, (projector, args) -> {
                    Integer rgb = HoloText.color(args.get(1));
                    if (rgb == null) {
                        throw args.error("not a colour: " + args.get(1));
                    }
                    projector.setColor(rgb);
                    return true;
                }))
                .fn("scale", write(computer, main, (projector, args) -> {
                    projector.setScale((float) args.number(1));
                    return true;
                }))
                .fn("height", write(computer, main, (projector, args) -> {
                    projector.setHeight((float) args.number(1));
                    return true;
                }))
                .fn("source", write(computer, main, (projector, args) -> {
                    projector.setSource(id(args));
                    return true;
                }))
                .fn("model", write(computer, main, (projector, args) -> {
                    ResourceLocation id = id(args);
                    projector.setModel(id);
                    return true;
                }))
                .fn("body", write(computer, main, (projector, args) -> {
                    projector.setBody();
                    return true;
                }))
                .fn("globe", write(computer, main, (projector, args) -> projector.setGlobe(args.string(1))))
                .fn("ship", write(computer, main, (projector, args) -> {
                    projector.setShip();
                    return true;
                }))
                .fn("get", write(computer, main, (projector, args) -> {
                    HoloDisplay display = projector.display();
                    return new ScriptMap()
                            .with("lines", new ScriptList(display.texts()))
                            .with("mode", display.mode().getSerializedName())
                            .with("source", display.source().toString())
                            .with("scale", (double) display.scale())
                            .with("height", (double) display.height());
                }));
    }

    /** The id in the second argument; a bare one means ours. */
    private static ResourceLocation id(ScriptCallable.Args args) {
        String text = args.string(1);
        ResourceLocation id = ResourceLocation.tryParse(text.indexOf(':') >= 0 ? text : VoidReclaimer.MOD_ID + ":" + text);
        if (id == null) {
            throw args.error("not an id: " + text);
        }
        return id;
    }

    private interface Write {
        Object apply(HoloProjectorBlockEntity projector, ScriptCallable.Args args);
    }

    /** A function whose first argument is a projector's address. */
    private static ScriptCallable.Body write(BlockEntity computer, OnMain main, Write write) {
        return (interp, args) -> {
            String address = args.string(0);
            return main.call(() -> {
                HoloProjectorBlockEntity projector = at(computer, address);
                if (projector == null) {
                    throw args.error("no Holo Projector at " + address + " within " + RANGE + " blocks");
                }
                return write.apply(projector, args);
            });
        };
    }

    static String address(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** The position in an address, or null when it is not one. */
    @Nullable
    static BlockPos parse(String address) {
        String[] parts = address.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()), Integer.parseInt(parts[2].strip()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static HoloProjectorBlockEntity at(BlockEntity computer, String address) {
        BlockPos pos = parse(address);
        Level level = computer.getLevel();
        if (pos == null || level == null || !pos.closerThan(computer.getBlockPos(), RANGE + 1) || !level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof HoloProjectorBlockEntity projector ? projector : null;
    }

    /** Every projector in reach, nearest first. */
    private static List<HoloProjectorBlockEntity> inReach(BlockEntity computer) {
        Level level = computer.getLevel();
        BlockPos center = computer.getBlockPos();
        List<HoloProjectorBlockEntity> out = new ArrayList<>();
        if (level == null) {
            return out;
        }
        for (int cx = (center.getX() - RANGE) >> 4; cx <= (center.getX() + RANGE) >> 4; cx++) {
            for (int cz = (center.getZ() - RANGE) >> 4; cz <= (center.getZ() + RANGE) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof HoloProjectorBlockEntity projector && entity.getBlockPos().closerThan(center, RANGE + 1)) {
                        out.add(projector);
                    }
                }
            }
        }
        out.sort(Comparator.<HoloProjectorBlockEntity>comparingDouble(projector -> projector.getBlockPos().distSqr(center))
                .thenComparing(BlockEntity::getBlockPos));
        return out;
    }
}
