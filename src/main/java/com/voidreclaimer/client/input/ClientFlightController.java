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

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // Vanilla's "Move with WASD" tutorial waits for walking input, which thruster flight replaces, so it would
        // never finish. Its later steps (punch a tree, craft planks) don't fit the mod either: end it.
        if (mc.options.tutorialStep != TutorialSteps.NONE && ThrusterFlight.isActive(mc.player)) {
            mc.getTutorial().setStep(TutorialSteps.NONE);
        }
        while (ModKeyMappings.TOGGLE_RCS_LOCK.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFlightSettingPayload(ToggleFlightSettingPayload.Setting.RCS_LOCK));
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
