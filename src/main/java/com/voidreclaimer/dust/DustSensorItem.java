package com.voidreclaimer.dust;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Dust Sensor module. Works from anywhere in the inventory: adds the cloud readout and markers to the HUD, the
 * directional hum, and the sensor pulse. Becomes a sensor body part in Phase 5a.
 */
public final class DustSensorItem extends Item {
    public DustSensorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.void_reclaimer.dust_sensor.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.void_reclaimer.dust_sensor.tooltip.pulse").withStyle(ChatFormatting.DARK_AQUA));
    }
}
