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

	private static final float R = 10f;                       // 籠の半径 (モデル単位)
	private static final float[] RIB_Y = {-13f, -5f, 4f, 13f}; // 肋骨の高さ
	private static final int RIB_SEG = 8;                     // 半周あたりの節数

	private final ModelPart root;

	public BoneCageModel(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 背骨 (脊柱): 椎骨を縦に積み、後ろへ棘突起を出す。
		for (int v = -18; v <= 18; v += 4) {
			root.addOrReplaceChild("vert" + (v + 18),
					CubeListBuilder.create()
							.texOffs(0, 0).addBox(-3f, v - 1.7f, -R - 1f, 6f, 3.4f, 6f)   // 椎体
							.texOffs(0, 0).addBox(-1.5f, v - 1.1f, -R - 4f, 3f, 2.2f, 3f), // 棘突起(後ろへ)
					PartPose.ZERO);
		}
		// 肋骨: 各高さで左右の半周を「円周上に節を並べて」描く → 確実にプレイヤーを囲う輪。
		for (int i = 0; i < RIB_Y.length; i++) {
			addRib(root, "rib_r" + i, +1, RIB_Y[i]);
			addRib(root, "rib_l" + i, -1, RIB_Y[i]);
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	/** 半径 R の円周(前→側面→後ろ)に節を並べて半周の肋骨を作る。side で左右。 */
	private static void addRib(PartDefinition root, String name, int side, float y) {
		float t = 2.4f;
		float segLen = (float) (Math.PI * R / RIB_SEG) * 1.25f; // 弧長/節 + 重なり
		for (int k = 0; k < RIB_SEG; k++) {
			double theta = Math.PI * (k + 0.5) / RIB_SEG; // 0(前)..π(後)
			float x = (float) (side * R * Math.sin(theta));
			float z = (float) (R * Math.cos(theta));
			float yaw = (float) Math.atan2(side * R * Math.cos(theta), -R * Math.sin(theta)); // 接線
			root.addOrReplaceChild(name + k,
					ribBox(t, segLen),
					PartPose.offsetAndRotation(x, y, z, 0f, yaw, 0f));
		}
	}

	/** 肋骨1節 (骨身 + 関節コブ)。中心を原点に local +Z 方向へ伸びる。 */
	private static CubeListBuilder ribBox(float t, float len) {
		float kt = t + 0.7f;
		return CubeListBuilder.create()
				.texOffs(0, 0).addBox(-t / 2, -t / 2, -len / 2, t, t, len)
				.texOffs(0, 0).addBox(-kt / 2, -kt / 2, -1f, kt, kt, 2f); // 関節コブ
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
