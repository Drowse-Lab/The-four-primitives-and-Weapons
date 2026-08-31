package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import the_four_primitives_and_weapons.damage.ElementDamageKind;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalParticles;
import the_four_primitives_and_weapons.entity.TargetDummyEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * ターゲットダミーの頭上表示。
 *
 * <ul>
 *   <li>名前と集計サマリ ( 累計 / hit / 平均 / DPS / DoT ) を常時表示</li>
 *   <li>被弾のたびにダメージ数値を 1 つポップアップさせ、浮かせながら消す</li>
 * </ul>
 *
 * <p>ポップアップはサーバーから同期される「被弾の通し番号」の変化で検出するので、
 * 専用のパケットは要らない。</p>
 */
public class DummyStatsRenderLayer<T extends TargetDummyEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final double MAX_RENDER_DIST_SQ = 1024.0; // 32^2
    /** ポップアップが消えるまでの tick。 */
    private static final int POPUP_LIFE = 30;
    /** 同時に出せるポップアップの数。 */
    private static final int POPUP_MAX = 12;

    /** 1 件のダメージ数値ポップアップ。 */
    private static final class Popup {
        final String text;
        final int    rgb;
        final int    birthTick;
        final float  offsetX;

        Popup(String text, int rgb, int birthTick, float offsetX) {
            this.text      = text;
            this.rgb       = rgb;
            this.birthTick = birthTick;
            this.offsetX   = offsetX;
        }
    }

    // クライアント側だけの表示状態 ( ダミーは普通 1〜数体なので軽い )
    private static final Map<UUID, Deque<Popup>> POPUPS   = new HashMap<>();
    private static final Map<UUID, Integer>      LAST_SEQ = new HashMap<>();

    public DummyStatsRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (Minecraft.getInstance().cameraEntity == null) return;
        if (entity.distanceToSqr(Minecraft.getInstance().cameraEntity) > MAX_RENDER_DIST_SQ) return;

        collectNewPopup(entity);

        Font font = Minecraft.getInstance().font;
        float top = entity.getBbHeight() + 0.6f;

        // ── 名前とサマリ ──────────────────────────────────────
        poseStack.pushPose();
        poseStack.translate(0.0, top, 0.0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.02f, -0.02f, 0.02f);
        Matrix4f matrix = poseStack.last().pose();

        drawCentered(font, matrix, buffer, packedLight, "§6ターゲットダミー", 0.0f, 0, 0xFFFFFFFF);
        String summary = entity.getSummary();
        if (summary != null && !summary.isEmpty()) {
            drawCentered(font, matrix, buffer, packedLight, summary, 0.0f, 10, 0xFFFFFFFF);
        }
        poseStack.popPose();

        // ── ダメージ数値ポップアップ ─────────────────────────
        Deque<Popup> popups = POPUPS.get(entity.getUUID());
        if (popups == null || popups.isEmpty()) return;

        int now = entity.tickCount;
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup popup = it.next();
            float age = (now - popup.birthTick) + partialTick;
            if (age >= POPUP_LIFE) {
                it.remove();
                continue;
            }
            if (age < 0.0f) age = 0.0f;

            float progress = age / POPUP_LIFE;
            int alpha = (int) (255 * (1.0f - progress * progress));   // 終盤で一気に薄くする
            if (alpha < 16) continue;

            poseStack.pushPose();
            poseStack.translate(0.0, top + 0.35 + progress * 0.9, 0.0);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);
            drawCentered(font, poseStack.last().pose(), buffer, packedLight,
                    popup.text, popup.offsetX, 0, (alpha << 24) | (popup.rgb & 0xFFFFFF));
            poseStack.popPose();
        }

        if (popups.isEmpty()) {
            POPUPS.remove(entity.getUUID());
        }
    }

    /**
     * サーバーから届いた被弾の通し番号が変わっていたら、ポップアップを 1 つ積む。
     * 初めて見たダミーでは積まない ( 同期しただけの値でいきなり数字が出ないように )。
     */
    private static void collectNewPopup(TargetDummyEntity entity) {
        UUID id = entity.getUUID();
        int seq = entity.getHitSeq();
        Integer last = LAST_SEQ.put(id, seq);
        if (last == null || last == seq) return;

        float damage = entity.getLastDamage();
        if (damage <= 0.0f) return;

        ElementType element = entity.getLastElement();
        ElementDamageKind kind = entity.getLastKind();

        String text = String.format("%.1f", damage);
        if (kind == ElementDamageKind.MAGIC) {
            text += " §d魔";
        } else if (kind == ElementDamageKind.BUILDUP) {
            text += " §2蓄";
        }

        Deque<Popup> popups = POPUPS.computeIfAbsent(id, key -> new ArrayDeque<>());
        while (popups.size() >= POPUP_MAX) {
            popups.pollFirst();
        }
        // 数字が重ならないよう左右に少し散らす
        float offsetX = (popups.size() % 2 == 0 ? 1 : -1) * (6.0f + popups.size() * 2.0f);
        popups.addLast(new Popup(text, colorOf(element), entity.tickCount, offsetX));
    }

    /** 属性の粒子色をそのまま数字の色に使う ( 暗すぎる属性は読める明るさまで持ち上げる )。 */
    private static int colorOf(ElementType type) {
        Vector3f color = ElementalParticles.colorOf(type);
        if (color == null) return 0xFFFFFF;

        float max = Math.max(color.x(), Math.max(color.y(), color.z()));
        float scale = max < 0.75f && max > 0.0f ? 0.75f / max : 1.0f;
        int r = clampByte(color.x() * scale);
        int g = clampByte(color.y() * scale);
        int b = clampByte(color.z() * scale);
        return (r << 16) | (g << 8) | b;
    }

    private static int clampByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private static void drawCentered(Font font, Matrix4f matrix, MultiBufferSource buffer,
                                     int packedLight, String text, float offsetX, int y, int argb) {
        float x = offsetX - font.width(text) / 2.0f;
        font.drawInBatch(text, x, y, argb, false, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);
    }
}
