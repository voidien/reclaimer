package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * A body together with its layout and the registry lookups the rules need. Built by {@link Bodies#view} in play, or
 * directly from {@link BuiltinBodies} in unit tests.
 *
 * @param data    the body
 * @param chassis the layout named by the core section
 * @param stats   the body's stats
 * @param sizes   size class lookup (null for unknown ids)
 * @param layouts chassis lookup (null for unknown ids)
 */
public record BodyView(BodyData data, Chassis chassis, BodyStats stats, Function<ResourceKey<SizeClass>, SizeClass> sizes,
                       Function<ResourceKey<Chassis>, Chassis> layouts) {
    /** The section in {@code socket}'s stats, or null. */
    @Nullable
    public SectionStats section(String socket) {
        return Parts.section(data.section(socket));
    }

    /** Every mount the installed sections offer, in socket order, then kind, then index. */
    public List<BodyData.Mount> mounts() {
        List<BodyData.Mount> mounts = new ArrayList<>();
        for (Socket socket : chassis.sockets()) {
            mounts.addAll(mounts(socket.id()));
        }
        return mounts;
    }

    /** The mounts the section in {@code socket} offers (none when the socket is empty). */
    public List<BodyData.Mount> mounts(String socket) {
        SectionStats section = section(socket);
        List<BodyData.Mount> mounts = new ArrayList<>();
        if (section != null) {
            for (ModuleKind kind : ModuleKind.values()) {
                for (int i = 0; i < section.mounts(kind); i++) {
                    mounts.add(new BodyData.Mount(socket, kind, i));
                }
            }
        }
        return mounts;
    }

    /** Tool mounts in wheel order. */
    public List<BodyData.Mount> toolMounts() {
        return mounts().stream().filter(m -> m.kind() == ModuleKind.TOOL).toList();
    }

    @Nullable
    public BodyData.Mount activeToolMount() {
        List<BodyData.Mount> tools = toolMounts();
        int index = data.activeTool();
        return index >= 0 && index < tools.size() ? tools.get(index) : null;
    }

    /** The active tool module, or empty. */
    public ItemStack activeTool() {
        BodyData.Mount mount = activeToolMount();
        return mount == null ? ItemStack.EMPTY : data.module(mount);
    }

    /** Sockets without a section. */
    public List<Socket> missing() {
        return chassis.sockets().stream().filter(s -> data.section(s.id()).isEmpty()).toList();
    }
}
