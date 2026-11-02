package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One place on a {@link Chassis} where a section can go.
 *
 * @param id       unique within the chassis; also the GeckoLib bone name the section renders on (Phase 5b)
 * @param accepts  socket types a section may have to fit here ({@link SectionStats#socketType()}); several allow
 *                 variants and cross-fitting (e.g. a ship hardpoint that also takes a Mech hand)
 * @param required the body can't be without it (only the chassis's core socket)
 * @param parent   the socket this one hangs off (the joint tree); empty for the root
 * @param pivot    joint position in model pixels, relative to the parent's pivot
 * @param area     group for the Body tab's filters ("arms", "legs", "propulsion", ...)
 * @param mirror   draw the section flipped left to right, for the far side of a pair built from one lopsided model
 * @param boxes    the small boxes the section's part of a big body is made of; none for a body that is only its vanilla box
 */
public record Socket(String id, List<ResourceLocation> accepts, boolean required, Optional<String> parent, Vec3 pivot,
                     String area, boolean mirror, List<HitBox> boxes) {
    public static final Codec<Socket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Socket::id),
            ResourceLocation.CODEC.listOf().fieldOf("accepts").forGetter(Socket::accepts),
            Codec.BOOL.optionalFieldOf("required", false).forGetter(Socket::required),
            Codec.STRING.optionalFieldOf("parent").forGetter(Socket::parent),
            Vec3.CODEC.optionalFieldOf("pivot", Vec3.ZERO).forGetter(Socket::pivot),
            Codec.STRING.optionalFieldOf("area", "body").forGetter(Socket::area),
            Codec.BOOL.optionalFieldOf("mirror", false).forGetter(Socket::mirror),
            HitBox.CODEC.listOf().optionalFieldOf("boxes", List.of()).forGetter(Socket::boxes)
    ).apply(instance, Socket::new));

    public Socket {
        boxes = List.copyOf(boxes);
    }

    /** A socket drawn the normal way round; the constructor addons have used since the first API. */
    public Socket(String id, List<ResourceLocation> accepts, boolean required, Optional<String> parent, Vec3 pivot, String area) {
        this(id, accepts, required, parent, pivot, area, false);
    }

    /** The same with no boxes; the constructor addons have used since mirroring. */
    public Socket(String id, List<ResourceLocation> accepts, boolean required, Optional<String> parent, Vec3 pivot, String area, boolean mirror) {
        this(id, accepts, required, parent, pivot, area, mirror, List.of());
    }

    /** This socket, drawn flipped left to right. */
    public Socket mirrored() {
        return new Socket(id, accepts, required, parent, pivot, area, true, boxes);
    }

    /** This socket with these boxes. */
    public Socket withBoxes(HitBox... list) {
        return new Socket(id, accepts, required, parent, pivot, area, mirror, List.of(list));
    }

    public boolean accepts(ResourceLocation socketType) {
        return accepts.contains(socketType);
    }
}
