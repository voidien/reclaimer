package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.Blueprint;
import com.voidreclaimer.body.Blueprints;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.Modules;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: a Body tab or module wheel action. The server validates everything ({@link Bodies}).
 *
 * @param socket        socket (section and module actions)
 * @param kind          mount kind (module actions)
 * @param index         mount index (module actions) or tool index (select)
 * @param inventorySlot source inventory slot (installs)
 * @param cosmetics     new looks (cosmetics)
 */
public record BodyActionPayload(Action action, String socket, ModuleKind kind, int index, int inventorySlot, Cosmetics cosmetics)
        implements CustomPacketPayload {
    public static final Type<BodyActionPayload> TYPE = new Type<>(VoidReclaimer.id("body_action"));

    public enum Action {
        INSTALL_SECTION, REMOVE_SECTION, INSTALL_MODULE, REMOVE_MODULE, SELECT_TOOL, COSMETICS, USE_TOOL, REPAIR,
        BLUEPRINT_SAVE, BLUEPRINT_APPLY, BLUEPRINT_QUEUE;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    public static final StreamCodec<ByteBuf, BodyActionPayload> STREAM_CODEC = StreamCodec.composite(
            Action.STREAM_CODEC, BodyActionPayload::action,
            ByteBufCodecs.STRING_UTF8, BodyActionPayload::socket,
            ModuleKind.STREAM_CODEC, BodyActionPayload::kind,
            ByteBufCodecs.VAR_INT, BodyActionPayload::index,
            ByteBufCodecs.VAR_INT, BodyActionPayload::inventorySlot,
            Cosmetics.STREAM_CODEC, BodyActionPayload::cosmetics,
            BodyActionPayload::new);

    public static BodyActionPayload installSection(String socket, int inventorySlot) {
        return new BodyActionPayload(Action.INSTALL_SECTION, socket, ModuleKind.CORE, 0, inventorySlot, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload removeSection(String socket) {
        return new BodyActionPayload(Action.REMOVE_SECTION, socket, ModuleKind.CORE, 0, 0, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload installModule(BodyData.Mount mount, int inventorySlot) {
        return new BodyActionPayload(Action.INSTALL_MODULE, mount.socket(), mount.kind(), mount.index(), inventorySlot, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload removeModule(BodyData.Mount mount) {
        return new BodyActionPayload(Action.REMOVE_MODULE, mount.socket(), mount.kind(), mount.index(), 0, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload selectTool(int index) {
        return new BodyActionPayload(Action.SELECT_TOOL, "", ModuleKind.TOOL, index, 0, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload cosmetics(Cosmetics cosmetics) {
        return new BodyActionPayload(Action.COSMETICS, "", ModuleKind.COSMETIC, 0, 0, cosmetics);
    }

    public static BodyActionPayload repair(String socket) {
        return new BodyActionPayload(Action.REPAIR, socket, ModuleKind.CORE, 0, 0, Cosmetics.DEFAULT);
    }

    /** A blueprint action on the blueprint in {@code inventorySlot}. */
    public static BodyActionPayload blueprint(Action action, int inventorySlot) {
        return new BodyActionPayload(action, "", ModuleKind.CORE, 0, inventorySlot, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload useTool() {
        return new BodyActionPayload(Action.USE_TOOL, "", ModuleKind.TOOL, 0, 0, Cosmetics.DEFAULT);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void blueprint(ServerPlayer player, BodyActionPayload payload) {
        int slot = payload.inventorySlot();
        if (slot < 0 || slot >= player.getInventory().items.size()) {
            return;
        }
        ItemStack stack = player.getInventory().items.get(slot);
        if (!stack.is(ModItems.BODY_BLUEPRINT.get())) {
            return;
        }
        Blueprint blueprint = stack.get(ModDataComponents.BLUEPRINT.get());
        switch (payload.action()) {
            case BLUEPRINT_SAVE -> Blueprints.save(player, stack);
            case BLUEPRINT_APPLY -> {
                if (blueprint != null) {
                    Blueprints.apply(player, blueprint);
                }
            }
            default -> {
                if (blueprint != null) {
                    Blueprints.queueMissing(player, blueprint);
                }
            }
        }
    }

    static void handle(BodyActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BodyData.Mount mount = new BodyData.Mount(payload.socket(), payload.kind(), payload.index());
        switch (payload.action()) {
            case INSTALL_SECTION -> Bodies.installSection(player, payload.socket(), payload.inventorySlot());
            case REMOVE_SECTION -> Bodies.removeSection(player, payload.socket());
            case INSTALL_MODULE -> Bodies.installModule(player, mount, payload.inventorySlot());
            case REMOVE_MODULE -> Bodies.removeModule(player, mount);
            case SELECT_TOOL -> Bodies.selectTool(player, payload.index());
            case COSMETICS -> Bodies.setCosmetics(player, payload.cosmetics());
            case USE_TOOL -> Modules.use(player);
            case REPAIR -> Bodies.repair(player, payload.socket());
            case BLUEPRINT_SAVE, BLUEPRINT_APPLY, BLUEPRINT_QUEUE -> blueprint(player, payload);
        }
    }
}
