package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoidReclaimer.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.void_reclaimer"))
            .icon(() -> ModItems.DUST_SENSOR.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.DUST_SENSOR.get());
                output.accept(ModItems.SCAFFOLD.get());
                output.accept(ModItems.SALVAGE_TOOL.get());
                output.accept(ModItems.NANITE_SWARM.get());
                output.accept(ModItems.BODY_BLUEPRINT.get());
                output.accept(ModItems.TETHER_ANCHOR.get());
                output.accept(ModItems.BACKUP_CORE.get());
                // Sections, by chassis, then modules.
                for (var item : ModItems.ITEMS.getEntries()) {
                    if (item.get() instanceof com.voidreclaimer.body.part.SectionItem) {
                        output.accept(item.get());
                    }
                }
                output.accept(ModItems.CORE_MK1.get());
                output.accept(ModItems.CORE_MK2.get());
                output.accept(ModItems.CORE_MK3.get());
                output.accept(ModItems.CORE_MK4.get());
                output.accept(ModItems.THRUSTER_MK1.get());
                output.accept(ModItems.THRUSTER_MK2.get());
                output.accept(ModItems.STORAGE_MK1.get());
                output.accept(ModItems.STORAGE_MK2.get());
                output.accept(ModItems.DUST_COMPRESSOR_MK1.get());
                output.accept(ModItems.DUST_COMPRESSOR_MK2.get());
                output.accept(ModItems.DUST_COMPRESSOR_MK3.get());
                output.accept(ModItems.BRASS_TRIM.get());
                output.accept(ModItems.COGWHEEL_CROWN.get());
                output.accept(ModItems.ELECTROLYZER.get());
                output.accept(ModItems.SPINNER.get());
                output.accept(ModItems.CLOUD_SIPHON.get());
                output.accept(ModItems.KINETIC_DRIVE_BASE.get());
                output.accept(ModItems.PAYLOAD_CHAMBER.get());
                output.accept(ModItems.MAGNETIC_BARREL_NOZZLE.get());
                output.accept(ModItems.QUANTUM_RECEIVER.get());
                output.accept(ModItems.MECHANICAL_DRILL.get());
                output.accept(ModItems.MATTER_HARVESTER.get());
                output.accept(ModItems.ASTEROID_SILICATE.get());
                output.accept(ModItems.ASTEROID_ICE.get());
                output.accept(ModItems.ASHEN_SLAG.get());
                output.accept(ModItems.PORTAL_FRAME.get());
                output.accept(ModItems.PORTAL_IGNITER.get());
                output.accept(ModItems.PLANETARY_GRID.get());
                output.accept(ModItems.TERRAFORMING_SPIRE.get());
                output.accept(ModItems.CHEMICAL_THRUSTER.get());
                output.accept(ModItems.ION_THRUSTER.get());
                output.accept(ModItems.HELM.get());
                output.accept(ModItems.STATION_CORE.get());
                output.accept(ModItems.DOCKING_CLAMP.get());
                output.accept(ModItems.UPLINK_SEAT.get());
                output.accept(ModItems.RELAY.get());
                output.accept(ModItems.DRONE_SHELL.get());
                output.accept(ModItems.MECH_SHELL.get());
                output.accept(ModItems.SHIP_SHELL.get());
                output.accept(ModItems.SHUTTLE_SHELL.get());
                output.accept(ModItems.TITAN_SHELL.get());
                output.accept(ModItems.HOLO_PROJECTOR.get());
                output.accept(ModItems.ASTEROID_CARBON.get());
                output.accept(ModItems.ASTEROID_METALLIC.get());
                for (var material : ModMaterials.ALL) {
                    output.accept(material.item().get());
                }
                for (var fluid : ModFluids.ALL) {
                    output.accept(fluid.bucket().get());
                }
                output.accept(ModItems.TEST_STARSHIP.get());
            })
            .build());

    private ModCreativeTabs() {}
}
