package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.display.HoloText;
import com.voidreclaimer.registry.ModRegistries;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Lua functions of the Holo Projector (CC: Tweaked generic peripheral). Lines count from 1, as Lua does. Writing text
 * switches the projector to text and {@code setSource} to a readout: the last writer wins.
 */
public final class HoloPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("holo_projector").toString();
    }

    /** Every line, as a list of strings. */
    @LuaFunction(mainThread = true)
    public final List<String> getLines(HoloProjectorBlockEntity holo) {
        return holo.display().texts();
    }

    /** Line {@code n} (from 1); {@code ""} past the last one. */
    @LuaFunction(mainThread = true)
    public final String getLine(HoloProjectorBlockEntity holo, int n) {
        return holo.display().text(n - 1);
    }

    /** {@code setLine(n, text[, color])}: writes one line; {@code true}, or {@code false} and why. Text is cut at the line length. */
    @LuaFunction(mainThread = true)
    public final MethodResult setLine(HoloProjectorBlockEntity holo, IArguments args) throws LuaException {
        int n = args.getInt(0);
        String text = args.getString(1);
        int color = args.get(2) == null ? HoloDisplay.NO_COLOR : color(args.get(2));
        return holo.setLine(n - 1, text, color) ? MethodResult.of(true) : MethodResult.of(false, "no_such_line");
    }

    /** {@code setLines({ "a", "b" })}: replaces all the text; long entries wrap on to the next lines. */
    @LuaFunction(mainThread = true)
    public final void setLines(HoloProjectorBlockEntity holo, Map<?, ?> lines) {
        List<String> texts = new ArrayList<>();
        for (int i = 1; lines.containsKey((double) i); i++) {
            texts.add(String.valueOf(lines.get((double) i)));
        }
        holo.setLines(texts);
    }

    @LuaFunction(mainThread = true)
    public final void clear(HoloProjectorBlockEntity holo) {
        holo.clearLines();
    }

    /** The colour of lines that have none of their own: 0xRRGGBB, {@code "#RRGGBB"} or a dye name. */
    @LuaFunction(mainThread = true)
    public final void setColor(HoloProjectorBlockEntity holo, IArguments args) throws LuaException {
        holo.setColor(color(args.get(0)));
    }

    @LuaFunction(mainThread = true)
    public final double getScale(HoloProjectorBlockEntity holo) {
        return holo.display().scale();
    }

    /** Size of the picture, 0.25 to 4 (held to that range). */
    @LuaFunction(mainThread = true)
    public final void setScale(HoloProjectorBlockEntity holo, double scale) {
        holo.setScale((float) scale);
    }

    @LuaFunction(mainThread = true)
    public final double getHeight(HoloProjectorBlockEntity holo) {
        return holo.display().height();
    }

    /** Blocks above the projector, 0 to 4 (held to that range). */
    @LuaFunction(mainThread = true)
    public final void setHeight(HoloProjectorBlockEntity holo, double height) {
        holo.setHeight((float) height);
    }

    /** Whether the picture turns to face the viewer. */
    @LuaFunction(mainThread = true)
    public final void setBillboard(HoloProjectorBlockEntity holo, boolean billboard) {
        holo.setBillboard(billboard);
    }

    /** What the projector shows now: {@code "text"} or {@code "readout"}. */
    @LuaFunction(mainThread = true)
    public final String getMode(HoloProjectorBlockEntity holo) {
        return holo.display().mode().name().toLowerCase(Locale.ROOT);
    }

    /** The chosen readout kind's id. */
    @LuaFunction(mainThread = true)
    public final String getSource(HoloProjectorBlockEntity holo) {
        return holo.display().source().toString();
    }

    /** Shows a readout from now on; see {@code getSources()}. {@code true}, or {@code false} and why. */
    @LuaFunction(mainThread = true)
    public final MethodResult setSource(HoloProjectorBlockEntity holo, String source) {
        ResourceLocation id = ResourceLocation.tryParse(source.indexOf(':') >= 0 ? source : VoidReclaimer.MOD_ID + ":" + source);
        if (id == null || !ModRegistries.HOLO_READOUTS.containsKey(id)) {
            return MethodResult.of(false, "unknown_source");
        }
        holo.setSource(id);
        return MethodResult.of(true);
    }

    /** Every readout kind's id, sorted. */
    @LuaFunction(mainThread = true)
    public final List<String> getSources(HoloProjectorBlockEntity holo) {
        return ModRegistries.HOLO_READOUTS.keySet().stream().map(ResourceLocation::toString).sorted().toList();
    }

    /** {@code lines, chars}: how much text fits. */
    @LuaFunction(mainThread = true)
    public final MethodResult getSize(HoloProjectorBlockEntity holo) {
        return MethodResult.of(HoloDisplay.MAX_LINES, HoloDisplay.MAX_CHARS);
    }

    private static int color(Object value) throws LuaException {
        Integer color = HoloText.color(value);
        if (color == null) {
            throw new LuaException("Expected a colour: 0xRRGGBB, \"#RRGGBB\" or a dye name");
        }
        return color;
    }
}
