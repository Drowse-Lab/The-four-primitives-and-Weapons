package the_four_primitives_and_weapons.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphics;
import the_four_primitives_and_weapons.client.event.RarityTooltipFrameHandler;

/** Hypixel風の角飾りが文字に重ならないよう、アイテムツールチップの余白を拡張する。 */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipPaddingMixin {

    private static final int HORIZONTAL_PADDING = 11;
    private static final int TOP_PADDING = 13;
    private static final int BOTTOM_PADDING = 11;
    private static final int BORDER_INSET = 1;

    @ModifyVariable(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static int theFourPrimitives$expandLeft(int x) {
        return x - HORIZONTAL_PADDING + BORDER_INSET;
    }

    @ModifyVariable(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private static int theFourPrimitives$expandTop(int y) {
        return y - TOP_PADDING + BORDER_INSET;
    }

    @ModifyVariable(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false)
    private static int theFourPrimitives$expandWidth(int width) {
        return width + HORIZONTAL_PADDING * 2 - BORDER_INSET * 2;
    }

    @ModifyVariable(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 3, remap = false)
    private static int theFourPrimitives$expandHeight(int height) {
        return height + TOP_PADDING + BOTTOM_PADDING - BORDER_INSET * 2;
    }

    @Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V",
            at = @At("TAIL"), remap = false)
    private static void theFourPrimitives$drawFrameAfterBackground(
            GuiGraphics graphics, int x, int y, int width, int height, int z,
            int backgroundTop, int backgroundBottom, int borderTop, int borderBottom,
            CallbackInfo ci) {
        RarityTooltipFrameHandler.renderPendingFrame(graphics, x, y, width, height);
    }
}
