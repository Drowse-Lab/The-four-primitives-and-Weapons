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
 * 「巨骨の腕」モデル — 餓者髑髏 (がしゃどくろ) 風の、痩せ細った巨大な骸骨の腕。
 *   root
 *    └ humerus (上腕骨: 細い骨身 + 大きな骨頭 + 三角筋粗面のコブ)
 *        └ forearm (前腕: 尺骨 + 肘頭 + 橈骨)
 *            └ hand (手根骨 + 中手骨)
 *                └ finger0..4 (各 基節→中節→末節→鉤爪。関節ごとに骨頭コブ)
 *
 * 骨は long軸 = +Z。指は長く鉤爪状で、安静時から軽く握り込んだ威圧的な姿勢。
 * アニメは tickCount 由来のフェーズ (GROW→SWEEP→SLAM→RETRACT) で手続き駆動。
 */
public class GiantBoneArmModel extends HierarchicalModel<GiantBoneArmEntity> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("the_four_primitives_and_weapons", "giant_bone_arm"), "main");

	private final ModelPart root;
	private final ModelPart humerus;
	private final ModelPart forearm;
	private final ModelPart hand;
	private final ModelPart[] fingerBase = new ModelPart[5];
	private final ModelPart[] fingerMid = new ModelPart[5];
	private final ModelPart[] fingerTip = new ModelPart[5];
	private final ModelPart[] fingerClaw = new ModelPart[5];

	private static final float LH = 30f; // 上腕骨 長さ
	private static final float LF = 26f; // 前腕 長さ
	private static final float LP = 10f; // 手 (手根+中手) 長さ

	// 指の各節長 (基節, 中節, 末節) — 餓者髑髏らしく長い
	private static final float[][] SEG = {
			{7f, 6f, 3f},   // 0 母指
			{9f, 8f, 5f},   // 1 示指
			{10f, 9f, 6f},  // 2 中指
			{9f, 8f, 5f},   // 3 環指
			{7f, 6f, 4f},   // 4 小指
	};
	private static final float[] CLAW = {5f, 5f, 6f, 5f, 4f}; // 鉤爪 長さ
	private static final float[] THICK = {2.6f, 2.4f, 2.6f, 2.4f, 2.0f};
	private static final float[] FX = {-6.5f, -4.5f, -1.5f, 1.5f, 4.5f};
	// 安静時の指の開き (扇状)
	private static final float[] SPREAD = {-1.0f, -0.32f, 0f, 0.3f, 0.58f};

	public GiantBoneArmModel(ModelPart root) {
		this.root = root;
		this.humerus = root.getChild("humerus");
		this.forearm = humerus.getChild("forearm");
		this.hand = forearm.getChild("hand");
		for (int i = 0; i < 5; i++) {
			this.fingerBase[i] = hand.getChild("finger" + i);
			this.fingerMid[i] = fingerBase[i].getChild("mid" + i);
			this.fingerTip[i] = fingerMid[i].getChild("tip" + i);
			this.fingerClaw[i] = fingerTip[i].getChild("claw" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 上腕骨: 細い骨身 + 球状の骨頭(肩/肘) + 三角筋粗面のコブ
		PartDefinition humerus = root.addOrReplaceChild("humerus",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-2.5f, -2.5f, 0f, 5f, 5f, LH)        // 骨身
						.texOffs(0, 0).addBox(-4f, -4f, -4f, 8f, 8f, 8f)           // 肩側 骨頭(球)
						.texOffs(0, 0).addBox(2.0f, -1.5f, LH * 0.35f, 3f, 3f, 7f) // 三角筋粗面のコブ
						.texOffs(0, 0).addBox(-4.5f, -4.5f, LH - 5f, 9f, 9f, 8f),  // 肘側 骨頭
				PartPose.offset(0f, 0f, 0f));

		// 前腕: 尺骨(+肘頭) + 橈骨
		PartDefinition forearm = humerus.addOrReplaceChild("forearm",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-3.8f, -2f, 0f, 3f, 4f, LF)         // 尺骨
						.texOffs(0, 0).addBox(-3.8f, -3.5f, -4f, 3f, 5f, 5f)      // 肘頭
						.texOffs(0, 0).addBox(0.8f, -1.8f, 0f, 3f, 3.6f, LF)      // 橈骨
						.texOffs(0, 0).addBox(-4.5f, -3f, LF - 4f, 9f, 6f, 6f),   // 手首側 骨頭
				PartPose.offset(0f, 0f, LH - 1f));

		// 手: 細い手根骨 + 中手骨5本 (痩せた骸骨の手の甲)
		CubeListBuilder handCubes = CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6f, -2f, 0f, 12f, 4f, 4f); // 手根骨ブロック
		for (int i = 0; i < 5; i++) {
			float mt = THICK[i] * 0.8f;
			handCubes.texOffs(0, 0).addBox(FX[i] - mt / 2, -1.2f, 4f, mt, mt, LP - 4f); // 中手骨
			handCubes.texOffs(0, 0).addBox(FX[i] - 1.6f, -0.2f, LP - 2f, 3.2f, 3.2f, 3.2f); // 拳の関節コブ
		}
		PartDefinition hand = forearm.addOrReplaceChild("hand",
				handCubes, PartPose.offset(0f, 0f, LF - 1f));

		// 指 5本: 基節→中節→末節→鉤爪。関節ごとに骨頭コブ。
		for (int i = 0; i < 5; i++) {
			float t = THICK[i];
			float baseLen = SEG[i][0];
			float midLen = SEG[i][1];
			float tipLen = SEG[i][2];
			float clawLen = CLAW[i];

			PartDefinition fb = hand.addOrReplaceChild("finger" + i,
					boneWithKnuckle(t, baseLen),
					PartPose.offset(FX[i], 1.2f, LP));

			PartDefinition fm = fb.addOrReplaceChild("mid" + i,
					boneWithKnuckle(t * 0.88f, midLen),
					PartPose.offset(0f, 0f, baseLen));

			PartDefinition ft = fm.addOrReplaceChild("tip" + i,
					boneWithKnuckle(t * 0.74f, tipLen),
					PartPose.offset(0f, 0f, midLen));

			// 鉤爪: 細く尖った末端 (下向きに反る)
			float ct = t * 0.55f;
			ft.addOrReplaceChild("claw" + i,
					CubeListBuilder.create()
							.texOffs(0, 0).addBox(-ct / 2, -ct / 2, 0f, ct, ct, clawLen * 0.6f)
							.texOffs(0, 0).addBox(-ct / 3, -ct / 3, clawLen * 0.6f, ct * 0.66f, ct * 0.66f, clawLen * 0.4f),
					PartPose.offsetAndRotation(0f, 0f, tipLen, 0.6f, 0f, 0f)); // 反り
		}

		// 骨ブロック柄を 128x128 にタイル。全 box が texOffs(0,0) 起点で領域内に収まる。
		return LayerDefinition.create(mesh, 128, 128);
	}

	/** 関節の骨頭コブ付きの骨セグメント (z=0 に小コブ, z+ に骨身)。 */
	private static CubeListBuilder boneWithKnuckle(float t, float len) {
		float kt = t + 0.8f;
		return CubeListBuilder.create()
				.texOffs(0, 0).addBox(-kt / 2, -kt / 2, -1.2f, kt, kt, 2.6f) // 関節コブ
				.texOffs(0, 0).addBox(-t / 2, -t / 2, 0f, t, t, Math.max(len, 0.01f)); // 骨身
	}

	@Override
	public void setupAnim(GiantBoneArmEntity entity, float limbSwing, float limbSwingAmount,
						  float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		float age = ageInTicks; // = tickCount + partialTick

		// --- フェーズ判定 ---
		float grow;     // 0..1 腕の出現/伸び
		float sweepYaw; // root yRot (薙ぎ払い)
		float armPitch; // root xRot (振り上げ/叩き)
		float curl;     // 指の握り込み 0..1

		if (age < GiantBoneArmEntity.GROW_END) {
			float p = age / GiantBoneArmEntity.GROW_END;
			grow = easeOut(p);
			sweepYaw = -1.1f;
			armPitch = -0.25f * grow;
			curl = 0.25f;
		} else if (age < GiantBoneArmEntity.SWEEP_END) {
			float p = (age - GiantBoneArmEntity.GROW_END)
					/ (GiantBoneArmEntity.SWEEP_END - GiantBoneArmEntity.GROW_END);
			grow = 1f;
			sweepYaw = Mth.lerp(easeInOut(p), -1.1f, 1.1f);
			armPitch = -0.25f + 0.1f * (float) Math.sin(p * Math.PI);
			curl = 0.2f; // 薙ぎ払いは指を開いて引っ掻く
		} else if (age < GiantBoneArmEntity.SLAM_END) {
			float pp = (age - GiantBoneArmEntity.SWEEP_END)
					/ (float) (GiantBoneArmEntity.SLAM_END - GiantBoneArmEntity.SWEEP_END);
			grow = 1f;
			sweepYaw = Mth.lerp(easeOut(Math.min(pp * 1.5f, 1f)), 1.1f, 0f);
			float raiseEnd = (float) (GiantBoneArmEntity.SLAM_IMPACT - GiantBoneArmEntity.SWEEP_END)
					/ (GiantBoneArmEntity.SLAM_END - GiantBoneArmEntity.SWEEP_END);
			if (pp < raiseEnd) {
				armPitch = Mth.lerp(easeOut(pp / raiseEnd), -0.25f, -1.4f);
				curl = 0.35f;
			} else {
				armPitch = Mth.lerp(easeIn((pp - raiseEnd) / (1f - raiseEnd)), -1.4f, 1.15f);
				curl = 0.7f; // 叩き込みで握り締める
			}
		} else {
			float p = (age - GiantBoneArmEntity.SLAM_END)
					/ (GiantBoneArmEntity.LIFETIME - GiantBoneArmEntity.SLAM_END);
			grow = 1f - easeIn(p);
			sweepYaw = 0f;
			armPitch = Mth.lerp(easeIn(p), 1.15f, -0.4f);
			curl = 0.7f;
		}

		// root に薙ぎ/振りを適用 + 出現スケール
		root.yRot = sweepYaw;
		root.xRot = armPitch;
		float s = 0.05f + 0.95f * grow;
		root.xScale = s;
		root.yScale = s;
		root.zScale = s;

		// 関節の自然な曲げ
		forearm.xRot = -0.15f - 0.1f * (1f - grow);
		hand.xRot = 0.1f + 0.3f * curl;

		// 指: 安静時から軽く鉤爪状に曲げ、curl で握り込む
		for (int i = 0; i < 5; i++) {
			float c = curl * (i == 0 ? 0.7f : 1f);
			// 扇状の開き
			fingerBase[i].yRot = SPREAD[i] * (1f - 0.5f * c);
			if (i == 0)
				fingerBase[i].xRot = 0.3f + 0.7f * c; // 母指は内へ
			else
				fingerBase[i].xRot = 0.35f + 0.95f * c;
			fingerMid[i].xRot = 0.55f + 1.0f * c;
			fingerTip[i].xRot = 0.5f + 0.9f * c;
			fingerClaw[i].xRot = 0.35f + 0.5f * c; // 鉤爪の反り
		}
	}

	private static float easeOut(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return 1f - (1f - t) * (1f - t);
	}

	private static float easeIn(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return t * t;
	}

	private static float easeInOut(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
	}

	@Override
	public ModelPart root() {
		return this.root;
	}
}
