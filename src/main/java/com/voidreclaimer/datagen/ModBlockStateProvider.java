package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.SCAFFOLD.get(), cubeAll(ModBlocks.SCAFFOLD.get()));
        simpleBlockWithItem(ModBlocks.TETHER_ANCHOR.get(), cubeAll(ModBlocks.TETHER_ANCHOR.get()));
        simpleBlockWithItem(ModBlocks.BACKUP_CORE.get(), cubeAll(ModBlocks.BACKUP_CORE.get()));
        machine(ModBlocks.ELECTROLYZER.get(), "electrolyzer");
        machine(ModBlocks.SPINNER.get(), "spinner");
        machine(ModBlocks.CLOUD_SIPHON.get(), "cloud_siphon");
        machine(ModBlocks.KINETIC_DRIVE_BASE.get(), "kinetic_drive_base");
        machine(ModBlocks.QUANTUM_RECEIVER.get(), "quantum_receiver");
        simpleBlockWithItem(ModBlocks.PAYLOAD_CHAMBER.get(), cubeAll(ModBlocks.PAYLOAD_CHAMBER.get()));
        simpleBlockWithItem(ModBlocks.MAGNETIC_BARREL_NOZZLE.get(), cubeAll(ModBlocks.MAGNETIC_BARREL_NOZZLE.get()));
        simpleBlockWithItem(ModBlocks.PORTAL_FRAME.get(), cubeAll(ModBlocks.PORTAL_FRAME.get()));
        simpleBlockWithItem(ModBlocks.TERRAFORMING_SPIRE.get(), cubeAll(ModBlocks.TERRAFORMING_SPIRE.get()));
        var thruster = models().cubeBottomTop("chemical_thruster", modLoc("block/chemical_thruster_side"),
                modLoc("block/chemical_thruster_back"), modLoc("block/chemical_thruster_nozzle"));
        directionalBlock(ModBlocks.CHEMICAL_THRUSTER.get(), thruster);
        simpleBlockItem(ModBlocks.CHEMICAL_THRUSTER.get(), thruster);
        var ion = models().cubeBottomTop("ion_thruster", modLoc("block/ion_thruster_side"),
                modLoc("block/ion_thruster_back"), modLoc("block/ion_thruster_nozzle"));
        directionalBlock(ModBlocks.ION_THRUSTER.get(), ion);
        simpleBlockItem(ModBlocks.ION_THRUSTER.get(), ion);
        // The RCS Thruster's item keeps its flat icon (a material): only the placed block needs a model here.
        directionalBlock(ModBlocks.RCS_THRUSTER.get(), models().cubeBottomTop("rcs_thruster", modLoc("block/rcs_thruster_side"),
                modLoc("block/rcs_thruster_back"), modLoc("block/rcs_thruster_nozzle")));
        // The Helm is drawn by GeckoLib; the blockstate points at an empty model and the item at a flat icon.
        getVariantBuilder(ModBlocks.HELM.get()).forAllStates(state -> net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder()
                .modelFile(models().getBuilder("helm").texture("particle", modLoc("block/helm_particle"))).build());
        itemModels().basicItem(ModItems.HELM.get());
        portal();
        simpleBlockWithItem(ModBlocks.ASHEN_SLAG.get(), cubeAll(ModBlocks.ASHEN_SLAG.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_SILICATE.get(),cubeAll(ModBlocks.ASTEROID_SILICATE.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_ICE.get(), cubeAll(ModBlocks.ASTEROID_ICE.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_CARBON.get(), cubeAll(ModBlocks.ASTEROID_CARBON.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_METALLIC.get(), cubeAll(ModBlocks.ASTEROID_METALLIC.get()));
    }

    /** The portal pane: a thin translucent plate, one model per width axis. */
    private void portal() {
        var acrossX = pane("atmospheric_portal_x", 6, 10, true);
        var acrossZ = pane("atmospheric_portal_z", 6, 10, false);
        getVariantBuilder(ModBlocks.ATMOSPHERIC_PORTAL.get())
                .partialState().with(com.voidreclaimer.earth.AtmosphericPortalBlock.AXIS, net.minecraft.core.Direction.Axis.X)
                .modelForState().modelFile(acrossX).addModel()
                .partialState().with(com.voidreclaimer.earth.AtmosphericPortalBlock.AXIS, net.minecraft.core.Direction.Axis.Z)
                .modelForState().modelFile(acrossZ).addModel();
    }

    private net.neoforged.neoforge.client.model.generators.BlockModelBuilder pane(String name, int from, int to, boolean faceNorthSouth) {
        var texture = modLoc("block/atmospheric_portal");
        var model = models().getBuilder(name).texture("portal", texture).texture("particle", texture).renderType("translucent");
        var element = model.element();
        if (faceNorthSouth) {
            element.from(0, 0, from).to(16, 16, to)
                    .face(net.minecraft.core.Direction.NORTH).texture("#portal").end()
                    .face(net.minecraft.core.Direction.SOUTH).texture("#portal").end();
        } else {
            element.from(from, 0, 0).to(to, 16, 16)
                    .face(net.minecraft.core.Direction.EAST).texture("#portal").end()
                    .face(net.minecraft.core.Direction.WEST).texture("#portal").end();
        }
        element.end();
        return model;
    }

    /** A kinetic machine: a column model turned to its shaft axis. */
    private void machine(net.minecraft.world.level.block.Block block, String name) {
        var model = models().cubeColumn(name, modLoc("block/" + name + "_side"), modLoc("block/" + name + "_end"));
        getVariantBuilder(block)
                .partialState().with(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS, net.minecraft.core.Direction.Axis.Y)
                .modelForState().modelFile(model).addModel()
                .partialState().with(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS, net.minecraft.core.Direction.Axis.X)
                .modelForState().modelFile(model).rotationX(90).rotationY(90).addModel()
                .partialState().with(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS, net.minecraft.core.Direction.Axis.Z)
                .modelForState().modelFile(model).rotationX(90).addModel();
        simpleBlockItem(block, model);
    }
}
