package com.voidreclaimer.link;

import com.voidreclaimer.registry.ModBlockEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Uplink Seat. Its id lasts as long as the block does, wherever a ship takes it, because a link has to find its seat again and a
 * position on a ship means nothing. Who sits in it is not stored: a link says which seat it came from, so a wake by any route frees
 * the seat and there is no second record to disagree.
 */
public class UplinkSeatBlockEntity extends BlockEntity {
    private static final String ID = "seat_id";

    private UUID id = UUID.randomUUID();

    public UplinkSeatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPLINK_SEAT.get(), pos, state);
    }

    public UUID id() {
        return id;
    }

    /** The player linked from this seat, if they are online. */
    public Optional<ServerPlayer> occupant() {
        if (level == null || level.getServer() == null) {
            return Optional.empty();
        }
        return level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> Links.state(player).link().filter(link -> link.seat().filter(id::equals).isPresent()).isPresent())
                .findFirst();
    }

    /** Takes {@code player} to the shell that stands ready, or tells them why not. Stands in for the picker. */
    public void connect(ServerPlayer player) {
        if (occupant().isPresent()) {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.uplink.taken"), true);
            return;
        }
        Optional<UUID> shell = Links.state(player).idleShell();
        if (shell.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.uplink.no_shell"), true);
            return;
        }
        Links.requestLink(player, shell.get(), id, result -> {
            if (result != Links.Result.LINKED) {
                player.displayClientMessage(Component.translatable("message.void_reclaimer.uplink.failed", result.name()), true);
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID(ID, id);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(ID)) {
            id = tag.getUUID(ID);
        }
    }
}
