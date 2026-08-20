package the_four_primitives_and_weapons.client.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.client.tooltip.EditableTooltipCornerTextures;
import the_four_primitives_and_weapons.client.tooltip.EditableTooltipElementTextures;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;

/**
 * レアリティ付き武器のツールチップを SkyBlock 風の濃い背景と
 * レアリティカラーのグラデーション枠にする。
 *
 * <p>Forge の標準ツールチップ描画をそのまま利用するため、JEI や独自 GUI でも
 * ItemStack と一緒に描画されるツールチップへ自動的に適用される。
 * 独自レアリティ NBT がない場合は、バニラの Item rarity に対応する枠を使用する。</p>
 */
@Mod.EventBusSubscriber(
        modid = "the_four_primitives_and_weapons",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class RarityTooltipFrameHandler {

    private static final int BACKGROUND_TOP = 0xF0101A2B;
    private static final int BACKGROUND_BOTTOM = 0xF0060D18;
    private static final int HORIZONTAL_PADDING = 11;
    private static final int TOP_PADDING = 13;
    private static final int BOTTOM_PADDING = 11;
    private static final int BORDER_INSET = 1;

    private RarityTooltipFrameHandler() {
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        WeaponRarity rarity = WeaponRarity.getFromStack(stack);
        if (rarity == null) {
            rarity = fromVanillaRarity(stack.getRarity());
        }

        FrameColors colors = colorsFor(rarity);
        event.setBackgroundStart(BACKGROUND_TOP);
        event.setBackgroundEnd(BACKGROUND_BOTTOM);
        // 枠線と角を別々に描くと1pxのずれが出るため、標準枠は透明にする。
        // 四辺を含む枠全体は tooltip_corners の編集可能PNGから描画する。
        event.setBorderStart(0x00000000);
        event.setBorderEnd(0x00000000);

        drawCorners(event, rarity, colors.top(), colors.bottom());
    }

    /** 独自レアリティがないアイテムを、バニラ本来のレアリティから変換する。 */
    private static WeaponRarity fromVanillaRarity(net.minecraft.world.item.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> WeaponRarity.COMMON;
            case UNCOMMON -> WeaponRarity.UNCOMMON;
            case RARE -> WeaponRarity.RARE;
            case EPIC -> WeaponRarity.EPIC;
        };
    }

    private static void drawCorners(RenderTooltipEvent.Color event, WeaponRarity rarity,
                                    int color, int shadowColor) {
        int width = 0;
        int height = event.getComponents().size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : event.getComponents()) {
            width = Math.max(width, component.getWidth(event.getFont()));
            height += component.getHeight();
        }

        // TooltipRenderUtil#renderTooltipBackground が実際に描く内側枠の座標。
        int left = event.getX() - 3 - HORIZONTAL_PADDING + BORDER_INSET;
        // 背景の外端から1px内側。上辺の外側に背景色の余白を残す。
        int top = event.getY() - 3 - TOP_PADDING + BORDER_INSET;
        // 背景の外端から1px内側。右辺の外側にも背景色の余白を残す。
        int right = event.getX() + width + 3 + HORIZONTAL_PADDING - BORDER_INSET - 1;
        int bottom = event.getY() + height + 2 + BOTTOM_PADDING - BORDER_INSET;
        GuiGraphics graphics = event.getGraphics();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 401);
        EditableTooltipCornerTextures.draw(graphics, rarity, left, top, right, bottom);
        drawElementOrnaments(graphics, event.getItemStack(), left, top, right, bottom);
        graphics.pose().popPose();
    }

    /** 属性紋様を上辺中央へ重ねる。複合属性は左右に並べる。 */
    private static void drawElementOrnaments(GuiGraphics g, ItemStack stack, int l, int t, int r, int b) {
        ElementType primary = ElementalDamageUtils.getEffectiveElementType(stack);
        if (primary == ElementType.NONE) return;

        ElementType secondary = primary == ElementType.SOUL_FIRE
                ? ElementType.NONE
                : ElementalDamageUtils.getSecondaryElementType(stack);
        if (secondary == primary) secondary = ElementType.NONE;

        int centerX = l + (r - l) / 2;
        // 上枠へ重ねず、ツールチップ内側に1px空けて16x16 PNGを描く。
        int insideTop = t + 1;
        if (secondary == ElementType.NONE) {
            EditableTooltipElementTextures.draw(g, primary, centerX, insideTop);
        } else {
            EditableTooltipElementTextures.draw(g, primary, centerX - 9, insideTop);
            EditableTooltipElementTextures.draw(g, secondary, centerX + 9, insideTop);
        }
    }

    /** 元画像にある、連続枠の内側へ重なる小さな四隅の鉤。 */
    private static void drawReferenceCornerMarks(GuiGraphics g, int l, int t, int r, int b, int color) {
        // 参考画像の鉴型は外周線に接続し、内側3pxの帯に収まる。
        h(g, l + 2, t, 5, color);       v(g, l + 2, t, 3, color);
        h(g, r - 7, t, 5, color);       v(g, r - 3, t, 3, color);
        h(g, l + 2, t + 2, 3, color);   h(g, r - 5, t + 2, 3, color);

        h(g, l + 2, b, 5, color);       v(g, l + 2, b - 2, 3, color);
        h(g, r - 7, b, 5, color);       v(g, r - 3, b - 2, 3, color);
        h(g, l + 2, b - 2, 3, color);   h(g, r - 5, b - 2, 3, color);
    }

    /*
     * Hypixel の角画像を1pxの格子に起こしたテンプレート。
     * 各角は左上のビットマップを上下・左右反転して描画する。
     */
    private static void drawCommonReferenceCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawCornerTemplate(g, l, t, r, b, color, new String[]{
                "010111",
                "000100",
                "001100",
                "001000",
                "001000",
                "010000"
        });
    }

    private static void drawRareReferenceCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawCornerTemplate(g, l, t, r, b, color, new String[]{
                "111111",
                "100100",
                "111100",
                "110000",
                "110000",
                "100000"
        });
    }

    private static void drawLegendaryReferenceCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawCornerTemplate(g, l, t, r, b, color, new String[]{
                "11111111",
                "10000000",
                "11101101",
                "11011100",
                "11000000",
                "11010000",
                "10000000",
                "10000000"
        });
    }

    private static void drawEpicReferenceCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawCornerTemplate(g, l, t, r, b, color, new String[]{
                "111111",
                "101000",
                "111110",
                "110100",
                "111000",
                "110000"
        });
    }

    private static void drawUncommonReferenceCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawCornerTemplate(g, l, t, r, b, color, new String[]{
                "1111111",
                "1000000",
                "1111011",
                "1101100",
                "1110000",
                "1101000",
                "1100000"
        });
    }

    private static void drawCornerTemplate(GuiGraphics g, int l, int t, int r, int b,
                                           int color, String[] rows) {
        int templateWidth = rows[0].length();
        int templateHeight = rows.length;
        for (int y = 0; y < templateHeight; y++) {
            for (int x = 0; x < templateWidth; x++) {
                if (rows[y].charAt(x) != '1') continue;
                pixel(g, l + x, t + y, color);
                pixel(g, r - x - 1, t + y, color);
                pixel(g, l + x, b - y, color);
                pixel(g, r - x - 1, b - y, color);
            }
        }
    }

    /** Legendary画像だけ、鉤を二重にして外周上へ小さな切れ目を加える。 */
    private static void drawLegendaryCornerMarks(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawLegendaryReferenceCorners(g, l, t, r, b, color);
    }

    /**
     * 参考画像の枠。直線を四隅まで伸ばさず、角そのものを階段状の折れ線で構成する。
     */
    private static void drawReferenceFrame(GuiGraphics g, int l, int t, int r, int b, int color) {
        int width = r - l;
        int height = b - t;
        h(g, l + 4, t, Math.max(0, width - 8), color);
        h(g, l + 4, b - 1, Math.max(0, width - 8), color);
        v(g, l, t + 4, Math.max(0, height - 8), color);
        v(g, r - 1, t + 4, Math.max(0, height - 8), color);

        drawCircuitCorner(g, l, t, 1, 1, color);
        drawCircuitCorner(g, r - 1, t, -1, 1, color);
        drawCircuitCorner(g, l, b - 1, 1, -1, color);
        drawCircuitCorner(g, r - 1, b - 1, -1, -1, color);
    }

    /** 元画像の金枠。同じ外周に、角付近だけ短い二本目の回路を追加する。 */
    private static void drawLegendaryReferenceFrame(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawReferenceCornerMarks(g, l, t, r, b, color);
        drawInnerCorner(g, l, t, 1, 1, color);
        drawInnerCorner(g, r - 1, t, -1, 1, color);
        drawInnerCorner(g, l, b - 1, 1, -1, color);
        drawInnerCorner(g, r - 1, b - 1, -1, -1, color);
        pixel(g, l + 7, t + 1, color); pixel(g, r - 8, t + 1, color);
        pixel(g, l + 7, b - 2, color); pixel(g, r - 8, b - 2, color);
    }

    private static void drawCircuitCorner(GuiGraphics g, int x, int y, int sx, int sy, int color) {
        // GUIスケールで巨大化しない、画像同等の3px角。
        line(g, x + sx, y, x + sx, y + sy * 2, color);
        line(g, x + sx, y + sy * 2, x + sx * 3, y + sy * 2, color);
        line(g, x + sx * 3, y + sy * 2, x + sx * 3, y + sy * 3, color);
        line(g, x, y + sy * 3, x + sx * 3, y + sy * 3, color);
    }

    private static void drawInnerCorner(GuiGraphics g, int x, int y, int sx, int sy, int color) {
        line(g, x + sx * 4, y + sy, x + sx * 6, y + sy, color);
        line(g, x + sx * 4, y + sy, x + sx * 4, y + sy * 2, color);
    }

    /** edgeY と dy で上下反転できる3px高の属性紋様。 */
    private static void drawElementGlyph(GuiGraphics g, ElementType type, int cx, int edgeY, int dy) {
        int color = elementColor(type);
        int dim = (color & 0xFF000000) | (((color >> 16) & 0xFF) / 2 << 16)
                | (((color >> 8) & 0xFF) / 2 << 8) | ((color & 0xFF) / 2);
        switch (type) {
            case ICE -> { // 六方向へ尖る氷晶
                pixel(g, cx, edgeY + dy * 3, color);
                h(g, cx - 2, edgeY + dy * 2, 5, color);
                pixel(g, cx, edgeY + dy, color);
                pixel(g, cx - 3, edgeY + dy * 3, dim);
                pixel(g, cx + 3, edgeY + dy * 3, dim);
            }
            case FIRE -> { // 高さの違う三つの炎先
                pixel(g, cx - 3, edgeY + dy, dim);
                pixel(g, cx - 2, edgeY + dy * 2, color);
                pixel(g, cx, edgeY + dy * 3, color);
                pixel(g, cx + 2, edgeY + dy * 2, color);
                pixel(g, cx + 3, edgeY + dy, dim);
            }
            case WATER -> { // 二連の波
                pixel(g, cx - 4, edgeY + dy, color); pixel(g, cx - 3, edgeY + dy * 2, color);
                h(g, cx - 2, edgeY + dy, 3, color);
                pixel(g, cx + 1, edgeY + dy * 2, color); h(g, cx + 2, edgeY + dy, 3, color);
            }
            case ELECTRIC -> { // 細い二重スパーク
                pixel(g, cx - 3, edgeY + dy * 3, color); pixel(g, cx - 2, edgeY + dy * 2, color);
                pixel(g, cx - 1, edgeY + dy, color); pixel(g, cx + 1, edgeY + dy * 3, dim);
                pixel(g, cx + 2, edgeY + dy * 2, color); pixel(g, cx + 3, edgeY + dy, color);
            }
            case THUNDER -> { // 太い稲妻
                h(g, cx - 2, edgeY + dy * 3, 4, color);
                h(g, cx - 1, edgeY + dy * 2, 3, color);
                h(g, cx - 2, edgeY + dy, 3, dim);
            }
            case WIND -> { // 流れる三本線
                h(g, cx - 4, edgeY + dy, 7, color);
                h(g, cx - 2, edgeY + dy * 2, 7, dim);
                pixel(g, cx + 4, edgeY + dy * 3, color);
            }
            case HOLY -> { // 光輪と中央の光
                h(g, cx - 3, edgeY + dy * 2, 7, color);
                pixel(g, cx, edgeY + dy * 3, 0xFFFFFFFF);
                pixel(g, cx, edgeY + dy, color);
            }
            case DARK -> { // 欠けた月
                h(g, cx - 2, edgeY + dy * 3, 4, color);
                pixel(g, cx - 3, edgeY + dy * 2, color);
                h(g, cx - 2, edgeY + dy, 3, dim);
            }
            case CORROSION -> { // 垂れる侵食液
                h(g, cx - 4, edgeY + dy, 9, color);
                pixel(g, cx - 2, edgeY + dy * 2, dim); pixel(g, cx + 1, edgeY + dy * 3, color);
                pixel(g, cx + 3, edgeY + dy * 2, dim);
            }
            case MIASMA -> { // 不揃いな瘴気泡
                pixel(g, cx - 4, edgeY + dy, dim); pixel(g, cx - 2, edgeY + dy * 3, color);
                pixel(g, cx, edgeY + dy, color); pixel(g, cx + 2, edgeY + dy * 2, dim);
                pixel(g, cx + 4, edgeY + dy * 3, color);
            }
            case BLOOD -> { // 三つの血滴
                pixel(g, cx - 3, edgeY + dy * 2, dim); pixel(g, cx, edgeY + dy * 3, color);
                pixel(g, cx + 3, edgeY + dy * 2, dim); h(g, cx - 1, edgeY + dy, 3, color);
            }
            case ERASURE -> { // 途切れた虚無の線
                h(g, cx - 5, edgeY + dy * 2, 3, color); pixel(g, cx, edgeY + dy * 3, color);
                h(g, cx + 2, edgeY + dy, 3, dim);
            }
            case SOUL -> { // 左右へ揺れる魂火
                pixel(g, cx - 2, edgeY + dy, dim); pixel(g, cx - 1, edgeY + dy * 2, color);
                pixel(g, cx, edgeY + dy * 3, color); pixel(g, cx + 1, edgeY + dy * 2, color);
                pixel(g, cx + 2, edgeY + dy, dim);
            }
            case SOUL_FIRE -> { // 青い魂炎を二山にする
                pixel(g, cx - 3, edgeY + dy, dim); pixel(g, cx - 2, edgeY + dy * 3, color);
                pixel(g, cx, edgeY + dy, color); pixel(g, cx + 2, edgeY + dy * 3, color);
                pixel(g, cx + 3, edgeY + dy, dim);
            }
            case NONE -> { }
        }
    }

    private static int elementColor(ElementType type) {
        return switch (type) {
            case ICE -> 0xFF9DEBFF;
            case ELECTRIC -> 0xFF5FFFF2;
            case CORROSION -> 0xFF86B82B;
            case HOLY -> 0xFFFFF2A8;
            case DARK -> 0xFF7652B8;
            case FIRE -> 0xFFFF5A24;
            case WIND -> 0xFFB8F2D0;
            case THUNDER -> 0xFFFFE135;
            case WATER -> 0xFF3D9BFF;
            case MIASMA -> 0xFF9A50A8;
            case BLOOD -> 0xFFD41432;
            case ERASURE -> 0xFFB8A6D9;
            case SOUL -> 0xFF63D7FF;
            case SOUL_FIRE -> 0xFF28AFFF;
            case NONE -> 0x00000000;
        };
    }

    private static void drawSquareCorners(GuiGraphics g, int l, int t, int r, int b,
                                          int color, int length, boolean stepped) {
        h(g, l, t, length, color);          v(g, l, t, length, color);
        h(g, r - length, t, length, color); v(g, r - 1, t, length, color);
        h(g, l, b - 1, length, color);      v(g, l, b - length, length, color);
        h(g, r - length, b - 1, length, color); v(g, r - 1, b - length, length, color);
        if (stepped) {
            h(g, l + 2, t + 1, length - 1, color); v(g, l + 2, t + 1, 3, color);
            h(g, r - length - 1, t + 1, length - 1, color); v(g, r - 3, t + 1, 3, color);
            h(g, l + 2, b - 2, length - 1, color); v(g, l + 2, b - 3, 3, color);
            h(g, r - length - 1, b - 2, length - 1, color); v(g, r - 3, b - 3, 3, color);
        }
    }

    /** Common は Hypixel のメニュー枠のような、淡色で一段欠けた角だけにする。 */
    private static void drawCommonCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        // 左上 / 右上
        h(g, l + 4, t, 7, color);       v(g, l, t + 4, 7, color);
        h(g, l + 1, t + 1, 4, color);   v(g, l + 2, t + 1, 3, color);
        h(g, r - 11, t, 7, color);      v(g, r - 1, t + 4, 7, color);
        h(g, r - 5, t + 1, 4, color);   v(g, r - 3, t + 1, 3, color);

        // 左下 / 右下（上側を反転したブラケット）
        h(g, l + 4, b - 1, 7, color);   v(g, l, b - 11, 7, color);
        h(g, l + 1, b - 2, 4, color);   v(g, l + 2, b - 3, 3, color);
        h(g, r - 11, b - 1, 7, color);  v(g, r - 1, b - 11, 7, color);
        h(g, r - 5, b - 2, 4, color);   v(g, r - 3, b - 3, 3, color);
    }

    /** Uncommon / Rare / Epic 共通の、画像に合わせた細い回路状コーナー。 */
    private static void drawHypixelCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        // 参考画像と同じ、一重枠へ小さな「コ」の字を噛ませた角。
        // 内側の縦線は本文開始位置 (l + 3) より外に置く。
        h(g, l, t, 10, color);              v(g, l, t, 7, color);
        h(g, r - 10, t, 10, color);         v(g, r - 1, t, 7, color);
        h(g, l, b - 1, 10, color);          v(g, l, b - 7, 7, color);
        h(g, r - 10, b - 1, 10, color);     v(g, r - 1, b - 7, 7, color);

        h(g, l + 1, t + 1, 5, color);       v(g, l + 2, t + 1, 3, color);
        h(g, r - 6, t + 1, 5, color);       v(g, r - 3, t + 1, 3, color);
        h(g, l + 1, b - 2, 5, color);       v(g, l + 2, b - 3, 3, color);
        h(g, r - 6, b - 2, 5, color);       v(g, r - 3, b - 3, 3, color);
    }

    /** Hypixel の金枠を意識した二重の直角迷路模様。 */
    private static void drawLegendaryCorners(GuiGraphics g, int l, int t, int r, int b, int color) {
        drawReferenceFrame(g, l, t, r, b, color);
        // 金枠だけは参考画像の二重回路を、文字領域へ入らない横線として追加。
        h(g, l + 4, t + 1, 8, color);       h(g, r - 12, t + 1, 8, color);
        h(g, l + 4, b - 2, 8, color);       h(g, r - 12, b - 2, 8, color);
        // 四隅から少し離れた小さな切れ目が、画像の回路状アクセントになる。
        h(g, l + 13, t, 2, color); h(g, r - 15, t, 2, color);
        h(g, l + 13, b - 1, 2, color); h(g, r - 15, b - 1, 2, color);
    }

    /** Forbidden 専用。赤黒い蔓と棘を枠内2pxの帯だけへ収める。 */
    private static void drawThornFrame(GuiGraphics g, int l, int t, int r, int b,
                                       int color, int shadow) {
        // 暗い蔓を枠のすぐ内側へ添えて太さとねじれを出す。
        h(g, l, t + 1, r - l, shadow);      h(g, l, b - 2, r - l, shadow);
        v(g, l + 1, t, b - t, shadow);      v(g, r - 2, t, b - t, shadow);
        drawReferenceCornerMarks(g, l, t, r, b, color);

        // 上下の三角棘。先端も境界内に留める。
        for (int x = l + 7; x < r - 7; x += 9) {
            pixel(g, x, t, color);
            pixel(g, x + 1, t + 1, shadow);
            pixel(g, x + 3, b - 2, shadow);
            pixel(g, x + 4, b - 1, color);
        }
        // 左右も同じ2px帯で交互に尖らせる。
        for (int y = t + 7; y < b - 7; y += 9) {
            pixel(g, l, y, color);
            pixel(g, l + 1, y + 1, shadow);
            pixel(g, r - 2, y + 3, shadow);
            pixel(g, r - 1, y + 4, color);
        }

        // 四隅の節も矩形からはみ出さない。
        h(g, l, t, 5, color);               v(g, l, t, 5, color);
        h(g, r - 5, t, 5, color);           v(g, r - 1, t, 5, color);
        h(g, l, b - 1, 5, color);           v(g, l, b - 5, 5, color);
        h(g, r - 5, b - 1, 5, color);       v(g, r - 1, b - 5, 5, color);
    }

    private static void h(GuiGraphics g, int x, int y, int length, int color) {
        if (length > 0) g.fill(x, y, x + length, y + 1, color);
    }

    private static void v(GuiGraphics g, int x, int y, int length, int color) {
        if (length > 0) g.fill(x, y, x + 1, y + length, color);
    }

    private static void pixel(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }

    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (y1 == y2) {
            int from = Math.min(x1, x2);
            h(g, from, y1, Math.abs(x2 - x1) + 1, color);
        } else if (x1 == x2) {
            int from = Math.min(y1, y2);
            v(g, x1, from, Math.abs(y2 - y1) + 1, color);
        }
    }

    private static FrameColors colorsFor(WeaponRarity rarity) {
        return switch (rarity) {
            case COMMON -> new FrameColors(0xFFCBE8F7, 0xFF91B7CC);
            case UNCOMMON -> new FrameColors(0xFF35FF50, 0xFF20D83D);
            case RARE -> new FrameColors(0xFF419BFF, 0xFF2879D8);
            case EPIC -> new FrameColors(0xFFC02FFF, 0xFF9320D8);
            case LEGENDARY -> new FrameColors(0xFFFFB000, 0xFFE56A00);
            case FORBIDDEN -> new FrameColors(0xFFFF3B3B, 0xFF62000D);
        };
    }

    private record FrameColors(int top, int bottom) {
    }
}
