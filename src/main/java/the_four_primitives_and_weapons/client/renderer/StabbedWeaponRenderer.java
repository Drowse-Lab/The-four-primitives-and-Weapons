package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;
import the_four_primitives_and_weapons.init.KnifeExtrasRegistrar;

/**
 * 地面に突き刺さった武器のレンダラー。 刃を下に向けて少し傾けて表示する。
 */
public class StabbedWeaponRenderer extends EntityRenderer<StabbedWeaponEntity> {

	// 見た目調整用 ( 必要なら数値を変える )。 傾きはエンティティのプリセット値を使う。
	public static float SINK = 0.35f;    // 地面へのめり込み量
	public static float SCALE = 1.5f;    // 杭の大きさ ( 刺した時のサイズはそのまま )

	public StabbedWeaponRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(StabbedWeaponEntity entity, float entityYaw, float partialTick,
	                   PoseStack pose, MultiBufferSource buffer, int packedLight) {
		ItemStack stack = entity.getItem();
		if (!stack.isEmpty()) {
			pose.pushPose();
			// 設置時の向き ( プレイヤーの向き )
			pose.mulPose(Axis.YP.rotationDegrees(entity.getStabYaw()));
			// フル回転: 傾き(pitch=X) → ロール+反転(Z)。 yaw は上の YP で適用済み。
			pose.mulPose(Axis.XP.rotationDegrees(entity.getTilt()));
			pose.mulPose(Axis.ZP.rotationDegrees(180f + entity.getRoll()));
			// 地面へ沈める
			pose.translate(0.0, -0.5 + SINK, 0.0);
			pose.scale(SCALE, SCALE, SCALE);
			// THIRD_PERSON_RIGHT_HAND: 元の比率・立体感を保つ ( FIXED は比率が崩れる )
			Minecraft.getInstance().getItemRenderer().renderStatic(
					stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
					packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
			pose.popPose();
		}

		// 編集ツール ( 戦地設営の杭 ) を持っている時だけ、 判定範囲を表す球ギズモを表示。
		Minecraft mc = Minecraft.getInstance();
		Object stakeItem = KnifeExtrasRegistrar.BATTLE_STAKE.get();
		if (mc.player != null
				&& (mc.player.getMainHandItem().getItem() == stakeItem
				|| mc.player.getOffhandItem().getItem() == stakeItem)) {
			drawSphereGizmo(pose, buffer, entity.getRadius());
		}

		super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
	}

	/** 立体回転ギズモ: 3 軸を色分けしたリング ( 赤=X / 緑=Y / 青=Z ) ＋ 高さ矢印。 */
	private static void drawSphereGizmo(PoseStack pose, MultiBufferSource buffer, float r) {
		VertexConsumer vc = buffer.getBuffer(RenderType.lines());
		Matrix4f mat = pose.last().pose();
		Matrix3f nrm = pose.last().normal();
		int seg = 36;
		for (int i = 0; i < seg; i++) {
			double a0 = (Math.PI * 2 * i) / seg;
			double a1 = (Math.PI * 2 * (i + 1)) / seg;
			float c0 = (float) Math.cos(a0) * r, s0 = (float) Math.sin(a0) * r;
			float c1 = (float) Math.cos(a1) * r, s1 = (float) Math.sin(a1) * r;
			// X 軸リング ( YZ 平面, x=0 ) = 赤
			line(vc, mat, nrm, 0, c0, s0, 0, c1, s1, 1.0f, 0.3f, 0.3f, 0.9f);
			// Y 軸リング ( XZ 平面, y=0 ) = 緑
			line(vc, mat, nrm, c0, 0, s0, c1, 0, s1, 0.3f, 1.0f, 0.3f, 0.9f);
			// Z 軸リング ( XY 平面, z=0 ) = 青
			line(vc, mat, nrm, c0, s0, 0, c1, s1, 0, 0.4f, 0.5f, 1.0f, 0.9f);
		}
		// 3 軸の矢印 ( Blockbench 配色: X=赤 / Y=緑 / Z=青 ) — どの軸かを分かりやすく
		float ax = r + 0.45f;
		axisArrow(vc, mat, nrm, ax, 0, 0, 1.0f, 0.3f, 0.3f); // X 赤
		axisArrow(vc, mat, nrm, 0, ax, 0, 0.3f, 1.0f, 0.3f); // Y 緑
		axisArrow(vc, mat, nrm, 0, 0, ax, 0.4f, 0.5f, 1.0f); // Z 青

		// 高さ矢印 ( 縦, 黄 ) — 上下に動かせることを示す
		float top = r + 0.45f;
		line(vc, mat, nrm, 0, -top, 0, 0, top, 0, 1.0f, 0.95f, 0.2f, 0.95f);
		line(vc, mat, nrm, 0, top, 0, -0.15f, top - 0.2f, 0, 1.0f, 0.95f, 0.2f, 0.95f);
		line(vc, mat, nrm, 0, top, 0, 0.15f, top - 0.2f, 0, 1.0f, 0.95f, 0.2f, 0.95f);
	}

	/** 原点から ( ex,ey,ez ) への軸矢印 ( 先端に矢じり )。 */
	private static void axisArrow(VertexConsumer vc, Matrix4f mat, Matrix3f nrm,
	                              float ex, float ey, float ez, float r, float g, float b) {
		line(vc, mat, nrm, 0, 0, 0, ex, ey, ez, r, g, b, 0.95f);
		// 矢じり ( 軸方向に応じて簡易的に2本 )
		float h = 0.18f;
		if (ex != 0) {
			line(vc, mat, nrm, ex, ey, ez, ex - h, ey + h, ez, r, g, b, 0.95f);
			line(vc, mat, nrm, ex, ey, ez, ex - h, ey - h, ez, r, g, b, 0.95f);
		} else if (ey != 0) {
			line(vc, mat, nrm, ex, ey, ez, ex + h, ey - h, ez, r, g, b, 0.95f);
			line(vc, mat, nrm, ex, ey, ez, ex - h, ey - h, ez, r, g, b, 0.95f);
		} else {
			line(vc, mat, nrm, ex, ey, ez, ex, ey + h, ez - h, r, g, b, 0.95f);
			line(vc, mat, nrm, ex, ey, ez, ex, ey - h, ez - h, r, g, b, 0.95f);
		}
	}

	private static void line(VertexConsumer vc, Matrix4f mat, Matrix3f nrm,
	                         float x0, float y0, float z0, float x1, float y1, float z1,
	                         float r, float g, float b, float a) {
		vc.vertex(mat, x0, y0, z0).color(r, g, b, a).normal(nrm, 0, 1, 0).endVertex();
		vc.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(nrm, 0, 1, 0).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(StabbedWeaponEntity entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
