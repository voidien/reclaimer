package com.voidreclaimer.core;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.tether.Tether;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server side of Backup Cores: registering, renaming and charging cores, keeping each owner's
 * {@link BackupCoreList} current, and the respawn at the core picked on the death screen.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class BackupCores {
    /** How close the owner must be to rename or charge a core. */
    private static final double USE_DISTANCE = 8.0;

    private BackupCores() {}

    public static BackupCoreList list(ServerPlayer player) {
        return player.getData(ModAttachments.BACKUP_CORES);
    }

    private static void store(ServerPlayer player, BackupCoreList list) {
        if (!list.equals(list(player))) {
            player.setData(ModAttachments.BACKUP_CORES, list);
            player.syncData(ModAttachments.BACKUP_CORES);
        }
    }

    private static GlobalPos at(ServerLevel level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }

    private static BackupCoreList.Entry entry(ServerLevel level, BackupCoreBlockEntity core) {
        return new BackupCoreList.Entry(at(level, core.getBlockPos()), core.name(), core.charges());
    }

    static void register(ServerPlayer player, BackupCoreBlockEntity core) {
        BackupCoreList list = list(player);
        core.setOwner(player.getUUID(), Component.translatable("block.void_reclaimer.backup_core.default_name", list.cores().size() + 1).getString());
        store(player, list.with(entry(player.serverLevel(), core)));
        player.displayClientMessage(Component.translatable("message.void_reclaimer.core.registered").withStyle(ChatFormatting.AQUA), true);
    }

    static void onCoreChanged(ServerLevel level, BackupCoreBlockEntity core) {
        ServerPlayer owner = online(level.getServer(), core.owner());
        if (owner != null) {
            store(owner, list(owner).with(entry(level, core)));
        }
    }

    static void onCoreRemoved(ServerLevel level, BlockPos pos, @Nullable UUID ownerId) {
        ServerPlayer owner = online(level.getServer(), ownerId);
        if (owner != null) {
            store(owner, list(owner).without(at(level, pos)));
        }
    }

    @Nullable
    private static ServerPlayer online(MinecraftServer server, @Nullable UUID id) {
        return id == null ? null : server.getPlayerList().getPlayer(id);
    }

    /** The owner's core at {@code pos}, loading its chunk if needed; null if it is gone or not theirs. */
    @Nullable
    private static BackupCoreBlockEntity core(ServerPlayer player, GlobalPos pos) {
        ServerLevel level = player.server.getLevel(pos.dimension());
        if (level == null) {
            return null;
        }
        level.getChunk(pos.pos());
        return level.getBlockEntity(pos.pos()) instanceof BackupCoreBlockEntity core && core.isOwnedBy(player.getUUID()) ? core : null;
    }

    /** Re-reads every listed core: updates names and charges, and drops cores that no longer exist. */
    public static void refresh(ServerPlayer player) {
        BackupCoreList list = list(player);
        BackupCoreList updated = list;
        for (BackupCoreList.Entry entry : list.cores()) {
            BackupCoreBlockEntity core = core(player, entry.pos());
            updated = core == null ? updated.without(entry.pos())
                    : updated.with(new BackupCoreList.Entry(entry.pos(), core.name(), core.charges()));
        }
        store(player, updated);
    }

    public static boolean rename(ServerPlayer player, BlockPos pos, String name) {
        BackupCoreBlockEntity core = usable(player, pos);
        if (core == null) {
            return false;
        }
        String clean = BackupCoreBlockEntity.cleanName(name);
        if (!clean.isEmpty()) {
            core.setName(clean);
        }
        return true;
    }

    /** Spends dust for one instant charge. */
    public static boolean charge(ServerPlayer player, BlockPos pos) {
        BackupCoreBlockEntity core = usable(player, pos);
        if (core == null) {
            return false;
        }
        if (core.charges() >= CoreCharging.MAX_CHARGES) {
            reply(player, "message.void_reclaimer.core.full");
            return false;
        }
        DustData dust = player.getData(ModAttachments.DUST);
        if (!CoreCharging.CHARGE_COST.canAfford(dust)) {
            reply(player, "message.void_reclaimer.replicator.not_enough_dust");
            return false;
        }
        player.setData(ModAttachments.DUST, CoreCharging.CHARGE_COST.takeFrom(dust));
        player.syncData(ModAttachments.DUST);
        core.setCharges(core.charges() + 1);
        return true;
    }

    /** Fills every core the player owns (debug command). Returns how many cores were found. */
    public static int fillAll(ServerPlayer player) {
        refresh(player);
        int count = 0;
        for (BackupCoreList.Entry entry : list(player).cores()) {
            BackupCoreBlockEntity core = core(player, entry.pos());
            if (core != null) {
                core.setCharges(CoreCharging.MAX_CHARGES);
                count++;
            }
        }
        return count;
    }

    @Nullable
    private static BackupCoreBlockEntity usable(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof BackupCoreBlockEntity core
                && core.isOwnedBy(player.getUUID())
                && player.position().distanceTo(Vec3.atCenterOf(pos)) <= USE_DISTANCE) {
            return core;
        }
        return null;
    }

    /** The death screen choice: a listed core, or empty for the free deep space spawn. */
    public static void select(ServerPlayer player, Optional<GlobalPos> pos) {
        store(player, list(player).withSelected(pos));
    }

    /**
     * Where to respawn if the player picked a core: it must still exist and hold a charge, which is spent. Returns
     * null for the default (free) deep space spawn. Clears the choice either way.
     */
    @Nullable
    public static DimensionTransition respawnAtSelectedCore(ServerPlayer player) {
        BackupCoreList list = list(player);
        Optional<GlobalPos> selected = list.selected();
        if (selected.isEmpty()) {
            return null;
        }
        store(player, list.withSelected(Optional.empty()));
        BackupCoreBlockEntity core = core(player, selected.get());
        ServerLevel level = player.server.getLevel(selected.get().dimension());
        BlockPos spot = core == null || level == null ? null : Tether.freeSpotNear(level, core.getBlockPos());
        if (core == null || core.charges() <= 0 || spot == null) {
            reply(player, "message.void_reclaimer.core.unavailable");
            return null;
        }
        core.setCharges(core.charges() - 1);
        // A core built into a station is addressed in the ship's own coordinates: respawn where the ship has carried it.
        Vec3 where = Vec3.atBottomCenterOf(spot);
        dev.ryanhcode.sable.sublevel.ServerSubLevel ship = com.voidreclaimer.ship.Ships.shipAt(level, core.getBlockPos());
        if (ship != null) {
            where = ship.logicalPose().transformPosition(where);
        }
        return new DimensionTransition(level, where, Vec3.ZERO, player.getYRot(), 0f, DimensionTransition.DO_NOTHING);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    /** Fresh charges and names for the death screen list. */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            store(player, list(player).withSelected(Optional.empty()));
            refresh(player);
        }
    }

    private static void reply(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }
}
