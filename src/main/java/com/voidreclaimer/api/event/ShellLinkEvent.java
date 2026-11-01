package com.voidreclaimer.api.event;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Remote control, on the game bus and the server side: a player linking to a shell, and coming back from one. */
public abstract class ShellLinkEvent extends Event {
    private final ServerPlayer player;
    private final UUID shell;

    protected ShellLinkEvent(ServerPlayer player, UUID shell) {
        this.player = player;
        this.shell = shell;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /** The shell's id, the same through every link. */
    public UUID getShell() {
        return shell;
    }

    /** Before anything changes. Cancel to refuse the link: nothing moves, and the link ends {@code CANCELLED}. */
    public static class Pre extends ShellLinkEvent implements ICancellableEvent {
        public Pre(ServerPlayer player, UUID shell) {
            super(player, shell);
        }
    }

    /** After the player is in the shell and that is saved. */
    public static class Started extends ShellLinkEvent {
        public Started(ServerPlayer player, UUID shell) {
            super(player, shell);
        }
    }

    /** After the player is back in the main body and that is saved. It cannot be cancelled: a player must always be able to come home. */
    public static class Ended extends ShellLinkEvent {
        private final Reason reason;

        public Ended(ServerPlayer player, UUID shell, Reason reason) {
            super(player, shell);
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }

    /** Why a link ended. */
    public enum Reason {
        /** The player chose to, or a command did. */
        LEFT,
        /** The seat's signal stopped reaching the shell. */
        SIGNAL_LOST,
        /** The shell was destroyed around the player, and is a wreck now. */
        BODY_LOST
    }
}
