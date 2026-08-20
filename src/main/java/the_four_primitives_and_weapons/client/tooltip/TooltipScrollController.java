package the_four_primitives_and_weapons.client.tooltip;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.config.TooltipConfig;

/** Allows tooltips taller than the screen to be moved with the mouse wheel. */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
public final class TooltipScrollController {
    private static final int SCREEN_MARGIN = 4;
    private static final int SCROLL_STEP = 12;

    private static ItemStack lastStack = ItemStack.EMPTY;
    private static int offset;
    private static double scrollRemainder;
    private static long lastRenderTime;

    private TooltipScrollController() {}

    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        boolean resumedAfterPause = Util.getMillis() - lastRenderTime > 250L;
        if (resumedAfterPause || !ItemStack.isSameItemSameTags(lastStack, stack)) {
            lastStack = stack.copy();
            offset = 0;
            scrollRemainder = 0.0;
        }
        lastRenderTime = Util.getMillis();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (applyScroll(event.getScrollDelta())) event.setCanceled(true);
    }

    /** GUI screens use ScreenEvent instead of InputEvent.MouseScrollingEvent. */
    @SubscribeEvent
    public static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (applyScroll(event.getScrollDelta())) event.setCanceled(true);
    }

    private static boolean applyScroll(double delta) {
        if (!Screen.hasShiftDown() || Util.getMillis() - lastRenderTime > 250L) return false;

        // A mouse wheel usually reports +/-1, while macOS trackpads send many
        // small fractional deltas. Accumulating the fraction supports both.
        double direction = TooltipConfig.reverseScrollDirection ? -1.0 : 1.0;
        scrollRemainder += delta * SCROLL_STEP * direction;
        int movement = (int) scrollRemainder;
        if (movement == 0) return true;

        scrollRemainder -= movement;
        int oldOffset = offset;
        offset += movement;
        return offset != oldOffset;
    }

    /** Called after vanilla has chosen the tooltip position. */
    public static int adjustedY(int vanillaY, int screenHeight, int tooltipHeight) {
        int minimumY = SCREEN_MARGIN - tooltipHeight;
        int maximumY = screenHeight - SCREEN_MARGIN;
        int adjusted = Math.max(minimumY, Math.min(maximumY, vanillaY + offset));
        // Keep the stored value synchronized with the clamped position so the
        // reverse direction responds immediately at either edge.
        offset = adjusted - vanillaY;
        return adjusted;
    }
}
