package the_four_primitives_and_weapons.client.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import the_four_primitives_and_weapons.damage.ElementType;

/** 本体JAR内の16x16 PNGを属性紋様としてツールチップ上辺に描画する。 */
public final class EditableTooltipElementTextures {
    private static final int SIZE = 16;

    private EditableTooltipElementTextures() {}

    public static void draw(GuiGraphics graphics, ElementType type, int centerX, int top) {
        if (type == ElementType.NONE) return;
        ResourceLocation texture = getTexture(type);
        if (texture != null) graphics.blit(texture, centerX - SIZE / 2, top, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    private static ResourceLocation getTexture(ElementType type) {
        return new ResourceLocation("the_four_primitives_and_weapons",
                "textures/gui/tooltip_elements/" + type.name().toLowerCase() + ".png");
    }

}
