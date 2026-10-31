package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.network.PilotInputPayload;
import com.voidreclaimer.ship.HelmSeat;
import com.voidreclaimer.ship.PilotInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client half of the Helm. While the player sits in a {@link HelmSeat}, the movement keys become the six pilot axes
 * (W/S surge, A/D sway, Space/Shift heave, Q/E roll, arrows pitch and yaw) and are cleared from vanilla's input so the
 * rider does not try to walk off; changes are sent to the server, where the Helm commands the thrusters.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientPilotController {
    private static PilotInput last = PilotInput.NONE;
    private static volatile com.voidreclaimer.ship.HelmBlockEntity.Readout readout = com.voidreclaimer.ship.HelmBlockEntity.Readout.EMPTY;

    private ClientPilotController() {}

    public static boolean isPiloting(LocalPlayer player) {
        return player.getVehicle() instanceof HelmSeat;
    }

    /** The seat the player sits in, or null. */
    public static HelmSeat seat(LocalPlayer player) {
        return player.getVehicle() instanceof HelmSeat seat ? seat : null;
    }

    /** What the Helm last told the pilot about the ship (empty when not in a chair). */
    public static com.voidreclaimer.ship.HelmBlockEntity.Readout readout() {
        return readout;
    }

    /** Installed as {@link com.voidreclaimer.network.HelmReadoutPayload#clientHandler}. */
    public static void onReadout(com.voidreclaimer.network.HelmReadoutPayload payload) {
        readout = payload.readout();
    }

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (!isPiloting(player)) {
            send(player, PilotInput.NONE);
            readout = com.voidreclaimer.ship.HelmBlockEntity.Readout.EMPTY;
            return;
        }
        Input input = event.getInput();
        Minecraft mc = Minecraft.getInstance();
        PilotInput now = PilotInput.of(input.up, input.down, input.right, input.left, input.jumping, input.shiftKeyDown,
                ModKeyMappings.ROLL_RIGHT.isDown(), ModKeyMappings.ROLL_LEFT.isDown(),
                ModKeyMappings.PITCH_UP.isDown(), ModKeyMappings.PITCH_DOWN.isDown(),
                ModKeyMappings.YAW_LEFT.isDown(), ModKeyMappings.YAW_RIGHT.isDown());
        send(player, now);
        // The seat holds the pilot: none of this should walk, jump or dismount.
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = input.down = input.left = input.right = input.jumping = false;
        // Shift is heave down at the Helm; leaving the chair is the sneak key held for a moment, handled by vanilla's own
        // dismount, which we keep by letting shiftKeyDown through only when no pilot axis is used.
        if (now.heave() < 0 && (now.surge() != 0 || now.sway() != 0 || now.roll() != 0 || now.pitch() != 0 || now.yaw() != 0)) {
            input.shiftKeyDown = false;
        }
        player.setSprinting(false);
        if (mc.options.keySprint.isDown()) {
            mc.options.keySprint.setDown(false);
        }
    }

    private static void send(LocalPlayer player, PilotInput input) {
        if (!last.equals(input)) {
            last = input;
            PacketDistributor.sendToServer(new PilotInputPayload(input));
        }
    }
}
