package com.voidreclaimer.body.part;

import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.body.Parts;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A body section (hull, torso, limb, wing, ...). Does nothing in the hand: install it in a socket from the Body tab.
 * Its stats live in the {@code section} component; core sections also name their chassis.
 */
public class SectionItem extends Item {
    public SectionItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        description(this, tooltip);
        SectionStats stats = Parts.section(stack);
        if (stats == null) {
            return;
        }
        Component type = Component.translatable("socket." + stats.socketType().getNamespace() + "." + stats.socketType().getPath());
        tooltip.add(Component.translatable(stats.isCore() ? "tooltip.void_reclaimer.section.core" : "tooltip.void_reclaimer.section",
                type, stats.tier(), ModuleItem.fmt(stats.mass())).withStyle(ChatFormatting.DARK_AQUA));
        MutableComponent bonuses = Component.empty();
        bonus(bonuses, "budget", stats.massBudget());
        bonus(bonuses, "health", stats.maxHealth() / 2f);
        bonus(bonuses, "reach", stats.reach());
        bonus(bonuses, "intake", stats.dustIntake());
        bonus(bonuses, "rows", stats.baseRows());
        bonus(bonuses, "stability", stats.stability());
        bonus(bonuses, "turn", stats.turnRate());
        bonus(bonuses, "wear", stats.wearResistance());
        if (!bonuses.getSiblings().isEmpty()) {
            tooltip.add(bonuses.withStyle(ChatFormatting.GRAY));
        }
        if (!stats.mounts().isEmpty()) {
            MutableComponent mounts = Component.translatable("tooltip.void_reclaimer.section.mounts");
            for (ModuleKind kind : ModuleKind.values()) {
                int count = stats.mounts(kind);
                if (count > 0) {
                    mounts.append(" ").append(Component.translatable("tooltip.void_reclaimer.section.mount", count, kind.displayName()));
                }
            }
            tooltip.add(mounts.withStyle(ChatFormatting.GRAY));
        }
    }

    private static void bonus(MutableComponent line, String key, float value) {
        if (value != 0f) {
            if (!line.getSiblings().isEmpty()) {
                line.append(", ");
            }
            line.append(Component.translatable("tooltip.void_reclaimer.section." + key, ModuleItem.fmt(value)));
        }
    }

    static void description(Item item, List<Component> tooltip) {
        String key = item.getDescriptionId() + ".tooltip";
        if (net.minecraft.locale.Language.getInstance().has(key)) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
