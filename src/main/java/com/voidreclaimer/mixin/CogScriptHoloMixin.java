package com.voidreclaimer.mixin;

import com.voidreclaimer.compat.act.HoloScript;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.blockentity.ComputerBlockEntity;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.ComputerOS;
import de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer.script.Interpreter;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives every CogScript program the {@code holo} module. CogScript has no extension point, so this is a patch: see
 * {@code docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md}. {@code require = 0} so an addon update that moves the method costs
 * programs the module, not the game its start; {@code theCogScriptMixinInstallsTheModule} is what notices.
 */
@Mixin(value = ComputerOS.class, remap = false)
public abstract class CogScriptHoloMixin {
    @Shadow
    @Final
    private ComputerBlockEntity computer;

    @Shadow
    abstract <T> T onMainCall(Supplier<T> work);

    @Inject(method = "installApi", at = @At("TAIL"), require = 0)
    private void voidReclaimer$installHolo(Interpreter interpreter, List<String> programArgs, CallbackInfo ci) {
        HoloScript.install(interpreter, computer, this::voidReclaimer$onServer);
    }

    /** {@code onMainCall} queues for the computer's next tick and waits, which would hang a caller on that very thread. */
    private Object voidReclaimer$onServer(Supplier<Object> work) {
        MinecraftServer server = computer.getLevel() == null ? null : computer.getLevel().getServer();
        return server != null && server.isSameThread() ? work.get() : onMainCall(work);
    }
}
