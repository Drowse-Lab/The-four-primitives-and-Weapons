package the_four_primitives_and_weapons.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import the_four_primitives_and_weapons.client.tooltip.TooltipScrollController;

@Mixin(GuiGraphics.class)
public abstract class TooltipPositionMixin {
    @Redirect(
            method = "renderTooltipInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"
            )
    )
    private Vector2ic theFourPrimitives$applyTooltipScroll(
            ClientTooltipPositioner positioner, int screenWidth, int screenHeight,
            int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) {
        Vector2ic positioned = positioner.positionTooltip(
                screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);
        int adjustedY = TooltipScrollController.adjustedY(positioned.y(), screenHeight, tooltipHeight);
        return new org.joml.Vector2i(positioned.x(), adjustedY);
    }
}
