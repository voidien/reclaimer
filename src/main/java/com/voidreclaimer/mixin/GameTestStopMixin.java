package com.voidreclaimer.mixin;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Test infrastructure only: a stopping server ticks its chunk maps "until they have no work", and a chunk map has work
 * while it has tickets. Sable keeps tickets on its ships' plot chunks for as long as the ships exist, so a GameTest
 * server with a ship in its world never leaves that loop: the JVM spins at full CPU after the summary, keeps the world's
 * lock, and the next run fails to start. For the GameTest server the loop is given a bounded number of turns, after
 * which the server saves and exits as it would have. Normal servers are untouched.
 */
@Mixin(MinecraftServer.class)
abstract class GameTestStopMixin {
    @Unique
    private static final Logger void_reclaimer$LOGGER = LogUtils.getLogger();
    /** Turns of the stop loop (one short wait each) before a GameTest server stops waiting for chunk work. */
    @Unique
    private static final int void_reclaimer$STOP_TURNS = 400;
    @Unique
    private int void_reclaimer$stopTurns;

    @Redirect(
            method = "stopServer",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
    private boolean void_reclaimer$boundedChunkWork(Stream<ServerLevel> levels, Predicate<ServerLevel> hasWork) {
        boolean busy = levels.anyMatch(hasWork);
        if (busy && (Object) this instanceof GameTestServer && ++void_reclaimer$stopTurns > void_reclaimer$STOP_TURNS) {
            void_reclaimer$LOGGER.warn("Chunk work never finished while stopping (ship plots keep tickets); saving and exiting anyway");
            return false;
        }
        return busy;
    }
}
