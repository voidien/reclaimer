base='src/main/java/com/voidreclaimer/'
def sub(path, pairs):
    s=open(path).read()
    for a,b in pairs:
        assert a in s, a
        s=s.replace(a,b)
    open(path,'w').write(s)
sub(base+'Config.java',[("    public static final ModConfigSpec.IntValue HOLO_SOURCE_RANGE;\n","    public static final ModConfigSpec.IntValue HOLO_SOURCE_RANGE;\n    public static final ModConfigSpec.IntValue HOLO_MAP_RADIUS;\n    public static final ModConfigSpec.IntValue HOLO_MAP_REFRESH_TICKS;\n"),
('''.defineInRange("sourceRange", 8, 1, 32);\n''','''.defineInRange("sourceRange", 8, 1, 32);\n        HOLO_MAP_RADIUS = BUILDER.comment("Blocks from the middle of a map hologram to its edge.").defineInRange("mapRadius", 64, 16, 256);\n        HOLO_MAP_REFRESH_TICKS = BUILDER.comment("Ticks between new pictures of a map hologram.").defineInRange("mapRefreshTicks", 100, 20, 1200);\n''')])
sub(base+'display/HoloDisplay.java',[("TEXT, READOUT, MODEL, BODY, GLOBE, SHIP;","TEXT, READOUT, MODEL, BODY, GLOBE, SHIP, MAP;"),("""    /** Showing one of {@link HoloGlobes#NAMES} from now on. */""","""    /** Showing a top-down map of the blocks around the projector from now on. */
    public HoloDisplay withMap() {
        return new HoloDisplay(List.of(), color, Mode.MAP, source, scale, height, billboard, "", progress);
    }

    /** Showing one of {@link HoloGlobes#NAMES} from now on. */""")])
sub(base+'display/HoloProjectorBlockEntity.java',[("""    /** Shows a globe and fills""","""    /** Shows a map of the blocks around the projector and draws its first picture at once. */
    public void setMap() {
        apply(display.withMap());
        refreshMap();
    }

    /** The newest map picture: empty before the first. See {@link HoloMapImage}. */
    public byte[] mapImage() {
        return mapImage;
    }

    /** Blocks from the middle of the map to its edge. */
    public int mapRadius() {
        return mapRadius;
    }

    /** Takes a new picture of the blocks around; sends it only if it differs. */
    public void refreshMap() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int radius = Config.HOLO_MAP_RADIUS.get();
        byte[] image = HoloMapImage.compose((x, y) -> mapPixel(level, worldPosition.getX() + HoloMapImage.offset(x, radius), worldPosition.getZ() + HoloMapImage.offset(y, radius)));
        if (radius != mapRadius || !Arrays.equals(image, mapImage)) {
            mapImage = image;
            mapRadius = radius;
            syncs++;
            ((ServerLevel) level).sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The top block of a column as a packed map colour; 0 for nothing there or a chunk that is not loaded. */
    private static int mapPixel(Level level, int x, int z) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return 0;
        }
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        BlockPos pos = new BlockPos(x, top, z);
        BlockState state = chunk.getBlockState(pos);
        return state.isAir() ? 0 : state.getMapColor(level, pos).getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
    }

    /** Shows a globe and fills"""),("""    private int revision;
""","""    private int revision;
    /** The newest map picture. Sent to clients, not saved: the blocks are there to draw it again. */
    private byte[] mapImage = new byte[0];
    private int mapRadius;
"""),("""        if ((level.getGameTime() + pos.asLong()) % Config.HOLO_REFRESH_TICKS.get() == 0) {
            projector.refresh();
        }
""","""        long tick = level.getGameTime() + pos.asLong();
        if (tick % Config.HOLO_REFRESH_TICKS.get() == 0) {
            projector.refresh();
        }
        if (projector.display.mode() == HoloDisplay.Mode.MAP && tick % Config.HOLO_MAP_REFRESH_TICKS.get() == 0) {
            projector.refreshMap();
        }
"""),("""        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
""","""        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        if (tag.contains("map")) {
            mapImage = tag.getByteArray("map");
            mapRadius = tag.getInt("mapRadius");
        }
"""),("""    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
""","""    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        if (display.mode() == HoloDisplay.Mode.MAP) {
            tag.putByteArray("map", mapImage);
            tag.putInt("mapRadius", mapRadius);
        }
        return tag;
"""),("""import java.util.ArrayList;
""","""import java.util.ArrayList;
import java.util.Arrays;
"""),("""import net.minecraft.world.level.block.state.BlockState;
""","""import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
""")])
sub(base+'client/render/holo/HoloProjectorRenderer.java',[("""            case SHIP -> drawShip(projector, display, time, flicker, pose, buffers);
""","""            case SHIP -> drawShip(projector, display, time, flicker, pose, buffers);
            case MAP -> drawMap(projector, display, time, flicker, pose, buffers);
"""),("""    private void drawGlobe(HoloProjectorBlockEntity""","""    private void drawMap(HoloProjectorBlockEntity projector, HoloDisplay display, float time, float flicker, PoseStack pose, MultiBufferSource buffers) {
        if (projector.mapImage().length == 0) {
            return;
        }
        float size = HoloTransform.MODEL_SIZE * display.scale();
        float pixel = size / HoloMapImage.SIZE;
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + size / 2, 0);
        pose.mulPose(turn(projector, display, pose));
        pose.scale(pixel, -pixel, pixel);
        pose.translate(-HoloMapImage.SIZE / 2f, -HoloMapImage.SIZE / 2f, 0);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer map = buffers.getBuffer(RenderType.text(HoloMaps.texture(projector)));
        int color = HoloStyle.argb(0xFFFFFF, HoloStyle.MODEL_ALPHA * flicker);
        float side = HoloMapImage.SIZE;
        map.addVertex(matrix, 0, side, 0).setColor(color).setUv(0, 1).setLight(LightTexture.FULL_BRIGHT);
        map.addVertex(matrix, side, side, 0).setColor(color).setUv(1, 1).setLight(LightTexture.FULL_BRIGHT);
        map.addVertex(matrix, side, 0, 0).setColor(color).setUv(1, 0).setLight(LightTexture.FULL_BRIGHT);
        map.addVertex(matrix, 0, 0, 0).setColor(color).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.GLOW, false));
        BlockPos center = projector.getBlockPos();
        int radius = projector.mapRadius();
        for (Player player : projector.getLevel().players()) {
            dot(glow, pose, matrix, player.getX() - center.getX(), player.getZ() - center.getZ(), radius, HoloStyle.argb(0xFFFFFF, flicker));
        }
        SubLevelContainer ships = SubLevelContainer.getContainer(projector.getLevel());
        if (ships != null) {
            for (SubLevel ship : ships.getAllSubLevels()) {
                var at = ship.logicalPose().position();
                dot(glow, pose, matrix, at.x() - center.getX(), at.z() - center.getZ(), radius, HoloStyle.argb(0xFFC860, flicker));
            }
        }
        pose.popPose();
    }

    /** A dot on the map at a block offset from its middle; nothing off the map. */
    private static void dot(VertexConsumer glow, PoseStack pose, Matrix4f matrix, double x, double z, int radius, int color) {
        int px = HoloMapImage.pixel(x, radius);
        int py = HoloMapImage.pixel(z, radius);
        if (px < 0 || py < 0) {
            return;
        }
        vertex(glow, pose, matrix, px - DOT, py + DOT, DOT_LIFT, 0, 1, color);
        vertex(glow, pose, matrix, px + DOT, py + DOT, DOT_LIFT, 1, 1, color);
        vertex(glow, pose, matrix, px + DOT, py - DOT, DOT_LIFT, 1, 0, color);
        vertex(glow, pose, matrix, px - DOT, py - DOT, DOT_LIFT, 0, 0, color);
    }

    private void drawGlobe(HoloProjectorBlockEntity"""),("""    private static final float CAPTION_GAP = 0.15f;
""","""    private static final float CAPTION_GAP = 0.15f;
    /** Half a dot on a map, and how far it stands in front of it, in map pixels. */
    private static final float DOT = 1.5f;
    private static final float DOT_LIFT = -0.2f;
"""),("""import com.voidreclaimer.display.HoloGlobes;
""","""import com.voidreclaimer.display.HoloGlobes;
import com.voidreclaimer.display.HoloMapImage;
"""),("""import dev.ryanhcode.sable.sublevel.SubLevel;
""","""import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
"""),("""import net.minecraft.resources.ResourceLocation;
""","""import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
""")])
sub(base+'command/HoloCommand.java',[("""                .then(Commands.literal("clear").executes(ctx -> {""","""                .then(Commands.literal("map").executes(ctx -> {
                    projector(ctx).setMap();
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("clear").executes(ctx -> {"""),("model|body|globe|ship|clear}","model|body|globe|ship|map|clear}")])
sub(base+'compat/cc/HoloPeripheral.java',[("""    /** The model shown, or nil when it is showing something else. */""","""    /** Shows a top-down map of the blocks around the projector, with dust, players and ships. */
    @LuaFunction(mainThread = true)
    public final void setMap(HoloProjectorBlockEntity holo) {
        holo.setMap();
    }

    /** The model shown, or nil when it is showing something else. */"""),("""{@code "globe"} or {@code "ship"}.""","""{@code "globe"}, {@code "ship"} or {@code "map"}.""")])
sub(base+'compat/act/HoloScript.java',[("""                .fn("get", write(""","""                .fn("map", write(computer, main, (projector, args) -> {
                    projector.setMap();
                    return true;
                }))
                .fn("get", write(""")])
