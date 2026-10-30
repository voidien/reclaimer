package com.voidreclaimer;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.machine.ConfigCondition;
import com.voidreclaimer.network.ModPayloads;
import com.voidreclaimer.dust.StarterNebula;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModChunkGenerators;
import com.voidreclaimer.registry.ModCreativeTabs;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModRecipeSerializers;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.registry.ModRegistries;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(VoidReclaimer.MOD_ID)
public final class VoidReclaimer {
    public static final String MOD_ID = "void_reclaimer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidReclaimer(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModRegistries.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ConfigCondition.CONDITIONS.register(modBus);
        com.voidreclaimer.machine.MachineSetup.FAN_TYPES.register(modBus);
        modBus.addListener(com.voidreclaimer.machine.MachineSetup::onCommonSetup);
        modBus.addListener(com.voidreclaimer.compat.cc.CCCompat::onCommonSetup);
        modBus.addListener(com.voidreclaimer.compat.cc.CCCompat::registerCapabilities);
        modBus.addListener(com.voidreclaimer.machine.MachineSetup::registerCapabilities);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
        com.voidreclaimer.registry.ModMenus.MENUS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModMaterials.init();
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        com.voidreclaimer.registry.ModEntities.ENTITY_TYPES.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModRecipeTypes.RECIPE_TYPES.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(ModPayloads::register);
        StarterNebula.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
