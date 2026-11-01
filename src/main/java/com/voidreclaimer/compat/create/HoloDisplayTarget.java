package com.voidreclaimer.compat.create;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lets a Create Display Link write to a Holo Projector: each line the link sends lands on the matching line, the others stay,
 * so several links can share one projector. The link is one more program writing text, so the last writer wins.
 */
public final class HoloDisplayTarget extends DisplayTarget {
    @Override
    public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
        if (!(context.getTargetBlockEntity() instanceof HoloProjectorBlockEntity projector)) {
            return;
        }
        for (int i = 0; i < text.size(); i++) {
            projector.setLine(line + i, text.get(i).getString(), HoloDisplay.NO_COLOR);
        }
    }

    @Override
    public DisplayTargetStats provideStats(DisplayLinkContext context) {
        return new DisplayTargetStats(HoloDisplay.MAX_LINES, HoloDisplay.MAX_CHARS, this);
    }
}
