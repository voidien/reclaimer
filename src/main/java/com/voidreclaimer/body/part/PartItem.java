package com.voidreclaimer.body.part;

import com.voidreclaimer.body.PartKind;
import com.voidreclaimer.body.PartStats;
import com.voidreclaimer.body.Parts;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A body part or module. Does nothing in the hand: install it from the Body tab of the Replicator. Its stats live in
 * the {@code part_stats} component, shown in the tooltip along with an optional description
 * ({@code item.void_reclaimer.<name>.tooltip}).
 */
public class PartItem extends Item {
    public PartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PartStats stats = Parts.stats(stack);
        String key = getDescriptionId() + ".tooltip";
        if (net.minecraft.locale.Language.getInstance().has(key)) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
        if (stats == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.void_reclaimer.part", stats.kind().displayName(), stats.tier(), fmt(stats.mass()))
                .withStyle(ChatFormatting.DARK_AQUA));
        stats.frame().ifPresent(f -> {
            tooltip.add(Component.translatable("tooltip.void_reclaimer.part.frame", f.size().displayName(), fmt(f.massBudget()),
                    fmt(f.maxHealth() / 2f)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.void_reclaimer.part.slots", f.slots(PartKind.THRUSTER), f.slots(PartKind.STORAGE),
                    f.slots(PartKind.SENSOR), f.slots(PartKind.TOOL), f.slots(PartKind.COSMETIC)).withStyle(ChatFormatting.GRAY));
        });
        stats.core().ifPresent(c -> tooltip.add(Component.translatable("tooltip.void_reclaimer.part.core", fmt(c.energyCapacity()),
                fmt(c.solarRate())).withStyle(ChatFormatting.GRAY)));
        stats.thruster().ifPresent(t -> tooltip.add(Component.translatable("tooltip.void_reclaimer.part.thruster",
                String.format(Locale.ROOT, "%.3f", t.thrust()), fmt(t.topSpeed())).withStyle(ChatFormatting.GRAY)));
        stats.storage().ifPresent(s -> tooltip.add(Component.translatable("tooltip.void_reclaimer.part.storage", s.rows(),
                fmt(s.dustCapacity())).withStyle(ChatFormatting.GRAY)));
        if (stats.minSize() != com.voidreclaimer.body.SizeClass.DRONE) {
            tooltip.add(Component.translatable("tooltip.void_reclaimer.part.min_size", stats.minSize().displayName())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static String fmt(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
