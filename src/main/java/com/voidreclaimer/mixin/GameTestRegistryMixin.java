package com.voidreclaimer.mixin;

import com.voidreclaimer.gametest.GameTestFilter;
import net.minecraft.gametest.framework.GameTestRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Test infrastructure only: with {@code -PgametestFilter=<terms>} (system property {@code void_reclaimer.gametestFilter})
 * the GameTest server registers only the tests whose class or method name matches, so one class or test runs in
 * seconds instead of all of them. Without the property everything runs but the tests that span a restart. It filters at registration because a test's
 * name does not contain its class name (our classes are not template-prefixed).
 */
@Mixin(GameTestRegistry.class)
abstract class GameTestRegistryMixin {
    @Inject(method = "register(Ljava/lang/reflect/Method;Ljava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private static void void_reclaimer$filter(Method testMethod, Set<String> allowedNamespaces, CallbackInfo ci) {
        String filter = System.getProperty(GameTestFilter.PROPERTY);
        if (!GameTestFilter.registers(filter, testMethod.getDeclaringClass().getSimpleName(), testMethod.getName())) {
            ci.cancel();
        }
    }
}
