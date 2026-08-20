package the_four_primitives_and_weapons.client.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;

/**
 * 本体JARに同梱されたPNGをツールチップの四隅として描画する。
 * 32x32 PNGの各16x16区画が、左上・右上・左下・右下に対応する。
 */
public final class EditableTooltipCornerTextures {
    private static final int ATLAS_SIZE = 32;
    private static final int CORNER_SIZE = 16;
    private static final int FRAME_LINE = 1;

    private EditableTooltipCornerTextures() {}

    public static void draw(GuiGraphics graphics, WeaponRarity rarity, int left, int top, int right, int bottom) {
        ResourceLocation texture = getTexture(rarity);
        if (texture == null) return;

        // Each edge is extended from the matching edge pixel in the same PNG.
        // Drawing these first lets the four corner tiles cover every join cleanly.
        int horizontalLength = Math.max(0, right - left - (CORNER_SIZE - 2) * 2);
        int verticalLength = Math.max(0, bottom - top - (CORNER_SIZE - 2) * 2);
        if (horizontalLength > 0) {
            graphics.blit(texture, left + CORNER_SIZE - 2, top,
                    horizontalLength, FRAME_LINE, 15, 1, 1, 1, ATLAS_SIZE, ATLAS_SIZE);
            graphics.blit(texture, left + CORNER_SIZE - 2, bottom,
                    horizontalLength, FRAME_LINE, 15, 30, 1, 1, ATLAS_SIZE, ATLAS_SIZE);
        }
        if (verticalLength > 0) {
            graphics.blit(texture, left, top + CORNER_SIZE - 2,
                    FRAME_LINE, verticalLength, 1, 15, 1, 1, ATLAS_SIZE, ATLAS_SIZE);
            graphics.blit(texture, right, top + CORNER_SIZE - 2,
                    FRAME_LINE, verticalLength, 30, 15, 1, 1, ATLAS_SIZE, ATLAS_SIZE);
        }

        // The editable atlas keeps one transparent pixel outside the frame line.
        // Place that padding outside the vanilla border so the line in each tile
        // lands exactly on left/top/right/bottom and joins the straight edges.
        graphics.blit(texture, left - 1, top - 1, 0, 0,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, right - CORNER_SIZE + 2, top - 1, 16, 0,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, left - 1, bottom - CORNER_SIZE + 2, 0, 16,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, right - CORNER_SIZE + 2, bottom - CORNER_SIZE + 2, 16, 16,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
    }

    private static ResourceLocation getTexture(WeaponRarity rarity) {
        return new ResourceLocation("the_four_primitives_and_weapons",
                "textures/gui/tooltip_corners/" + rarity.name().toLowerCase() + ".png");
    }
}
