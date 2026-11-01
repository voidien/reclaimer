base='src/main/java/com/voidreclaimer/'
def sub(path, pairs):
    s=open(path).read()
    for a,b in pairs:
        assert a in s, a
        s=s.replace(a,b)
    open(path,'w').write(s)
sub(base+'display/HoloDisplay.java',[("TEXT, READOUT, MODEL, BODY, GLOBE;","TEXT, READOUT, MODEL, BODY, GLOBE, SHIP;"),("""    /** Showing one of {@link HoloGlobes#NAMES} from now on. */""","""    /** Showing a miniature of a ship from now on: the one the projector stands on, or the nearest. */
    public HoloDisplay withShip() {
        return new HoloDisplay(List.of(), color, Mode.SHIP, source, scale, height, billboard, "", progress);
    }

    /** Showing one of {@link HoloGlobes#NAMES} from now on. */""")])
sub(base+'display/HoloProjectorBlockEntity.java',[("""    /** Shows a globe and fills""","""    public void setShip() {
        apply(display.withShip());
    }

    /** Shows a globe and fills""")])
sub(base+'client/render/holo/HoloProjectorRenderer.java',[("""            case GLOBE -> drawGlobe(projector, display, time, flicker, pose, buffers);
""","""            case GLOBE -> drawGlobe(projector, display, time, flicker, pose, buffers);
            case SHIP -> drawShip(projector, display, time, flicker, pose, buffers);
"""),("""    private void drawGlobe(HoloProjectorBlockEntity""","""    private void drawShip(HoloProjectorBlockEntity projector, HoloDisplay display, float time, float flicker, PoseStack pose, MultiBufferSource buffers) {
        SubLevel ship = HoloShips.shipFor(projector.getLevel(), projector.getBlockPos());
        if (ship == null) {
            drawText(projector, display.withLines(List.of("NO SHIP")), time, flicker, pose, buffers);
            return;
        }
        HoloMesh mesh = HoloShips.mesh(ship, projector.getLevel().getGameTime());
        if (mesh == null || mesh.vertices() == 0) {
            return;
        }
        float size = HoloTransform.MODEL_SIZE * display.scale();
        HoloTransform.Fit fit = HoloTransform.fit(mesh.box(), size);
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + size / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(HoloTransform.spin(time)));
        pose.scale(fit.scale(), fit.scale(), fit.scale());
        pose.translate(-fit.centerX(), -fit.centerY(), -fit.centerZ());
        mesh.replay(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS, false)),
                HoloStyle.argb(HoloStyle.MODEL_TINT, HoloStyle.MODEL_ALPHA * flicker));
        pose.popPose();
    }

    private void drawGlobe(HoloProjectorBlockEntity"""),("""import net.minecraft.client.renderer.texture.OverlayTexture;
""","""import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
"""),("""import com.voidreclaimer.display.HoloProjectorBlockEntity;
""","""import com.voidreclaimer.display.HoloProjectorBlockEntity;
import dev.ryanhcode.sable.sublevel.SubLevel;
""")])
sub(base+'command/HoloCommand.java',[("""                .then(Commands.literal("clear").executes(ctx -> {""","""                .then(Commands.literal("ship").executes(ctx -> {
                    projector(ctx).setShip();
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("clear").executes(ctx -> {"""),("model|body|globe|clear}","model|body|globe|ship|clear}")])
sub(base+'compat/cc/HoloPeripheral.java',[("""    /** The model shown, or nil when it is showing something else. */""","""    /** Shows a miniature of the ship the projector stands on, or else the nearest ship. */
    @LuaFunction(mainThread = true)
    public final void setShip(HoloProjectorBlockEntity holo) {
        holo.setShip();
    }

    /** The model shown, or nil when it is showing something else. */"""),("""{@code "model"}, {@code "body"} or {@code "globe"}.""","""{@code "model"}, {@code "body"}, {@code "globe"} or {@code "ship"}.""")])
sub(base+'compat/act/HoloScript.java',[("""                .fn("get", write(""","""                .fn("ship", write(computer, main, (projector, args) -> {
                    projector.setShip();
                    return true;
                }))
                .fn("get", write(""")])
