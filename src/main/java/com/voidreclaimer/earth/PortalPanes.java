package com.voidreclaimer.earth;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Every Atmospheric Portal pane of a level, kept by the pane block as it is placed and removed, so ships can be tested
 * against the portals they might fly through without scanning the world. Saved with the level, not synced.
 */
public record PortalPanes(List<BlockPos> panes) {
    public static final PortalPanes NONE = new PortalPanes(List.of());
    public static final Codec<PortalPanes> CODEC = BlockPos.CODEC.listOf().xmap(PortalPanes::new, PortalPanes::panes);

    public PortalPanes {
        panes = List.copyOf(panes);
    }

    public PortalPanes with(BlockPos pos) {
        if (panes.contains(pos)) {
            return this;
        }
        List<BlockPos> list = new ArrayList<>(panes);
        list.add(pos.immutable());
        return new PortalPanes(list);
    }

    public PortalPanes without(BlockPos pos) {
        if (!panes.contains(pos)) {
            return this;
        }
        List<BlockPos> list = new ArrayList<>(panes);
        list.remove(pos);
        return new PortalPanes(list);
    }
}
