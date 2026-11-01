package com.voidreclaimer.link;

import com.voidreclaimer.registry.ModBlockEntities;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An Uplink Seat. Its id lasts as long as the block does, wherever a ship takes it, because a link has to find its seat again and a
 * position on a ship means nothing. Who sits in it is not stored: a link says which seat it came from, so a wake by any route frees
 * the seat and there is no second record to disagree.
 *
 * <p>While someone is linked from it the seat shows their main body, which is held in the player file and not in the world: a
 * mirror, rebuilt by the seat whenever it is missing and never saved, so there is never one on disk to go stale.
 */
public class UplinkSeatBlockEntity extends BlockEntity {
    private static final String ID = "seat_id";
    private static final int CHECK_TICKS = 20;

    private UUID id = UUID.randomUUID();
    @Nullable
    private BodyHost mirror;

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

    static void serverTick(Level level, BlockPos pos, BlockState state, UplinkSeatBlockEntity seat) {
        if (level.getGameTime() % CHECK_TICKS == 0) {
            seat.keepMirror((ServerLevel) level);
        }
    }

    private void keepMirror(ServerLevel level) {
        Optional<ServerPlayer> player = occupant();
        if (player.isEmpty()) {
            dropMirror();
        } else if (mirror == null || mirror.isRemoved()) {
            Links.state(player.get()).link()
                    .flatMap(link -> BodySnapshot.load(link.home(), level.registryAccess()))
                    .ifPresent(home -> mirror = BodyHost.spawn(level, Vec3.atBottomCenterOf(worldPosition.above()), HostRole.MIRROR, home.body(), player.get().getUUID()));
        }
    }

    private void dropMirror() {
        if (mirror != null) {
            mirror.discard();
            mirror = null;
        }
    }

    /** Also runs when the chunk unloads: the mirror is not saved, and the seat builds it again when it comes back. */
    @Override
    public void setRemoved() {
        dropMirror();
        super.setRemoved();
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
