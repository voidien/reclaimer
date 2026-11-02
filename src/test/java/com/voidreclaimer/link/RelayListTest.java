package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RelayListTest {
    private static final UUID FIRST = new UUID(1L, 1L);
    private static final UUID SECOND = new UUID(2L, 2L);
    private static final UUID SHIP = new UUID(9L, 9L);

    private static RelayList.Entry entry(UUID id, boolean lit) {
        return new RelayList.Entry(id, new BlockPos(1, 2, 3), new Vec3(1.5, 2.5, 3.5), lit, Optional.empty());
    }

    private static RelayList.Entry aboard(UUID id, UUID ship) {
        return new RelayList.Entry(id, new BlockPos(1, 2, 3), new Vec3(1.5, 2.5, 3.5), true, Optional.of(ship));
    }

    @Test
    void aNewRelayIsAdded() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true)).with(entry(SECOND, false));
        assertEquals(List.of(entry(FIRST, true), entry(SECOND, false)), list.relays());
    }

    @Test
    void aKnownRelayReplacesItsOwnRow() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true)).with(entry(SECOND, true)).with(entry(FIRST, false));
        assertEquals(List.of(entry(FIRST, false), entry(SECOND, true)), list.relays());
    }

    @Test
    void anUnchangedRowLeavesTheSameList() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true));
        assertSame(list, list.with(entry(FIRST, true)));
    }

    @Test
    void forgettingTakesOneRow() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true)).with(entry(SECOND, true));
        assertEquals(List.of(entry(SECOND, true)), list.without(FIRST).relays());
        assertSame(list, list.without(new UUID(3L, 3L)));
    }

    @Test
    void aShipTakesItsOwnRowsWithIt() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true)).with(aboard(SECOND, SHIP)).with(aboard(new UUID(3L, 3L), new UUID(8L, 8L)));
        assertEquals(List.of(FIRST, new UUID(3L, 3L)), list.withoutShip(SHIP).relays().stream().map(RelayList.Entry::id).toList());
        assertSame(list, list.withoutShip(new UUID(7L, 7L)));
    }

    @Test
    void aSavedListReadsBack() {
        RelayList list = RelayList.EMPTY.with(entry(FIRST, true)).with(aboard(SECOND, SHIP));
        Tag saved = RelayList.CODEC.encodeStart(NbtOps.INSTANCE, list).getOrThrow();
        assertEquals(list, RelayList.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
    }

    @Test
    void aBadRowCostsThatRowAlone() {
        CompoundTag saved = (CompoundTag) RelayList.CODEC.encodeStart(NbtOps.INSTANCE, RelayList.EMPTY.with(entry(FIRST, true))).getOrThrow();
        ListTag rows = saved.getList("relays", Tag.TAG_COMPOUND);
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "not an id");
        rows.add(0, broken);
        RelayList read = RelayList.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(List.of(entry(FIRST, true)), read.relays());
    }

    @Test
    void aSaveWithNoListReadsEmpty() {
        assertTrue(RelayList.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow().relays().isEmpty());
    }
}
