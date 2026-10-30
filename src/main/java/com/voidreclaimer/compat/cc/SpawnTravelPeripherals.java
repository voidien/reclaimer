package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.CoreCharging;
import com.voidreclaimer.earth.AtmosphericPortals;
import com.voidreclaimer.earth.PortalShape;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.tether.Tether;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Lua functions of the spawn and travel blocks (CC: Tweaked), all read-only. The Backup Core has a block entity, so it is a
 * generic peripheral; the Tether Anchor and the Atmospheric Portal Frame are plain blocks, so they are {@link IPeripheral}s
 * that {@code CCCompat} attaches through CC's block capability.
 */
public final class SpawnTravelPeripherals {
    private SpawnTravelPeripherals() {}

    /** Backup Core: its name, owner and respawn charges. */
    public static final class BackupCore implements GenericPeripheral {
        @Override
        public String id() {
            return VoidReclaimer.id("backup_core").toString();
        }

        @LuaFunction(mainThread = true)
        public final String getName(BackupCoreBlockEntity core) {
            return core.name();
        }

        /** The owner's name if the server knows it, else the UUID; nil before anyone placed it. */
        @Nullable
        @LuaFunction(mainThread = true)
        public final String getOwner(BackupCoreBlockEntity core) {
            if (core.owner() == null) {
                return null;
            }
            var cache = core.getLevel() == null || core.getLevel().getServer() == null ? null : core.getLevel().getServer().getProfileCache();
            return Optional.ofNullable(cache).flatMap(c -> c.get(core.owner())).map(p -> p.getName()).orElse(core.owner().toString());
        }

        /** Respawn charges stored (each respawn here spends one). */
        @LuaFunction(mainThread = true)
        public final int getCharges(BackupCoreBlockEntity core) {
            return core.charges();
        }

        @LuaFunction(mainThread = true)
        public final int getMaxCharges(BackupCoreBlockEntity core) {
            return CoreCharging.MAX_CHARGES;
        }

        /** Progress of sunlight toward the next charge, 0 to 1 (0 when full). */
        @LuaFunction(mainThread = true)
        public final double getSunProgress(BackupCoreBlockEntity core) {
            return core.sunFraction();
        }
    }

    /** A peripheral fixed to one block, for blocks without a block entity. */
    abstract static class AtBlock implements IPeripheral {
        final Level level;
        final BlockPos pos;
        private final String type;

        AtBlock(Level level, BlockPos pos, String type) {
            this.level = level;
            this.pos = pos.immutable();
            this.type = VoidReclaimer.id(type).toString();
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return other instanceof AtBlock block && block.getClass() == getClass() && block.level == level && block.pos.equals(pos);
        }
    }

    /** Tether Anchor: who is clipped to it. */
    public static final class TetherAnchor extends AtBlock {
        public TetherAnchor(Level level, BlockPos pos) {
            super(level, pos, "tether_anchor");
        }

        /** Names of the players clipped to this anchor right now. */
        @LuaFunction(mainThread = true)
        public final List<String> getTethered() {
            List<String> names = new ArrayList<>();
            if (level instanceof ServerLevel server) {
                GlobalPos anchor = GlobalPos.of(level.dimension(), pos);
                for (ServerPlayer player : server.getServer().getPlayerList().getPlayers()) {
                    var data = Tether.data(player);
                    if (data.clipped() && data.isAnchoredAt(anchor)) {
                        names.add(player.getGameProfile().getName());
                    }
                }
            }
            return names;
        }
    }

    /** Atmospheric Portal Frame: the portal it belongs to. */
    public static final class PortalFrame extends AtBlock {
        public PortalFrame(Level level, BlockPos pos) {
            super(level, pos, "portal_frame");
        }

        /**
         * nil if this block is not part of a closed frame; otherwise {lit, width, height, axis}, with the interior size in
         * blocks and the axis ("x" or "z") its width runs along.
         */
        @Nullable
        @LuaFunction(mainThread = true)
        public final Map<String, Object> getPortal() {
            Optional<PortalShape.Rect> rect = AtmosphericPortals.findFrame(level, pos);
            if (rect.isEmpty()) {
                return null;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("lit", level.getBlockState(rect.get().at(0, 0)).is(ModBlocks.ATMOSPHERIC_PORTAL.get()));
            out.put("width", rect.get().width());
            out.put("height", rect.get().height());
            out.put("axis", rect.get().axis().getName());
            return out;
        }
    }
}
