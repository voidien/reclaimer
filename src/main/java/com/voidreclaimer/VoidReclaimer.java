package com.voidreclaimer;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.network.ModPayloads;
import com.voidreclaimer.registry.ModAttachments;
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
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(ModPayloads::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
