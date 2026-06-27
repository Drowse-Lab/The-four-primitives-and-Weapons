package the_four_primitives_and_weapons.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 明治期の制服（大日本軍 / 抜刀隊 / 明治警官）共通のカスタム3D防具モデル。
 *
 * 3制服はシルエットが共通（浅いピークドキャップ＋詰襟チュニック＋スリムなズボン＋革ブーツ）なので
 * ジオメトリは共有し、 テクスチャ（getArmorTexture）だけを制服ごとに差し替える。
 *
 * のっぺり回避のため肩章(エポーレット)・袖章(カフ)・帽章(トップボタン)・前立て等の立体ディテールを付与。
 * 「すらっと」させるため body/arm/leg のインフレ量(CubeDeformation)を小さめにして体に沿わせる。
 * 帽子は浅く（頭頂に乗る程度、目の上で止まる）。 テクスチャは 128x128。
 *
 * パーツは HumanoidModel に合わせて head/body/right_arm/left_arm/right_leg/left_leg を持ち、
 * ブーツ用に right_boot/left_boot を別途用意（ズボンと靴の二重描画によるZファイト回避）。
 */
public class ModelMeijiUniform<T extends Entity> extends EntityModel<T> {

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "model_meiji_uniform"), "main");

	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart right_arm;
	public final ModelPart left_arm;
	public final ModelPart right_leg;
	public final ModelPart left_leg;
	public final ModelPart right_boot;
	public final ModelPart left_boot;

	public ModelMeijiUniform(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.right_arm = root.getChild("right_arm");
		this.left_arm = root.getChild("left_arm");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
		this.right_boot = root.getChild("right_boot");
		this.left_boot = root.getChild("left_boot");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// === 兜（浅いピークドキャップ） ===
		// 顔は出すため頭の立方体は描かず、 帽子のクラウン(浅)＋バンド＋つば＋天面ボタンのみ。
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
						// クラウン（頭頂に浅く乗る。 全体を上げて深被り回避） texOffs(0,34) : y -9.2..-6.2
						.texOffs(0, 34).addBox(-4.5F, -9.2F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
						// 鉢巻きバンド texOffs(28,50) : y -6.2..-4.7 ( 目の上で止まる = 浅い )
						.texOffs(28, 50).addBox(-4.6F, -6.2F, -4.6F, 9.0F, 1.5F, 9.0F, new CubeDeformation(0.2F))
						// つば（バンド前下端に密着させ、 前へ少しだけ突き出す） texOffs(0,50) : y -5.3..-4.3
						.texOffs(0, 50).addBox(-4.5F, -5.3F, -6.3F, 9.0F, 1.0F, 2.3F, new CubeDeformation(0.0F))
						// 天面ボタン texOffs(40,76)
						.texOffs(40, 76).addBox(-1.0F, -9.7F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// === チュニック（上着） : スリム ===
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
						.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
						// 詰襟 texOffs(36,34) : 本体表面のすぐ外側（はみ出し控えめ deform 0.3）
						.texOffs(36, 34).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 3.0F, 5.0F, new CubeDeformation(0.3F))
						// ベルト texOffs(36,42)
						.texOffs(36, 42).addBox(-4.5F, 8.5F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(0.3F))
						// 前立て（中央の立体リッジ。 前面へ少しだけ突出） texOffs(48,76)
						.texOffs(48, 76).addBox(-0.5F, 1.0F, -2.95F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// === 袖 : スリム + 肩章 + 袖口 + 白手袋 ===
		// 袖は手首(y6)で終わらせ、 手の位置に手袋キューブ。
		//   袖/肩章/袖口 = uniform テクスチャ（overlay, 非染色）
		//   手袋(glove)  = gloves テクスチャ（染色される層）  texOffs(60/76,64)
		PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
						.texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.45F)) // 袖 y-2..6
						.texOffs(0, 92).addBox(-3.5F, -2.8F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.18F))  // 肩章 (net 20x6 @0,92)
						.texOffs(0, 76).addBox(-3.4F, 4.0F, -2.4F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.18F))   // 袖口 y4..6
						.texOffs(60, 64).addBox(-3.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.4F)),  // 手袋 y6..10 (底=腕底に合わせ浮き防止)
				PartPose.offset(-5.0F, 2.0F, 0.0F));
		PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
						.texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.45F))
						.texOffs(20, 92).addBox(-1.5F, -2.8F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.18F)) // 肩章 (net 20x6 @20,92)
						.texOffs(20, 76).addBox(-1.6F, 4.0F, -2.4F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.18F))
						.texOffs(76, 64).addBox(-1.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.4F)),
				PartPose.offset(5.0F, 2.0F, 0.0F));

		// === ズボン : スリム ===
		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
						.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.4F)),
				PartPose.offset(-1.9F, 12.0F, 0.0F));
		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
						.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.4F)),
				PartPose.offset(1.9F, 12.0F, 0.0F));

		// === 革ブーツ（脛から下） ===
		PartDefinition right_boot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
						.texOffs(0, 64).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.85F)),
				PartPose.offset(-1.9F, 12.0F, 0.0F));
		PartDefinition left_boot = root.addOrReplaceChild("left_boot", CubeListBuilder.create().mirror()
						.texOffs(16, 64).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.85F)),
				PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buf, int packedLight, int packedOverlay,
							   float red, float green, float blue, float alpha) {
		head.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		right_boot.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
		left_boot.render(pose, buf, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
