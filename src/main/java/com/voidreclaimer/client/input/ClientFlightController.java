package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.network.ThrustInputPayload;
import com.voidreclaimer.network.ToggleFlightSettingPayload;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client half of thruster flight. While thrusters are active, the movement keys are captured as thrust input
 * (forward/back, strafe, Space = up, Shift = down) and cleared from vanilla's input, so the player doesn't walk,
 * jump, crouch or toggle creative flight. Changes are sent to the server. The physics itself runs in
 * {@link ThrusterFlight#travel}, called from the travel mixin.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientFlightController {
    /** Closer to the view than this is pointing at it, in degrees. */
    private static final float HEADING_SAME = 0.05f;
    /** A hit this tick, waiting for the tick to end. */
    private static net.minecraft.world.phys.Vec3 crash;
    private static final int HEADING_AGAIN_TICKS = 20;
    /** The last heading the server was told, or null if it thinks the body points at its view. */
    private static com.voidreclaimer.body.BodyHeading sentHeading;

    private ClientFlightController() {}

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        Input input = event.getInput();
        // A walking body (the Mech) fires its thrusters when Space is pressed, and drops back to walking when it
        // is on the ground with no up-thrust held.
        boolean walker = ThrusterFlight.canEngage(player);
        FlightInput stored = player.getData(ModAttachments.FLIGHT_INPUT);
        if (walker && !stored.engaged() && input.jumping) {
            stored = stored.withEngaged(true);
            player.setData(ModAttachments.FLIGHT_INPUT, stored);
        }
        if (!ThrusterFlight.isActive(player)) {
            setInput(player, FlightInput.NONE);
            return;
        }
        boolean engaged = walker && !FlightInput.shouldDisengage(player.onGround(), input.jumping);
        setInput(player, FlightInput.of(input.up, input.down, input.left, input.right, input.jumping, input.shiftKeyDown)
                .withEngaged(engaged));

        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        player.setSprinting(false);
    }

    /** Installed as {@link ThrusterFlight#crashed}. It waits for the end of the tick: the move that reached the wall goes to the server first. */
    public static void onCrash(net.minecraft.world.phys.Vec3 lost) {
        crash = lost;
    }

    private static void reportCrash() {
        if (crash != null) {
            PacketDistributor.sendToServer(com.voidreclaimer.network.BodyCrashPayload.of(crash));
            crash = null;
        }
    }

    /** Tells the server where a big body points while that isn't its view, once for each change and once more when it catches up. */
    private static void reportHeading(LocalPlayer player) {
        com.voidreclaimer.body.BodyHeading heading = com.voidreclaimer.body.BodyTurning.of(player);
        boolean lagging = Math.abs(net.minecraft.util.Mth.wrapDegrees(heading.yaw() - player.getYRot())) > HEADING_SAME
                || Math.abs(heading.pitch() - player.getXRot()) > HEADING_SAME;
        // Once a second regardless: the server may have held a word back, and a body stuck against a wall has no change to report.
        if (lagging ? !heading.equals(sentHeading) || player.tickCount % HEADING_AGAIN_TICKS == 0 : sentHeading != null) {
            PacketDistributor.sendToServer(new com.voidreclaimer.network.BodyHeadingPayload(lagging, heading.yaw(), heading.pitch()));
            sentHeading = lagging ? heading : null;
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        reportHeading(mc.player);
        reportCrash();
        // Vanilla's "Move with WASD" tutorial waits for walking input, which thruster flight replaces, so it would
        // never finish. Its later steps (punch a tree, craft planks) don't fit the mod either: end it.
        if (mc.options.tutorialStep != TutorialSteps.NONE && ThrusterFlight.isActive(mc.player)) {
            mc.getTutorial().setStep(TutorialSteps.NONE);
        }
        while (ModKeyMappings.TOGGLE_RCS_LOCK.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFlightSettingPayload(ToggleFlightSettingPayload.Setting.RCS_LOCK));
        }
        while (ModKeyMappings.TOGGLE_AIM_STEERING.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFlightSettingPayload(ToggleFlightSettingPayload.Setting.AIM_STEERING));
        }
        while (ModKeyMappings.TOGGLE_DAMPENER.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFlightSettingPayload(ToggleFlightSettingPayload.Setting.DAMPENER));
        }
        while (ModKeyMappings.TOGGLE_CREATIVE_THRUSTERS.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFlightSettingPayload(ToggleFlightSettingPayload.Setting.CREATIVE_THRUSTERS));
        }
    }

    private static void setInput(LocalPlayer player, FlightInput input) {
        if (!player.getData(ModAttachments.FLIGHT_INPUT).equals(input)) {
            player.setData(ModAttachments.FLIGHT_INPUT, input);
            PacketDistributor.sendToServer(new ThrustInputPayload(input));
        }
    }
}
