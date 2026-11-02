package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Parts one body into two along a {@linkplain Chassis#halfOf() half} and puts them back. Pure: what happens to the bodies is someone else's. */
public final class BodySplit {
    /**
     * @param rest the body that stays, with what isn't on the half
     * @param half the part that leaves, on {@code chassis}
     */
    public record Parts(BodyData rest, BodyData half) {}

    private BodySplit() {}

    /** Whether {@code body} has the part {@code half} would take. A part that has already left has nothing to leave with. */
    public static boolean canSplit(BodyData body, Chassis half) {
        return !body.section(half.coreSocket()).isEmpty();
    }

    /** {@code body} in two. Paint goes with both; what is waiting to be handed out stays with the body the player is in. */
    public static Parts split(BodyData body, Chassis half) {
        Set<String> leaving = new HashSet<>();
        for (Socket socket : half.sockets()) {
            leaving.add(socket.id());
        }
        Map<String, ItemStack> restSections = new LinkedHashMap<>();
        Map<String, ItemStack> halfSections = new LinkedHashMap<>();
        body.sections().forEach((socket, stack) -> (leaving.contains(socket) ? halfSections : restSections).put(socket, stack));
        Map<BodyData.Mount, ItemStack> restModules = new LinkedHashMap<>();
        Map<BodyData.Mount, ItemStack> halfModules = new LinkedHashMap<>();
        body.modules().forEach((mount, stack) -> (leaving.contains(mount.socket()) ? halfModules : restModules).put(mount, stack));
        return new Parts(new BodyData(body.coreSocket(), restSections, restModules, 0, body.cosmetics(), body.pending()),
                new BodyData(half.coreSocket(), halfSections, halfModules, 0, body.cosmetics(), List.of()));
    }

    /** The two together again. Anything {@code half} was waiting to hand out goes with the body. */
    public static BodyData join(BodyData rest, BodyData half) {
        Map<String, ItemStack> sections = new LinkedHashMap<>(rest.sections());
        sections.putAll(half.sections());
        Map<BodyData.Mount, ItemStack> modules = new LinkedHashMap<>(rest.modules());
        modules.putAll(half.modules());
        List<ItemStack> pending = new ArrayList<>(rest.pending());
        pending.addAll(half.pending());
        return new BodyData(rest.coreSocket(), sections, modules, rest.activeTool(), rest.cosmetics(), pending);
    }
}
