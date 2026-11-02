package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModCodecs;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A body layout: which sockets a body has and how they hang together. The chassis's core section (Drone hull, Mech
 * torso, Ship keel) names its chassis; installing it sets the layout and the size class. Entries live in the synced
 * datapack registry {@code void_reclaimer:chassis}; the name defaults to {@code body.<ns>.chassis.<path>}.
 *
 * @param sizeClass  size class of bodies built on it
 * @param coreSocket id of the socket the core section sits in (must be one of {@code sockets})
 * @param sockets    every socket, in Body tab order
 * @param name       display name
 * @param cockpit    where the first-person camera sits and whether the vanilla hand is hidden (big ships); none keeps the player's eyes
 * @param walks      true for bodies with legs (the Mech): where gravity is back they walk like a normal player and fire
 *                   their thrusters only when asked (Space); every other body flies on thrusters all the time
 * @param halfOf     set on a part of a bigger body that can fly apart from it, naming the art rig it is drawn with: a body on this layout
 *                   is the sections it has, with no core section to name it, so it goes by the section in {@code coreSocket}
 */
public record Chassis(ResourceKey<SizeClass> sizeClass, String coreSocket, List<Socket> sockets, Optional<Component> name,
                      Optional<Cockpit> cockpit, boolean walks, Optional<String> halfOf) {
    /**
     * First-person camera offset from the eyes, in blocks at scale 1 (multiplied by the body's scale): {@code forward} along the view,
     * {@code up} and {@code right} across it. A chassis with a cockpit also hides the first-person hand.
     */
    public record Cockpit(float forward, float up, float right) {
        public static final Codec<Cockpit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("forward", 0f).forGetter(Cockpit::forward),
                Codec.FLOAT.optionalFieldOf("up", 0f).forGetter(Cockpit::up),
                Codec.FLOAT.optionalFieldOf("right", 0f).forGetter(Cockpit::right)
        ).apply(instance, Cockpit::new));
    }

    public static final Codec<Chassis> CODEC = RecordCodecBuilder.<Chassis>create(instance -> instance.group(
            ModCodecs.key(VoidReclaimerRegistries.SIZE_CLASS).fieldOf("size_class").forGetter(Chassis::sizeClass),
            Codec.STRING.fieldOf("core_socket").forGetter(Chassis::coreSocket),
            Socket.CODEC.listOf().fieldOf("sockets").forGetter(Chassis::sockets),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Chassis::name),
            Cockpit.CODEC.optionalFieldOf("cockpit").forGetter(Chassis::cockpit),
            Codec.BOOL.optionalFieldOf("walks", false).forGetter(Chassis::walks),
            Codec.STRING.optionalFieldOf("half_of").forGetter(Chassis::halfOf)
    ).apply(instance, Chassis::new)).validate(Chassis::validate);

    /** A whole body. */
    public Chassis(ResourceKey<SizeClass> sizeClass, String coreSocket, List<Socket> sockets, Optional<Component> name,
                   Optional<Cockpit> cockpit, boolean walks) {
        this(sizeClass, coreSocket, sockets, name, cockpit, walks, Optional.empty());
    }

    /** The same layout as a part of a body drawn with the rig {@code rig}. */
    public Chassis asHalfOf(String rig) {
        return new Chassis(sizeClass, coreSocket, sockets, name, cockpit, walks, Optional.of(rig));
    }

    /** A chassis that flies on thrusters all the time (the form before {@code walks} existed). */
    public Chassis(ResourceKey<SizeClass> sizeClass, String coreSocket, List<Socket> sockets, Optional<Component> name,
                   Optional<Cockpit> cockpit) {
        this(sizeClass, coreSocket, sockets, name, cockpit, false);
    }

    /** The same chassis with legs. */
    public Chassis withWalks(boolean value) {
        return new Chassis(sizeClass, coreSocket, sockets, name, cockpit, value);
    }

    public Chassis(ResourceKey<SizeClass> sizeClass, String coreSocket, List<Socket> sockets) {
        this(sizeClass, coreSocket, sockets, Optional.empty(), Optional.empty());
    }

    public Chassis(ResourceKey<SizeClass> sizeClass, String coreSocket, List<Socket> sockets, Cockpit cockpit) {
        this(sizeClass, coreSocket, sockets, Optional.empty(), Optional.of(cockpit));
    }

    @Nullable
    public Socket socket(String id) {
        for (Socket socket : sockets) {
            if (socket.id().equals(id)) {
                return socket;
            }
        }
        return null;
    }

    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("body." + id.getNamespace() + ".chassis." + id.getPath()));
    }

    private static DataResult<Chassis> validate(Chassis chassis) {
        Set<String> ids = new HashSet<>();
        for (Socket socket : chassis.sockets) {
            if (!ids.add(socket.id())) {
                return DataResult.error(() -> "duplicate socket " + socket.id());
            }
        }
        if (!ids.contains(chassis.coreSocket)) {
            return DataResult.error(() -> "core socket " + chassis.coreSocket + " is not one of the sockets");
        }
        for (Socket socket : chassis.sockets) {
            if (socket.parent().isPresent() && !ids.contains(socket.parent().get())) {
                return DataResult.error(() -> "socket " + socket.id() + " has unknown parent " + socket.parent().get());
            }
        }
        return DataResult.success(chassis);
    }
}
