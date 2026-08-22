package the_four_primitives_and_weapons.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import the_four_primitives_and_weapons.damage.ElementType;

/** 本体JAR内の16x16 PNGを、ツールチップ全周から伸びる属性紋様として描画する。 */
public final class EditableTooltipElementTextures {
    private static final int SIZE = 16;

    private EditableTooltipElementTextures() {}

    public static void draw(GuiGraphics graphics, ElementType type, int centerX, int top) {
        if (type == ElementType.NONE) return;
        ResourceLocation texture = getTexture(type);
        if (texture != null) graphics.blit(texture, centerX - SIZE / 2, top, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    /**
     * Ice and FireのDragonforgeを意識した、暗い炉レンガと属性色の発光亀裂。
     * 装飾は外周の細い帯だけに収め、中央の文字領域へ大柄な画像を置かない。
     */
    public static void drawBorderPattern(GuiGraphics graphics, ElementType type,
                                         int left, int top, int right, int bottom, float alpha) {
        drawStaticElementOverlay(graphics, type, left, top, right, bottom);
    }

    /**
     * MOD専用の静止PNGを、縁の一部から少しだけ内側へ出る装飾として描画する。
     * 属性画像自体を見せるのではなく、文字領域を侵食しない縁飾りに限定する。
     */
    private static void drawStaticElementOverlay(GuiGraphics graphics, ElementType type,
                                                 int left, int top, int right, int bottom) {
        ResourceLocation texture = getTexture(type);
        int source = 32;
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.88F);
        graphics.enableScissor(left, top, right, bottom);

        // ハイピクセル風の「縁から少し出る」量。ツールチップの大きさに関係なく大柄にしない。
        int horizontalSpan = Math.min(width - 4, Math.max(24, Math.min(48, width / 4)));
        int verticalSpan = Math.min(height - 4, Math.max(20, Math.min(40, height / 4)));
        int verticalDepth = Math.min(height, 12);
        int horizontalDepth = Math.min(width, 12);

        switch (type) {
            case FIRE -> drawFromBottom(graphics, texture,
                    left + (width - horizontalSpan) / 2, bottom, horizontalSpan, verticalDepth, source);
            case SOUL_FIRE -> drawFromBottom(graphics, texture,
                    left + width / 6, bottom, horizontalSpan, verticalDepth, source);
            case ICE -> drawFromTop(graphics, texture,
                    right - horizontalSpan - 8, top, horizontalSpan, verticalDepth, source);
            case BLOOD -> drawFromTop(graphics, texture,
                    left + 8, top, horizontalSpan, verticalDepth, source);
            case ELECTRIC -> drawFromLeft(graphics, texture,
                    left, top + 12, verticalSpan, horizontalDepth, source);
            case THUNDER -> drawFromTop(graphics, texture,
                    left + (width - horizontalSpan) / 2, top, horizontalSpan, verticalDepth, source);
            case CORROSION -> drawFromRight(graphics, texture,
                    right, top + Math.max(8, (height - verticalSpan) / 2), verticalSpan, horizontalDepth, source);
            case WATER -> drawFromRight(graphics, texture,
                    right, top + 10, verticalSpan, horizontalDepth, source);
            case WIND -> drawFromLeft(graphics, texture,
                    left, bottom - verticalSpan - 10, verticalSpan, horizontalDepth, source);
            case HOLY -> drawFromTop(graphics, texture,
                    left + (width - horizontalSpan) / 2, top, horizontalSpan, verticalDepth, source);
            case DARK, ERASURE -> drawFromRight(graphics, texture,
                    right, top + (height - verticalSpan) / 2, verticalSpan, horizontalDepth, source);
            case MIASMA -> drawFromBottom(graphics, texture,
                    right - horizontalSpan - 10, bottom, horizontalSpan, verticalDepth, source);
            case SOUL -> drawFromLeft(graphics, texture,
                    left, top + (height - verticalSpan) / 2, verticalSpan, horizontalDepth, source);
            case NONE -> { }
        }

        graphics.disableScissor();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawFromBottom(GuiGraphics g, ResourceLocation texture,
                                       int x, int bottom, int span, int depth, int source) {
        g.blit(texture, x, bottom - depth, span, depth,
                0, 0, source, source, source, source);
    }

    private static void drawFromTop(GuiGraphics g, ResourceLocation texture,
                                    int x, int top, int span, int depth, int source) {
        g.pose().pushPose();
        g.pose().translate(0.0F, top * 2.0F + depth, 0.0F);
        g.pose().scale(1.0F, -1.0F, 1.0F);
        g.blit(texture, x, top, span, depth,
                0, 0, source, source, source, source);
        g.pose().popPose();
    }

    private static void drawFromLeft(GuiGraphics g, ResourceLocation texture,
                                     int left, int y, int span, int depth, int source) {
        g.pose().pushPose();
        g.pose().translate(left + depth, y, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(90.0F));
        g.blit(texture, 0, 0, span, depth,
                0, 0, source, source, source, source);
        g.pose().popPose();
    }

    private static void drawFromRight(GuiGraphics g, ResourceLocation texture,
                                      int right, int y, int span, int depth, int source) {
        g.pose().pushPose();
        g.pose().translate(right - depth, y + span, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90.0F));
        g.blit(texture, 0, 0, span, depth,
                0, 0, source, source, source, source);
        g.pose().popPose();
    }

    private static void drawBorderPatternNative(GuiGraphics graphics, ElementType type,
                                                int left, int top, int right, int bottom, float alpha) {
        if (type == ElementType.NONE || right <= left || bottom <= top) return;
        int width = right - left;
        int height = bottom - top;
        int glow = withAlpha(elementColor(type), Math.min(1.0F, alpha * 4.5F));
        int glowSoft = withAlpha(elementColor(type), Math.min(1.0F, alpha * 1.7F));
        int carved = withAlpha(0x05070B, Math.min(1.0F, alpha * 2.2F));

        // 状態変化系は縁そのものを炎・氷・血へ置き換えるので彫り込み線を描かない。
        if (type != ElementType.FIRE && type != ElementType.ICE && type != ElementType.BLOOD) {
            gFrame(graphics, left + 1, top + 1, right - 1, bottom - 1, carved);
        }

        // 炎・氷・血は枠そのものが変質する専用表現なので、直線の共通主脈を使わない。
        if (type != ElementType.FIRE && type != ElementType.ICE && type != ElementType.BLOOD) {
            graphics.fill(left + 3, top + 2, right - 3, top + 3, glowSoft);
            graphics.fill(left + 3, bottom - 3, right - 3, bottom - 2, glowSoft);
            graphics.fill(left + 2, top + 3, left + 3, bottom - 3, glowSoft);
            graphics.fill(right - 3, top + 3, right - 2, bottom - 3, glowSoft);
        }

        drawElementSpecificPattern(graphics, type, left, top, right, bottom, glow, glowSoft);

        if (type != ElementType.FIRE && type != ElementType.ICE && type != ElementType.BLOOD) {
            // 四隅の竜角。状態変化系には直線状の角飾りを付けない。
            graphics.fill(left + 3, top + 3, left + 9, top + 4, glow);
            graphics.fill(left + 3, top + 3, left + 4, top + 9, glow);
            graphics.fill(right - 9, top + 3, right - 3, top + 4, glow);
            graphics.fill(right - 4, top + 3, right - 3, top + 9, glow);
            graphics.fill(left + 3, bottom - 4, left + 9, bottom - 3, glow);
            graphics.fill(left + 3, bottom - 9, left + 4, bottom - 3, glow);
            graphics.fill(right - 9, bottom - 4, right - 3, bottom - 3, glow);
            graphics.fill(right - 4, bottom - 9, right - 3, bottom - 3, glow);
        }
    }

    private static void gFrame(GuiGraphics g, int l, int t, int r, int b, int color) {
        g.fill(l + 2, t, r - 2, t + 1, color);
        g.fill(l + 2, b - 1, r - 2, b, color);
        g.fill(l, t + 2, l + 1, b - 2, color);
        g.fill(r - 1, t + 2, r, b - 2, color);
        // 四隅は角を落とし、竜炉の石彫刻のような八角形にする。
        g.fill(l + 1, t + 1, l + 3, t + 2, color);
        g.fill(r - 3, t + 1, r - 1, t + 2, color);
        g.fill(l + 1, b - 2, l + 3, b - 1, color);
        g.fill(r - 3, b - 2, r - 1, b - 1, color);
    }

    private static void drawElementSpecificPattern(GuiGraphics g, ElementType type,
                                                   int l, int t, int r, int b,
                                                   int color, int dim) {
        int cx = l + (r - l) / 2;
        int cy = t + (b - t) / 2;
        switch (type) {
            case FIRE -> { // 段階的に細くなる三色のピクセル炎
                int hot = replaceRgb(color, 0xFFD84A);
                int ember = replaceRgb(dim, 0x9E2408);
                // 根元を重ねた炎だけで縁を構成する。直線の下地は一切使わない。
                for (int x = l + 8, i = 0; x < r - 7; x += 11, i++) {
                    int depth = 16 + (i % 3) * 4;
                    drawFlame(g, x, t + 2, depth, 1, ember, color, hot);
                    drawFlame(g, x + 5, b - 3, 14 + ((i + 1) % 3) * 4, -1, ember, color, hot);
                }
                for (int y = t + 8, i = 0; y < b - 7; y += 11, i++) {
                    int depth = 14 + (i % 3) * 4;
                    drawSideFlame(g, l + 2, y, depth, 1, ember, color, hot);
                    drawSideFlame(g, r - 3, y + 5, 14 + ((i + 1) % 3) * 4, -1, ember, color, hot);
                }
            }
            case ICE -> { // 縁全体の霜、氷晶、内側へ伸びる氷柱
                g.fill(l + 4, t + 3, r - 4, t + 5, dim);
                g.fill(l + 4, b - 5, r - 4, b - 3, dim);
                for (int x = l + 8, i = 0; x < r - 6; x += 11, i++) {
                    int icicle = 4 + (i % 3) * 2;
                    g.fill(x, t + 3, x + 2, t + 3 + icicle, color);
                    g.fill(x + 1, t + 3 + icicle, x + 2, t + 5 + icicle, dim);
                    g.fill(x + 4, b - 3 - icicle, x + 6, b - 3, color);
                }
                for (int y = t + 11; y < b - 7; y += 15) {
                    g.fill(l + 3, y, l + 9, y + 2, color);
                    g.fill(r - 9, y + 5, r - 3, y + 7, color);
                }
                drawCrystal(g, cx, t + 6, color, dim);
                drawCrystal(g, cx, b - 7, color, dim);
                drawCrystalSide(g, l + 6, cy, color, dim);
                drawCrystalSide(g, r - 7, cy, color, dim);
            }
            case ELECTRIC -> { // 細かく枝分かれする放電
                for (int x = l + 9; x < r - 5; x += 16) {
                    g.fill(x, t + 2, x + 1, t + 5, color);
                    g.fill(x - 2, t + 5, x + 1, t + 6, color);
                    g.fill(x - 2, t + 5, x - 1, t + 8, dim);
                }
            }
            case THUNDER -> { // 大きく太い稲妻
                for (int x = l + 18; x < r - 8; x += 38) {
                    g.fill(x, t + 2, x + 2, t + 6, color);
                    g.fill(x - 4, t + 5, x + 2, t + 7, color);
                    g.fill(x - 4, t + 6, x - 2, t + 11, color);
                    g.fill(x - 2, b - 10, x, b - 5, dim);
                    g.fill(x - 2, b - 6, x + 4, b - 4, color);
                }
            }
            case CORROSION -> { // 侵食する棘蔓
                for (int x = l + 13; x < r - 8; x += 26) {
                    g.fill(x, t + 2, x + 1, t + 9, color);
                    g.fill(x - 3, t + 5, x + 1, t + 6, color);
                    g.fill(x + 1, t + 7, x + 4, t + 8, dim);
                    g.fill(x + 3, b - 8, x + 4, b - 2, color);
                    g.fill(x, b - 6, x + 4, b - 5, color);
                }
            }
            case WATER -> { // 二段の波
                drawWave(g, l + 6, r - 6, t + 5, color, dim);
                drawWave(g, l + 10, r - 4, b - 6, dim, color);
            }
            case WIND -> { // 風の流線
                g.fill(l + 8, t + 5, r - 12, t + 6, color);
                g.fill(l + 14, t + 8, r - 6, t + 9, dim);
                g.fill(l + 8, b - 6, r - 18, b - 5, color);
                g.fill(l + 7, t + 5, l + 8, t + 8, color);
                g.fill(r - 12, t + 3, r - 11, t + 6, dim);
            }
            case HOLY -> { // 聖光の放射
                g.fill(cx, t + 2, cx + 1, t + 11, color);
                g.fill(cx - 5, t + 6, cx + 6, t + 7, color);
                g.fill(cx, b - 11, cx + 1, b - 2, color);
                g.fill(l + 2, cy, l + 11, cy + 1, dim);
                g.fill(r - 11, cy, r - 2, cy + 1, dim);
            }
            case DARK -> { // 闇の鉤爪
                drawClaw(g, l + 10, t + 2, 1, color);
                drawClaw(g, r - 11, b - 9, -1, color);
            }
            case MIASMA -> { // 瘴気の不規則な雲
                for (int x = l + 11; x < r - 8; x += 29) {
                    g.fill(x, t + 4, x + 7, t + 6, dim);
                    g.fill(x + 2, t + 2, x + 5, t + 8, color);
                    g.fill(x + 5, b - 7, x + 9, b - 4, dim);
                }
            }
            case BLOOD -> { // 上辺に溜まった血が長さを変えて滴り、下辺には血溜まりができる
                g.fill(l + 4, t + 2, r - 4, t + 5, dim);
                for (int x = l + 9, i = 0; x < r - 7; x += 13, i++) {
                    int drop = 4 + (i % 4) * 2;
                    g.fill(x, t + 4, x + 2, t + 4 + drop, color);
                    g.fill(x - 1, t + 3 + drop, x + 3, t + 5 + drop, color);
                    if ((i & 1) == 0) g.fill(x, t + 5 + drop, x + 2, t + 7 + drop, dim);
                }
                // 左右を伝う血筋。
                for (int y = t + 13, i = 0; y < b - 10; y += 19, i++) {
                    int reach = 4 + (i % 2) * 3;
                    g.fill(l + 2, y, l + reach, y + 2, color);
                    g.fill(r - reach, y + 7, r - 2, y + 9, dim);
                }
                g.fill(l + 8, b - 5, r - 10, b - 3, color);
                for (int x = l + 14; x < r - 12; x += 27) {
                    g.fill(x, b - 7, x + 7, b - 4, dim);
                }
            }
            case ERASURE -> { // 途切れた虚無ルーン
                for (int x = l + 8; x < r - 8; x += 20) {
                    g.fill(x, t + 3, Math.min(r - 3, x + 7), t + 4, color);
                    g.fill(x + 3, b - 4, Math.min(r - 3, x + 10), b - 3, dim);
                }
            }
            case SOUL -> { // 魂の揺らぎ
                for (int x = l + 15; x < r - 8; x += 28) {
                    g.fill(x, t + 2, x + 1, t + 9, color);
                    g.fill(x - 2, t + 5, x + 3, t + 6, dim);
                    g.fill(x + 1, b - 9, x + 2, b - 2, color);
                }
            }
            case SOUL_FIRE -> { // 二重の魂炎
                for (int x = l + 12; x < r - 8; x += 24) {
                    g.fill(x, t + 2, x + 1, t + 10, color);
                    g.fill(x + 3, t + 4, x + 4, t + 8, dim);
                    g.fill(x - 2, t + 7, x + 4, t + 8, color);
                }
            }
            case NONE -> { }
        }
    }

    private static void drawCrystal(GuiGraphics g, int x, int y, int c, int d) {
        g.fill(x, y - 4, x + 1, y + 5, c); g.fill(x - 4, y, x + 5, y + 1, c);
        g.fill(x - 2, y - 2, x + 3, y + 3, d);
    }

    private static void drawFlame(GuiGraphics g, int x, int edgeY, int depth, int direction,
                                  int outer, int middle, int core) {
        for (int n = 0; n < depth; n++) {
            int y = edgeY + direction * n;
            int half = Math.max(0, (depth - n) / 3);
            int sway = n > depth / 2 ? ((n & 1) == 0 ? 1 : 0) : 0;
            int minX = x - half + sway;
            int maxX = x + half + 2 + sway;
            g.fill(minX, y, maxX, y + 1, outer);
            if (n >= 2 && n < depth - 1) {
                g.fill(x + sway, y, x + 2 + sway, y + 1, middle);
            }
            if (n >= 3 && n < depth - 3) {
                g.fill(x + 1 + sway, y, x + 2 + sway, y + 1, core);
            }
        }
    }

    private static void drawSideFlame(GuiGraphics g, int edgeX, int y, int depth, int direction,
                                      int outer, int middle, int core) {
        for (int n = 0; n < depth; n++) {
            int x = edgeX + direction * n;
            int half = Math.max(0, (depth - n) / 3);
            int sway = n > depth / 2 ? (n & 1) : 0;
            int minY = y - half + sway;
            int maxY = y + half + 2 + sway;
            g.fill(x, minY, x + 1, maxY, outer);
            if (n >= 2 && n < depth - 1) {
                g.fill(x, y + sway, x + 1, y + 2 + sway, middle);
            }
            if (n >= 3 && n < depth - 3) {
                g.fill(x, y + 1 + sway, x + 1, y + 2 + sway, core);
            }
        }
    }

    private static int replaceRgb(int argb, int rgb) {
        return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
    }

    private static void drawCrystalSide(GuiGraphics g, int x, int y, int c, int d) {
        g.fill(x - 4, y, x + 5, y + 1, c); g.fill(x, y - 4, x + 1, y + 5, d);
    }

    private static void drawWave(GuiGraphics g, int l, int r, int y, int c, int d) {
        for (int x = l; x < r; x += 12) {
            g.fill(x, y, Math.min(r, x + 5), y + 1, c);
            g.fill(Math.min(r - 1, x + 4), y - 2, Math.min(r, x + 5), y + 1, d);
        }
    }

    private static void drawClaw(GuiGraphics g, int x, int y, int direction, int c) {
        for (int i = 0; i < 3; i++) {
            int px = x + direction * i * 4;
            g.fill(px, y, px + 1, y + 8 - i, c);
            g.fill(Math.min(px, px + direction * 3), y + 7 - i,
                    Math.max(px, px + direction * 3) + 1, y + 8 - i, c);
        }
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int elementColor(ElementType type) {
        return switch (type) {
            case FIRE -> 0xFF5A18;
            case ICE -> 0x9DEBFF;
            case WATER -> 0x258DFF;
            case ELECTRIC -> 0xFFF266;
            case THUNDER -> 0xCCE6FF;
            case WIND -> 0x8CFFD2;
            case HOLY -> 0xFFE66A;
            case DARK, ERASURE -> 0x7548D8;
            case CORROSION -> 0xBF1A8C;
            case MIASMA -> 0xA94AC9;
            case BLOOD -> 0xE22525;
            case SOUL -> 0x44D6C7;
            case SOUL_FIRE -> 0x28E6FF;
            default -> 0xFFFFFF;
        };
    }

    private static ResourceLocation getTexture(ElementType type) {
        return new ResourceLocation("the_four_primitives_and_weapons",
                "textures/gui/tooltip_elements/" + type.name().toLowerCase() + ".png");
    }

}
