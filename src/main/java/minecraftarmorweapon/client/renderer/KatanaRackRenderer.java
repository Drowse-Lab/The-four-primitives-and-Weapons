package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 刀掛け（カタナラック）を直接 VertexConsumer で描画する。
 * テクスチャはバニラの oak_planks をスタンドアローンで参照。
 * 形状: ベース板 + 左右2本の柱（柱の上に剣が水平に乗る）
 */
public class KatanaRackRenderer {

	private static final ResourceLocation OAK_PLANKS_TEX =
		new ResourceLocation("minecraft", "textures/block/oak_planks.png");

	private static final float P = 1.0F / 16.0F; // 1ピクセル = 1/16ブロック

	public static void render(PoseStack ps, MultiBufferSource buffer, int packedLight) {
		VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(OAK_PLANKS_TEX));
		Matrix4f m = ps.last().pose();
		Matrix3f n = ps.last().normal();

		// ベース板: 14p × 1p × 4p（地面に置かれる）
		cube(vc, m, n, packedLight, -7 * P, 0, -2 * P, 7 * P, 1 * P, 2 * P);
		// 左の柱: 2p × 6p × 2p
		cube(vc, m, n, packedLight, -6 * P, 1 * P, -1 * P, -4 * P, 7 * P, 1 * P);
		// 右の柱
		cube(vc, m, n, packedLight, 4 * P, 1 * P, -1 * P, 6 * P, 7 * P, 1 * P);
	}

	private static void cube(VertexConsumer vc, Matrix4f m, Matrix3f n, int light,
							 float x1, float y1, float z1,
							 float x2, float y2, float z2) {
		// north (-Z)
		face(vc, m, n, light, x1, y2, z1, x1, y1, z1, x2, y1, z1, x2, y2, z1, 0, 0, -1);
		// south (+Z)
		face(vc, m, n, light, x2, y2, z2, x2, y1, z2, x1, y1, z2, x1, y2, z2, 0, 0, 1);
		// east (+X)
		face(vc, m, n, light, x2, y2, z1, x2, y1, z1, x2, y1, z2, x2, y2, z2, 1, 0, 0);
		// west (-X)
		face(vc, m, n, light, x1, y2, z2, x1, y1, z2, x1, y1, z1, x1, y2, z1, -1, 0, 0);
		// up (+Y)
		face(vc, m, n, light, x1, y2, z2, x1, y2, z1, x2, y2, z1, x2, y2, z2, 0, 1, 0);
		// down (-Y)
		face(vc, m, n, light, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, 0, -1, 0);
	}

	private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, int light,
							 float ax, float ay, float az,
							 float bx, float by, float bz,
							 float cx, float cy, float cz,
							 float dx, float dy, float dz,
							 float nx, float ny, float nz) {
		v(vc, m, n, ax, ay, az, 0f, 0f, nx, ny, nz, light);
		v(vc, m, n, bx, by, bz, 0f, 1f, nx, ny, nz, light);
		v(vc, m, n, cx, cy, cz, 1f, 1f, nx, ny, nz, light);
		v(vc, m, n, dx, dy, dz, 1f, 0f, nx, ny, nz, light);
	}

	private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n,
						  float x, float y, float z, float u, float v,
						  float nx, float ny, float nz, int light) {
		vc.vertex(m, x, y, z)
			.color(255, 255, 255, 255)
			.uv(u, v)
			.overlayCoords(OverlayTexture.NO_OVERLAY)
			.uv2(light)
			.normal(n, nx, ny, nz)
			.endVertex();
	}
}
