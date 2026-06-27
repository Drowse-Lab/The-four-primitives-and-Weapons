package the_four_primitives_and_weapons.entity.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import the_four_primitives_and_weapons.entity.GiantBoneArmEntity;

/**
 * ガード専用「肋骨の籠(ドーム)」モデル。
 * 背骨(後ろ)から左右に弧を描く肋骨を高さ違いで並べ、プレイヤーを囲う骨の籠を作る。
 * 腕モデルの流用ではなく、籠の形を直接組むので「守っている」が明確に出る。
 */
public class BoneCageModel extends HierarchicalModel<GiantBoneArmEntity> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("the_four_primitives_and_weapons", "bone_cage"), "main");

	private static final float R = 13f;               // 籠の半径 (モデル単位)
	private static final float[] RIB_Y = {-14f, -6f, 2f, 10f}; // 肋骨の高さ
	private static final int RIB_SEG = 5;             // 1本の肋骨の節数
	private static final float SEG_LEN = 6.0f;        // 1節の長さ

	private final ModelPart root;

	public BoneCageModel(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 背骨 (後ろ・縦の柱)
		root.addOrReplaceChild("spine",
				CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -18f, -R - 1f, 5f, 38f, 5f),
				PartPose.ZERO);
		// 上下の閉じ (頭頂・足元の小さな冠)
		root.addOrReplaceChild("crown_top",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -20f, -4f, 8f, 4f, 8f),
				PartPose.ZERO);
		root.addOrReplaceChild("crown_bot",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4f, 16f, -4f, 8f, 4f, 8f),
				PartPose.ZERO);

		// 肋骨: 各高さで左右に、背骨から前面へ弧を描く
		for (int i = 0; i < RIB_Y.length; i++) {
			addRib(root, "rib_r" + i, +1, RIB_Y[i]);
			addRib(root, "rib_l" + i, -1, RIB_Y[i]);
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	/** 背骨(後ろ)から側面を通って前面へ弧を描く肋骨1本 (節の連鎖)。 */
	private static void addRib(PartDefinition root, String name, int side, float y) {
		float t = 2.6f;
		float dYaw = side * (float) Math.toRadians(34); // 1節ごとの曲がり
		// 背骨(z=-R)から開始、まず側面方向へ向ける
		PartDefinition cur = root.addOrReplaceChild(name,
				ribBox(t, SEG_LEN),
				PartPose.offsetAndRotation(side * 1.5f, y, -R + 1f, 0f, side * (float) Math.toRadians(28), 0f));
		for (int k = 1; k < RIB_SEG; k++) {
			cur = cur.addOrReplaceChild(name + "_" + k,
					ribBox(t * (1f - 0.05f * k), SEG_LEN),
					PartPose.offsetAndRotation(0f, 0f, SEG_LEN, 0f, dYaw, 0f));
		}
	}

	/** 肋骨1節 (骨身 + 関節コブ)。local +Z へ伸びる。 */
	private static CubeListBuilder ribBox(float t, float len) {
		float kt = t + 0.8f;
		return CubeListBuilder.create()
				.texOffs(0, 0).addBox(-t / 2, -t / 2, 0f, t, t, len)
				.texOffs(0, 0).addBox(-kt / 2, -kt / 2, -0.9f, kt, kt, 2.0f); // 関節コブ
	}

	@Override
	public void setupAnim(GiantBoneArmEntity entity, float limbSwing, float limbSwingAmount,
						  float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		float age = ageInTicks;
		float life = GiantBoneArmEntity.GUARD_LIFETIME;
		float appear = Mth.clamp(age / 4f, 0f, 1f);                  // せり上がり出現
		float retract = Mth.clamp((age - (life - 5f)) / 5f, 0f, 1f); // 終盤に消える
		float grow = appear * (1f - retract);
		root.xScale = grow;
		root.yScale = grow;
		root.zScale = grow;
	}

	@Override
	public ModelPart root() {
		return this.root;
	}
}
