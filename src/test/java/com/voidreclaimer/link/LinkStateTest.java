package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class LinkStateTest {
    private static final LinkState.Place PLACE = new LinkState.Place(Level.END, new Vec3(1.5, 80, -2.5), 90f, -10f);
    private static final UUID SHELL = new UUID(1L, 2L);

    private static CompoundTag body(int marker) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("marker", marker);
        return tag;
    }

    private static LinkState throughASave(LinkState state) {
        Tag saved = LinkState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        return LinkState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    }

    @Test
    void nobodyIsLinkedAtFirst() {
        assertFalse(LinkState.NONE.linked());
        assertEquals(LinkState.NONE, throughASave(LinkState.NONE));
    }

    @Test
    void aLinkAndItsMainBodyComeBackFromASave() {
        LinkState state = LinkState.NONE.withLink(new LinkState.Link(SHELL, 7L, body(1), PLACE));
        assertTrue(state.linked());
        assertEquals(state, throughASave(state));
    }

    @Test
    void outboxesComeBackFromASaveBesideALink() {
        LinkState state = LinkState.NONE
                .withLink(new LinkState.Link(SHELL, 7L, body(1), PLACE))
                .withOutbox(new LinkState.Outbox(new UUID(3L, 4L), 2L, body(2), PLACE, false))
                .withOutbox(new LinkState.Outbox(new UUID(5L, 6L), 1L, body(3), PLACE, false));
        assertEquals(state, throughASave(state));
    }

    @Test
    void endingALinkLeavesTheOutboxesAndTheOtherWayRound() {
        LinkState.Link link = new LinkState.Link(SHELL, 7L, body(1), PLACE);
        LinkState.Outbox outbox = new LinkState.Outbox(SHELL, 8L, body(2), PLACE, false);
        LinkState both = LinkState.NONE.withLink(link).withOutbox(outbox);
        assertEquals(Optional.of(outbox), both.withoutLink().outbox(SHELL));
        assertEquals(Optional.of(link), both.withoutOutbox(SHELL).link());
    }

    /** Waking two shells before either reaches the disk must not lose the first. */
    @Test
    void eachShellHasItsOwnOutbox() {
        UUID other = new UUID(3L, 4L);
        LinkState state = LinkState.NONE
                .withOutbox(new LinkState.Outbox(SHELL, 1L, body(1), PLACE, false))
                .withOutbox(new LinkState.Outbox(other, 1L, body(2), PLACE, false));
        assertEquals(2, state.outboxes().size());
        assertTrue(state.withoutOutbox(SHELL).outbox(SHELL).isEmpty());
        assertTrue(state.withoutOutbox(SHELL).outbox(other).isPresent());
    }

    @Test
    void aNewerCopyOfAShellReplacesTheOlder() {
        LinkState state = LinkState.NONE
                .withOutbox(new LinkState.Outbox(SHELL, 1L, body(1), PLACE, false))
                .withOutbox(new LinkState.Outbox(SHELL, 2L, body(2), PLACE, false));
        assertEquals(1, state.outboxes().size());
        assertEquals(2L, state.outbox(SHELL).orElseThrow().epoch());
    }

    @Test
    void aSaveFromBeforeThisExistedReadsAsNotLinked() {
        assertEquals(LinkState.NONE, LinkState.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
    }
}
