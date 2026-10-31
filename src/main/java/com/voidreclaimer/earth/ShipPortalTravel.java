package com.voidreclaimer.earth;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.ship.HelmBlockEntity;
import com.voidreclaimer.ship.HelmSeat;
import com.voidreclaimer.ship.Ships;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import com.voidreclaimer.station.StationAssembly;
import com.voidreclaimer.station.StationMath;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Ships through the Atmospheric Portal. Sable cannot move a ship between dimensions, so a ship that flies into a portal
 * pane is rebuilt on the far side: its blocks (with their block entities) are copied into the world just beyond the
 * matching gate, turned so it comes out the way it went in, assembled with Sable's own assembler, then turned to its
 * exact orientation and given its velocity back; the seated pilot and anyone standing on it are brought along to their
 * spots. A ship crosses only if its turned bounds fit the gate's opening and the far side is clear; otherwise it bumps
 * and the crew is told why. The old ship is removed once the new one stands.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ShipPortalTravel {
    /** Ticks between looks at ships near portals. */
    static final int LOOK_TICKS = 2;
    /** How far (blocks) from the pane's plane a ship counts as at the portal: before it can hit the frame round the opening. */
    static final double REACH = 1.0;
    /** A ship moves at least this fast (blocks per second) through the plane to cross. */
    static final double MIN_SPEED = 0.05;
    /** After crossing or bumping, a ship is left alone for this long (ticks). */
    static final int COOLDOWN_TICKS = 100;
    /** Blocks of clear space between the gate and the arriving ship's nearest face. */
    static final double CLEARANCE = 2.0;
    static final String COOLDOWN_TAG = "void_reclaimer_portal_cooldown";
    /** Ticks after assembly before the arrived ship's body can be turned and pushed. */
    static final int SETTLE_TICKS = 2;

    private ShipPortalTravel() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % LOOK_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null || container.getAllSubLevels().isEmpty()) {
                continue;
            }
            List<PortalShape.Rect> rects = rects(level);
            if (rects.isEmpty()) {
                continue;
            }
            for (ServerSubLevel ship : new ArrayList<>(container.getAllSubLevels())) {
                if (ship.isRemoved() || coolingDown(ship, level.getGameTime())) {
                    continue;
                }
                for (PortalShape.Rect rect : rects) {
                    int sign = approaching(ship, rect);
                    if (sign != 0) {
                        cross(level, ship, rect, sign);
                        break;
                    }
                }
            }
        }
    }

    /**
     * The distinct portal rectangles of the level: its registered panes, plus Earth's own gate from the portal links
     * (its panes may date from before panes were registered), as long as it is still lit.
     */
    static List<PortalShape.Rect> rects(ServerLevel level) {
        List<PortalShape.Rect> rects = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        PortalPanes panes = level.getExistingDataOrNull(ModAttachments.PORTAL_PANES);
        if (panes != null) {
            for (BlockPos pane : panes.panes()) {
                Optional<PortalShape.Rect> rect = AtmosphericPortals.rectAround(level, pane);
                if (rect.isPresent() && seen.add(rect.get().bottomLeft())) {
                    rects.add(rect.get());
                }
            }
        }
        if (RuinedEarth.is(level)) {
            PortalLinks links = level.getExistingDataOrNull(ModAttachments.PORTAL_LINKS);
            if (links != null && links.gate().isPresent()) {
                PortalShape.Rect gate = links.gate().get().rect();
                if (seen.add(gate.bottomLeft()) && level.getBlockState(gate.at(gate.width() / 2, gate.height() / 2)).is(com.voidreclaimer.registry.ModBlocks.ATMOSPHERIC_PORTAL.get())) {
                    rects.add(gate);
                }
            }
        }
        return rects;
    }

    /**
     * The side ({@code +1} south/east, {@code -1} north/west) the ship is about to pass to through {@code rect}, or 0
     * when it is not at the plane or not moving through it.
     */
    static int approaching(ServerSubLevel ship, PortalShape.Rect rect) {
        BoundingBox3dc bounds = ship.boundingBox();
        if (bounds == null) {
            return 0;
        }
        Vec3 base = rect.centerBottom();
        boolean acrossX = rect.axis() == Direction.Axis.X;
        double planeAt = acrossX ? base.z : base.x;
        double half = rect.width() / 2.0;
        double acrossMin = (acrossX ? base.x : base.z) - half, acrossMax = (acrossX ? base.x : base.z) + half;
        double shipAcrossMin = acrossX ? bounds.minX() : bounds.minZ(), shipAcrossMax = acrossX ? bounds.maxX() : bounds.maxZ();
        double shipNormalMin = acrossX ? bounds.minZ() : bounds.minX(), shipNormalMax = acrossX ? bounds.maxZ() : bounds.maxX();
        boolean overlaps = shipAcrossMax > acrossMin && shipAcrossMin < acrossMax
                && bounds.maxY() > base.y && bounds.minY() < base.y + rect.height()
                && shipNormalMax > planeAt - REACH && shipNormalMin < planeAt + REACH;
        if (!overlaps) {
            return 0;
        }
        RigidBodyHandle body = RigidBodyHandle.of(ship);
        if (body == null || !body.isValid()) {
            return 0;
        }
        Vector3d velocity = body.getLinearVelocity(new Vector3d());
        double through = acrossX ? velocity.z : velocity.x;
        if (Math.abs(through) < MIN_SPEED) {
            return 0;
        }
        // Still on the side it is leaving: its centre has not passed the plane yet.
        double centre = acrossX ? ship.logicalPose().position().z() : ship.logicalPose().position().x();
        if (through > 0 ? centre > planeAt + 0.5 : centre < planeAt - 0.5) {
            return 0;
        }
        return through > 0 ? 1 : -1;
    }

    /** Takes {@code ship} through {@code rect} to the other dimension, or bumps it. */
    static void cross(ServerLevel from, ServerSubLevel ship, PortalShape.Rect rect, int sign) {
        MinecraftServer server = from.getServer();
        Direction leaving = rect.normal(sign);
        List<ServerPlayer> crew = crewOf(from, ship);
        long now = from.getGameTime();
        // Where to: Earth's gate from anywhere else, or back out through the remembered exit.
        ServerLevel to;
        Direction arriving;
        Vec3 gateBase;
        PortalShape.Rect gate;
        if (RuinedEarth.is(from)) {
            Optional<PortalLinks.Exit> exit = from.getData(ModAttachments.PORTAL_LINKS).exit();
            to = exit.map(PortalLinks.Exit::dimension).map(server::getLevel).orElse(server.getLevel(ModDimensions.DEEP_SPACE));
            if (to == null) {
                bump(ship, crew, now, "message.void_reclaimer.ship_portal.no_way_back");
                return;
            }
            arriving = exit.map(PortalLinks.Exit::side).orElse(Direction.SOUTH);
            gateBase = exit.map(PortalLinks.Exit::base).orElse(SpawnHandler.spawnPosition());
            gate = null;
        } else {
            to = server.getLevel(RuinedEarth.DIMENSION);
            if (to == null) {
                bump(ship, crew, now, "message.void_reclaimer.ship_portal.no_way_back");
                return;
            }
            Vec3 extents = ShipGate.worldExtents(ship.getPlot().getBoundingBox(), ship.logicalPose());
            if (!ShipGate.fits(extents, rect.axis(), rect.width(), rect.height())) {
                bump(ship, crew, now, "message.void_reclaimer.ship_portal.too_big");
                return;
            }
            int width = Math.min(PortalShape.MAX_SIZE, Math.max(PortalTravel.MIN_GATE_WIDTH, rect.width()));
            int height = Math.min(PortalShape.MAX_SIZE, Math.max(PortalTravel.MIN_GATE_HEIGHT, rect.height()));
            gate = PortalTravel.ensureGate(to, width, height);
            to.setData(ModAttachments.PORTAL_LINKS, to.getData(ModAttachments.PORTAL_LINKS).withExit(
                    new PortalLinks.Exit(from.dimension(), rect.centerBottom(), leaving)));
            arriving = Direction.SOUTH;
            gateBase = gate.centerBottom();
        }
        int delta = ShipGate.turnsBetween(leaving, arriving);
        Quaterniond turn = ShipGate.turn(delta);
        // The ship's blocks, as they are and as they will lie: turned by the ship's own nearest quarter turn plus the
        // turn between the portals, around its plot centre, in the world beyond the far gate.
        List<BlockPos> blocks = StationAssembly.blocksOf(from, ship);
        if (blocks.isEmpty()) {
            return;
        }
        int turns = Math.floorMod(StationMath.quarterTurns(ship.logicalPose().orientation()) + delta, 4);
        BlockPos centreBlock = ship.getPlot().getCenterBlock();
        List<BlockPos> turned = new ArrayList<>(blocks.size());
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos block : blocks) {
            BlockPos rel = StationMath.rotateY(block.subtract(centreBlock), turns);
            turned.add(rel);
            minX = Math.min(minX, rel.getX()); maxX = Math.max(maxX, rel.getX());
            minY = Math.min(minY, rel.getY()); maxY = Math.max(maxY, rel.getY());
            minZ = Math.min(minZ, rel.getZ()); maxZ = Math.max(maxZ, rel.getZ());
        }
        // Centre the hull across the gate, lift it so its lowest block clears the gate's floor row, and set it
        // CLEARANCE blocks beyond the plane along the way it arrives.
        double depth = arriving.getAxis() == Direction.Axis.Z ? maxZ - minZ + 1 : maxX - minX + 1;
        double along = CLEARANCE + depth / 2.0;
        Vec3 centre = gateBase.add(arriving.getStepX() * along, 1.0 - minY, arriving.getStepZ() * along);
        BlockPos origin = BlockPos.containing(centre);
        List<BlockPos> landing = new ArrayList<>(turned.size());
        for (BlockPos rel : turned) {
            BlockPos where = origin.offset(rel);
            BlockState there = to.getBlockState(where);
            if (!there.isAir() && !there.canBeReplaced()) {
                bump(ship, crew, now, "message.void_reclaimer.ship_portal.blocked");
                return;
            }
            landing.add(where);
        }
        // Remember the ship, then take it apart here and build it there.
        List<BlockState> states = new ArrayList<>(blocks.size());
        List<CompoundTag> tags = new ArrayList<>(blocks.size());
        for (BlockPos block : blocks) {
            states.add(from.getBlockState(block));
            BlockEntity entity = from.getBlockEntity(block);
            tags.add(entity == null ? null : entity.saveWithoutMetadata(from.registryAccess()));
        }
        String name = ship.getName();
        CompoundTag userData = ship.getUserDataTag() == null ? null : ship.getUserDataTag().copy();
        RigidBodyHandle body = RigidBodyHandle.of(ship);
        Vector3d velocity = body == null ? new Vector3d() : body.getLinearVelocity(new Vector3d());
        Vector3d spin = body == null ? new Vector3d() : body.getAngularVelocity(new Vector3d());
        Quaterniond orientation = new Quaterniond(ship.logicalPose().orientation());
        List<Vec3> crewLocal = new ArrayList<>();
        List<BlockPos> crewHelm = new ArrayList<>();
        for (ServerPlayer player : crew) {
            crewLocal.add(ship.logicalPose().transformPositionInverse(player.position()));
            crewHelm.add(player.getVehicle() instanceof HelmSeat seat ? seat.helmPos() : null);
            if (player.getVehicle() != null) {
                player.stopRiding();
            }
        }
        SubLevelContainer.getContainer(from).removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
        net.minecraft.world.level.block.Rotation rotation = StationMath.blockRotation(turns);
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos where = landing.get(i);
            to.setBlock(where, states.get(i).rotate(rotation), 2);
            CompoundTag tag = tags.get(i);
            if (tag != null) {
                BlockEntity entity = BlockEntity.loadStatic(where, to.getBlockState(where), tag, to.registryAccess());
                if (entity != null) {
                    to.setBlockEntity(entity);
                }
            }
        }
        ServerSubLevel arrived = SubLevelAssemblyHelper.assembleBlocks(to, landing.get(0), landing, new BoundingBox3i(origin.offset(minX, minY, minZ), origin.offset(maxX, maxY, maxZ)));
        if (arrived == null) {
            VoidReclaimer.LOGGER.warn("A ship crossed the portal but could not be assembled on the far side at {}", origin);
            return;
        }
        if (name != null && !name.isBlank()) {
            arrived.setName(name);
        }
        CompoundTag data = userData == null ? new CompoundTag() : userData;
        data.putLong(COOLDOWN_TAG, to.getGameTime() + COOLDOWN_TICKS);
        arrived.setUserDataTag(data);
        // The crew, to their spots on the grid-aligned hull; then the exact orientation and the velocity.
        for (int i = 0; i < crew.size(); i++) {
            ServerPlayer player = crew.get(i);
            Vec3 local = crewLocal.get(i);
            BlockPos helmPlot = crewHelm.get(i);
            Vec3 relTurned = turnLocal(local.subtract(Vec3.atLowerCornerOf(centreBlock)), turns);
            Vec3 spot = Vec3.atLowerCornerOf(origin).add(relTurned);
            player.teleportTo(to, spot.x, spot.y, spot.z, player.getYRot() + delta * 90f, player.getXRot());
            ServerPlayer now2 = server.getPlayerList().getPlayer(player.getUUID());
            if (helmPlot != null && now2 != null) {
                BlockPos helmWorld = origin.offset(StationMath.rotateY(helmPlot.subtract(centreBlock), turns));
                HelmBlockEntity helm = helmNearest(to, arrived, Vec3.atCenterOf(helmWorld));
                if (helm != null) {
                    helm.seat(now2);
                }
            }
        }
        // The new body exists after Sable's next physics tick: give it its exact orientation and its velocity then.
        Quaterniond exact = new Quaterniond(turn).mul(orientation).mul(ShipGate.turn(-turns));
        Vector3d movingAt = turn.transform(new Vector3d(velocity));
        Vector3d spinningAt = turn.transform(new Vector3d(spin));
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + SETTLE_TICKS, () -> {
            RigidBodyHandle newBody = arrived.isRemoved() ? null : RigidBodyHandle.of(arrived);
            if (newBody != null && newBody.isValid()) {
                newBody.teleport(new Vector3d(arrived.logicalPose().position()), exact);
                newBody.addLinearAndAngularVelocity(movingAt, spinningAt);
            }
        }));
        for (ServerPlayer player : crew) {
            ServerPlayer now2 = server.getPlayerList().getPlayer(player.getUUID());
            if (now2 != null) {
                now2.displayClientMessage(Component.translatable("message.void_reclaimer.ship_portal.crossed").withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    /** The ship's Helm whose place in the world is nearest {@code world}: the block entities have moved into the plot. */
    @Nullable
    static HelmBlockEntity helmNearest(ServerLevel level, ServerSubLevel ship, Vec3 world) {
        HelmBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : StationAssembly.blocksOf(level, ship)) {
            if (level.getBlockEntity(pos) instanceof HelmBlockEntity helm) {
                double distance = ship.logicalPose().transformPosition(Vec3.atCenterOf(pos)).distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = helm;
                }
            }
        }
        return best;
    }

    /** A ship-local offset turned like the blocks are. */
    static Vec3 turnLocal(Vec3 offset, int quarterTurns) {
        return switch (Math.floorMod(quarterTurns, 4)) {
            case 1 -> new Vec3(offset.z, offset.y, -offset.x);
            case 2 -> new Vec3(-offset.x, offset.y, -offset.z);
            case 3 -> new Vec3(-offset.z, offset.y, offset.x);
            default -> offset;
        };
    }

    /** The players standing on or seated at {@code ship}. */
    static List<ServerPlayer> crewOf(ServerLevel level, ServerSubLevel ship) {
        List<ServerPlayer> crew = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            boolean seated = player.getVehicle() instanceof HelmSeat seat && Ships.shipAt(level, seat.helmPos()) == ship;
            boolean standing = Sable.HELPER.getTrackingOrVehicleSubLevel(player) == ship;
            if (seated || standing) {
                crew.add(player);
            }
        }
        return crew;
    }

    private static boolean coolingDown(ServerSubLevel ship, long now) {
        CompoundTag data = ship.getUserDataTag();
        return data != null && data.contains(COOLDOWN_TAG) && data.getLong(COOLDOWN_TAG) > now;
    }

    private static void bump(ServerSubLevel ship, List<ServerPlayer> crew, long now, String message) {
        CompoundTag data = ship.getUserDataTag() == null ? new CompoundTag() : ship.getUserDataTag();
        data.putLong(COOLDOWN_TAG, now + COOLDOWN_TICKS);
        ship.setUserDataTag(data);
        for (ServerPlayer player : crew) {
            player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.RED), true);
        }
    }

    @Nullable
    static ServerLevel earth(MinecraftServer server) {
        return server.getLevel(RuinedEarth.DIMENSION);
    }
}
