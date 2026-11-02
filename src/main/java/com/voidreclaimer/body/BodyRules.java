package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

/**
 * Pure install rules for the body. Moving items to and from the inventory is the caller's job ({@link Bodies}).
 *
 * <p>Tier gate: sections and modules must not exceed the core module's tier. Only the core section (and the core
 * module on it) is required; other sections may be missing, taking their mounts and stats with them.
 */
public final class BodyRules {
    private BodyRules() {}

    public enum Check {
        OK, NOT_A_SECTION, NOT_A_MODULE, NO_SUCH_SOCKET, WRONG_SOCKET, NO_SECTION, NO_SUCH_MOUNT, WRONG_MOUNT,
        TIER_TOO_HIGH, TOO_SMALL, CORE_TOO_LOW, REQUIRED, EMPTY, UNKNOWN_CHASSIS
    }

    /** The body after a change, and the items that left it. */
    public record Change(BodyData body, List<ItemStack> returned) {}

    // ---- sections -----------------------------------------------------------------------------------------

    /**
     * Whether {@code stack} may go into {@code socketId}, replacing what is there. The core socket takes any core
     * section: a different chassis changes the layout ({@link #installSection}).
     */
    public static Check canInstallSection(BodyView view, String socketId, ItemStack stack) {
        SectionStats section = Parts.section(stack);
        if (section == null) {
            return Check.NOT_A_SECTION;
        }
        Socket socket = view.chassis().socket(socketId);
        if (socket == null) {
            return Check.NO_SUCH_SOCKET;
        }
        boolean coreSocket = socketId.equals(view.chassis().coreSocket());
        boolean half = view.chassis().halfOf().isPresent();
        if (half ? !socket.accepts(section.socketType()) : coreSocket != section.isCore() || !coreSocket && !socket.accepts(section.socketType())) {
            return Check.WRONG_SOCKET;
        }
        if (coreSocket && !half && view.layouts().apply(section.chassis().get()) == null) {
            return Check.UNKNOWN_CHASSIS;
        }
        if (section.tier() > view.stats().coreTier()) {
            return Check.TIER_TOO_HIGH;
        }
        return Check.OK;
    }

    public static Check canRemoveSection(BodyView view, String socketId) {
        if (socketId.equals(view.chassis().coreSocket())) {
            return Check.REQUIRED;
        }
        return view.data().section(socketId).isEmpty() ? Check.EMPTY : Check.OK;
    }

    /**
     * Installs a section (already checked with {@link #canInstallSection}). The replaced section leaves; modules stay
     * on mounts the new section also has. A core section of another chassis switches the layout: sections and
     * modules that don't fit the new one leave.
     */
    public static Change installSection(BodyView view, String socketId, ItemStack stack) {
        BodyData data = view.data();
        List<ItemStack> returned = new ArrayList<>();
        ItemStack old = data.section(socketId);
        if (!old.isEmpty()) {
            returned.add(old);
        }
        Chassis layout = view.chassis();
        BodyData result;
        if (socketId.equals(layout.coreSocket())) {
            Chassis next = view.layouts().apply(Parts.section(stack).chassis().get());
            String nextCore = next.coreSocket();
            // Move the core and its modules to the new layout's core socket; everything else is re-checked below.
            Map<String, ItemStack> sections = new LinkedHashMap<>(data.sections());
            sections.remove(socketId);
            ItemStack displaced = sections.remove(nextCore);
            if (displaced != null && !nextCore.equals(socketId)) {
                returned.add(displaced);
            }
            sections.put(nextCore, stack);
            Map<BodyData.Mount, ItemStack> modules = new LinkedHashMap<>();
            data.modules().forEach((mount, module) -> modules.put(mount.socket().equals(socketId)
                    ? new BodyData.Mount(nextCore, mount.kind(), mount.index()) : mount, module));
            result = new BodyData(nextCore, sections, modules, data.activeTool(), data.cosmetics(), data.pending());
            layout = next;
        } else {
            result = data.withSection(socketId, stack);
        }
        Change normalized = normalize(result, layout);
        returned.addAll(normalized.returned());
        return new Change(normalized.body(), returned);
    }

    /** Takes a section and every module on it off the body. */
    public static Change removeSection(BodyView view, String socketId) {
        BodyData data = view.data();
        List<ItemStack> returned = new ArrayList<>();
        returned.add(data.section(socketId));
        Map<String, ItemStack> sections = new LinkedHashMap<>(data.sections());
        sections.remove(socketId);
        Map<BodyData.Mount, ItemStack> modules = new LinkedHashMap<>();
        data.modules().forEach((mount, module) -> {
            if (mount.socket().equals(socketId)) {
                returned.add(module.copy());
            } else {
                modules.put(mount, module);
            }
        });
        Change normalized = normalize(new BodyData(data.coreSocket(), sections, modules, data.activeTool(), data.cosmetics(),
                data.pending()), view.chassis());
        returned.addAll(normalized.returned());
        return new Change(normalized.body(), returned);
    }

    // ---- modules ------------------------------------------------------------------------------------------

    /** Whether {@code stack} may go on {@code mount}, replacing what is there. */
    public static Check canInstallModule(BodyView view, BodyData.Mount mount, ItemStack stack) {
        ModuleStats module = Parts.module(stack);
        if (module == null) {
            return Check.NOT_A_MODULE;
        }
        if (module.kind() != mount.kind()) {
            return Check.WRONG_MOUNT;
        }
        SectionStats section = view.section(mount.socket());
        if (section == null) {
            return Check.NO_SECTION;
        }
        if (mount.index() < 0 || mount.index() >= section.mounts(mount.kind())) {
            return Check.NO_SUCH_MOUNT;
        }
        if (mount.kind() == ModuleKind.CORE) {
            // A weaker core must still carry everything installed.
            if (highestTierExceptCore(view.data()) > module.tier()) {
                return Check.CORE_TOO_LOW;
            }
        } else if (module.tier() > view.stats().coreTier()) {
            return Check.TIER_TOO_HIGH;
        }
        if (module.minSize().isPresent()) {
            SizeClass needed = view.sizes().apply(module.minSize().get());
            if (needed != null && needed.rank() > view.stats().sizeRank()) {
                return Check.TOO_SMALL;
            }
        }
        return Check.OK;
    }

    /** The core module can only be replaced, never left empty. */
    public static Check canRemoveModule(BodyView view, BodyData.Mount mount) {
        if (mount.kind() == ModuleKind.CORE) {
            return Check.REQUIRED;
        }
        return view.data().module(mount).isEmpty() ? Check.EMPTY : Check.OK;
    }

    // ---- consistency --------------------------------------------------------------------------------------

    /**
     * Fits a body to a layout: sections in sockets that don't exist or don't accept them leave, and so do modules on
     * mounts the installed sections don't offer. The active tool index is kept in range.
     */
    public static Change normalize(BodyData data, Chassis chassis) {
        List<ItemStack> returned = new ArrayList<>();
        Map<String, ItemStack> sections = new LinkedHashMap<>();
        data.sections().forEach((socketId, stack) -> {
            Socket socket = chassis.socket(socketId);
            SectionStats section = Parts.section(stack);
            boolean core = socketId.equals(chassis.coreSocket());
            boolean fits = socket != null && section != null
                    && (chassis.halfOf().isPresent() ? socket.accepts(section.socketType())
                            : core ? section.isCore() : !section.isCore() && socket.accepts(section.socketType()));
            if (fits) {
                sections.put(socketId, stack);
            } else {
                returned.add(stack.copy());
            }
        });
        Map<BodyData.Mount, ItemStack> modules = new LinkedHashMap<>();
        data.modules().forEach((mount, stack) -> {
            SectionStats section = Parts.section(sections.getOrDefault(mount.socket(), ItemStack.EMPTY));
            ModuleStats module = Parts.module(stack);
            boolean fits = section != null && module != null && module.kind() == mount.kind()
                    && mount.index() >= 0 && mount.index() < section.mounts(mount.kind());
            if (fits) {
                modules.put(mount, stack);
            } else {
                returned.add(stack.copy());
            }
        });
        BodyData result = new BodyData(chassis.coreSocket(), sections, modules, data.activeTool(), data.cosmetics(), data.pending());
        int tools = 0;
        for (Socket socket : chassis.sockets()) {
            SectionStats section = Parts.section(sections.getOrDefault(socket.id(), ItemStack.EMPTY));
            tools += section == null ? 0 : section.mounts(ModuleKind.TOOL);
        }
        if (result.activeTool() >= tools) {
            result = result.withActiveTool(0);
        }
        return new Change(result, returned);
    }

    private static int highestTierExceptCore(BodyData data) {
        int tier = 0;
        for (ItemStack stack : data.sections().values()) {
            SectionStats section = Parts.section(stack);
            if (section != null) {
                tier = Math.max(tier, section.tier());
            }
        }
        for (Map.Entry<BodyData.Mount, ItemStack> entry : data.modules().entrySet()) {
            ModuleStats module = Parts.module(entry.getValue());
            if (module != null && entry.getKey().kind() != ModuleKind.CORE) {
                tier = Math.max(tier, module.tier());
            }
        }
        return tier;
    }

    /** Short reason text key for a failed check, or null when OK. */
    @Nullable
    public static String messageKey(Check check) {
        return check == Check.OK ? null : "message.void_reclaimer.body." + check.name().toLowerCase(java.util.Locale.ROOT);
    }
}
