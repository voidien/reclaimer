package com.voidreclaimer.debug;

import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyBoxes;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * Each big body's hit boxes, for photo mode's overlay: green when free, red when a box is inside a block, and a line for where the body
 * points. Read on the server. It shows the data before anything can stop a player, and it is how a wrong box gets found.
 */
public final class BoxOverlay {
    public static final int FREE = 0xFF50E050;
    public static final int TOUCHING = 0xFFFF4040;
    public static final int HEADING = 0xFFFFE060;
    /** How long the heading line is, in blocks at scale 1. */
    private static final double HEADING_LENGTH = 4.0;

    private BoxOverlay() {}

    /** Adds every body with boxes within {@code range} blocks of {@code around}. */
    public static void add(ServerLevel level, Vec3 around, double range, DebugShapes shapes, List<String> facts) {
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(range))) {
            body(level, body, shapes, facts);
        }
    }

    private static void body(ServerLevel level, LivingEntity entity, DebugShapes shapes, List<String> facts) {
        if (!entity.hasData(com.voidreclaimer.registry.ModAttachments.BODY)) {
            return;
        }
        BodyView view = Bodies.view(entity);
        BodyData data = view.data();
        double scale = view.stats().scale();
        List<BodyBoxes.Box> boxes = BodyBoxes.place(view.chassis(), socket -> !data.section(socket).isEmpty(), entity.position(), entity.getYRot(), entity.getXRot(), scale);
        if (boxes.isEmpty()) {
            return;
        }
        int touching = 0;
        for (BodyBoxes.Box box : boxes) {
            boolean inBlock = inBlock(level, box);
            touching += inBlock ? 1 : 0;
            shapes.box(BodyBoxes.corners(box), inBlock ? TOUCHING : FREE);
        }
        Vector3d forward = BodyBoxes.turn(entity.getYRot(), entity.getXRot()).transform(new Vector3d(0, 0, -HEADING_LENGTH * scale));
        shapes.arrow(entity.position(), new Vec3(forward.x, forward.y, forward.z), HEADING);
        facts.add(String.format(Locale.ROOT, "%s: %d boxes, %d in a block", entity.getName().getString(), boxes.size(), touching));
    }

    private static boolean inBlock(ServerLevel level, BodyBoxes.Box box) {
        List<Vec3> corners = BodyBoxes.corners(box);
        AABB around = new AABB(corners.get(0), corners.get(7));
        for (Vec3 corner : corners) {
            around = around.minmax(new AABB(corner, corner));
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(around.minX, around.minY, around.minZ), BlockPos.containing(around.maxX, around.maxY, around.maxZ))) {
            for (AABB shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                if (BodyBoxes.push(box, BodyBoxes.Box.of(shape.move(pos))).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }
}
