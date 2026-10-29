package com.voidreclaimer.body;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

/** Pure install rules for the body. Item movement to and from the inventory is the caller's job ({@link Bodies}). */
public final class BodyRules {
    private BodyRules() {}

    public enum Check {
        OK, NOT_A_PART, WRONG_SLOT, TIER_TOO_HIGH, FRAME_TOO_SMALL, BAD_SLOT, CORE_TOO_LOW, REQUIRED
    }

    /** Whether {@code stack} may go into slot {@code index} of {@code kind}, replacing what is there. */
    public static Check canInstall(BodyData body, PartKind kind, int index, ItemStack stack) {
        PartStats stats = Parts.stats(stack);
        if (stats == null) {
            return Check.NOT_A_PART;
        }
        if (stats.kind() != kind) {
            return Check.WRONG_SLOT;
        }
        if (index < 0 || index >= body.slots(kind)) {
            return Check.BAD_SLOT;
        }
        BodyStats current = body.stats();
        switch (kind) {
            case FRAME -> {
                // A new frame must be carried by the current core.
                if (stats.tier() > current.coreTier()) {
                    return Check.TIER_TOO_HIGH;
                }
            }
            case CORE -> {
                // A weaker core must still carry every installed part.
                for (ItemStack installed : allParts(body)) {
                    PartStats s = Parts.stats(installed);
                    if (s != null && s.kind() != PartKind.CORE && s.tier() > stats.tier()) {
                        return Check.CORE_TOO_LOW;
                    }
                }
                if (!current.size().atLeast(stats.minSize())) {
                    return Check.FRAME_TOO_SMALL;
                }
            }
            default -> {
                if (stats.tier() > current.coreTier()) {
                    return Check.TIER_TOO_HIGH;
                }
                if (!current.size().atLeast(stats.minSize())) {
                    return Check.FRAME_TOO_SMALL;
                }
            }
        }
        return Check.OK;
    }

    /** Frame and core can only be replaced, never left empty. */
    public static Check canRemove(BodyData body, PartKind kind, int index) {
        if (kind == PartKind.FRAME || kind == PartKind.CORE) {
            return Check.REQUIRED;
        }
        if (index < 0 || index >= body.slots(kind) || body.get(kind, index).isEmpty()) {
            return Check.BAD_SLOT;
        }
        return Check.OK;
    }

    /**
     * Installs a new frame: parts move into the new frame's slots in order; parts that no longer fit (too few slots,
     * or needing a bigger size class) are returned.
     */
    public static Swap swapFrame(BodyData body, ItemStack newFrame) {
        PartStats stats = Parts.stats(newFrame);
        SizeClass size = stats != null && stats.frame().isPresent() ? stats.frame().get().size() : SizeClass.DRONE;
        BodyData result = body.withFrame(newFrame);
        List<ItemStack> returned = new ArrayList<>();
        returned.add(body.frame());
        for (PartKind kind : PartKind.values()) {
            if (kind == PartKind.FRAME) {
                continue;
            }
            List<ItemStack> old = body.parts(kind);
            int slot = 0;
            for (int i = 0; i < old.size(); i++) {
                ItemStack part = old.get(i);
                if (part.isEmpty()) {
                    continue;
                }
                PartStats ps = Parts.stats(part);
                boolean fits = ps == null || size.atLeast(ps.minSize());
                // Clear the copied slot, then place the part into the next free slot of the new frame if it fits.
                if (i < result.slots(kind)) {
                    result = result.with(kind, i, ItemStack.EMPTY);
                }
                if (fits && slot < result.slots(kind)) {
                    result = result.with(kind, slot++, part);
                } else {
                    returned.add(part);
                }
            }
        }
        if (result.activeTool() >= result.slots(PartKind.TOOL)) {
            result = result.withActiveTool(0);
        }
        return new Swap(result, returned);
    }

    /** @param returned items that leave the body (the old frame and parts that don't fit) */
    public record Swap(BodyData body, List<ItemStack> returned) {}

    private static List<ItemStack> allParts(BodyData body) {
        return body.installedParts();
    }

    /** Short reason text key for a failed check, or null when OK. */
    @Nullable
    public static String messageKey(Check check) {
        return check == Check.OK ? null : "message.void_reclaimer.body." + check.name().toLowerCase(java.util.Locale.ROOT);
    }
}
