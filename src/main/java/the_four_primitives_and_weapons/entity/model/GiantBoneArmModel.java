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
	/** 侵食属性で骨から生えるガラス結晶 (ガラスパスのみ描画)。 */
	private final java.util.List<ModelPart> shards = new java.util.ArrayList<>();

	/** ガラス結晶が「びっしり」になる侵食レベル。これ以降は結晶サイズが増す。 */
	private static final float GLASS_MAX_LEVEL_MODEL = 12f;
	/** Lv12 以降、1レベルごとの結晶サイズ増加量。 */
	private static final float GLASS_GROWTH_PER_LV = 0.018f;
	/** 結晶サイズ増加の上限 (Lv100でも大きくなりすぎず見やすく保つ)。 */
	private static final float GLASS_GROWTH_MAX = 0.9f;
	/** 全ガラス結晶の配置定義 (骨表面に細かくびっしり)。 */
	private static final java.util.List<ShardDef> SHARD_DEFS = buildShardDefs();

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

	/** 指の曲がる向き。-1=下向き / +1=上向き (上下逆ならここを反転)。 */
	private static final float CURL_SIGN = -1f;

	// 薙ぎ払い: 右(SWEEP_FROM)→左(SWEEP_TO) へ。進行が逆なら 2値を入れ替え。
	private static final float SWEEP_FROM = 1.1f;
	private static final float SWEEP_TO = -1.1f;
	/** 薙ぎ払い中のロール (前腕軸まわり)。手のひらを左へ向ける。逆向きなら符号反転。 */
	private static final float SWEEP_ROLL = (float) (Math.PI / 2);

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
		for (ShardDef d : SHARD_DEFS) {
			ModelPart parent = d.parent.equals("humerus") ? humerus
					: d.parent.equals("forearm") ? forearm : hand;
			shards.add(parent.getChild(d.name));
		}
	}

	/** ガラス結晶の表示切替 (骨パス=false / ガラスパス=true)。 */
	public void setShardsVisible(boolean visible) {
		for (ModelPart s : shards)
			s.visible = visible;
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

		// 侵食属性のガラス結晶 (骨表面から外向きに細かくびっしり生やす)。
		for (ShardDef d : SHARD_DEFS) {
			PartDefinition parent = d.parent.equals("humerus") ? humerus
					: d.parent.equals("forearm") ? forearm : hand;
			addShard(parent, d.name, d.px, d.py, d.pz, d.rx, d.ry, d.rz, d.size, d.len);
		}

		// 骨ブロック柄を 128x128 にタイル。全 box が texOffs(0,0) 起点で領域内に収まる。
		return LayerDefinition.create(mesh, 128, 128);
	}

	/** 骨表面から突き出るガラス結晶 (角ばった = ガラスブロック状)。local +Z 方向へ伸びる。 */
	private static void addShard(PartDefinition parent, String name,
								 float px, float py, float pz,
								 float rx, float ry, float rz, float size, float len) {
		parent.addOrReplaceChild(name,
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-size / 2, -size / 2, 0f, size, size, len)              // 基部ブロック
						.texOffs(0, 0).addBox(-size * 0.34f, -size * 0.34f, len, size * 0.68f, size * 0.68f, len * 0.55f), // 段
				PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
	}

	/** ガラス結晶 1個の配置定義。 */
	private static final class ShardDef {
		final String parent, name;
		final float px, py, pz, rx, ry, rz, size, len;

		ShardDef(String parent, String name, float px, float py, float pz,
				 float rx, float ry, float rz, float size, float len) {
			this.parent = parent;
			this.name = name;
			this.px = px;
			this.py = py;
			this.pz = pz;
			this.rx = rx;
			this.ry = ry;
			this.rz = rz;
			this.size = size;
			this.len = len;
		}
	}

	/** 骨表面に不規則に結晶を散らす配置を生成 (固定seedで毎回同じ)。 */
	private static java.util.List<ShardDef> buildShardDefs() {
		java.util.List<ShardDef> defs = new java.util.ArrayList<>();
		java.util.Random rnd = new java.util.Random(20240601L);
		// (parent, z範囲, 半径, 本数, size最小/最大[太さ], len最小/最大[長さ])
		scatter(defs, rnd, "humerus", 4f, LH - 3f, 3.0f, 26, 1.1f, 2.8f, 2.4f, 5.2f);
		scatter(defs, rnd, "forearm", 3f, LF - 2f, 3.0f, 20, 1.0f, 2.4f, 2.2f, 4.6f);
		scatter(defs, rnd, "hand", 2f, LP - 0.5f, 3.0f, 8, 1.0f, 2.2f, 2.0f, 4.0f);
		return defs;
	}

	/** 骨に沿って結晶をランダム散布 (位置・周方向角・太さ・長さ・捻りを全てばらつかせる)。 */
	private static void scatter(java.util.List<ShardDef> defs, java.util.Random rnd, String parent,
							   float z0, float z1, float r, int count,
							   float minSize, float maxSize, float minLen, float maxLen) {
		for (int i = 0; i < count; i++) {
			float z = z0 + rnd.nextFloat() * (z1 - z0);          // 骨に沿って不規則
			// 周方向は「側面のみ」(上面/下面 ±Y からは生やさない)。
			// 左右どちらかの水平方向 ± SIDE_SPREAD の範囲に限定。
			boolean leftSide = rnd.nextBoolean();
			double spread = (rnd.nextFloat() - 0.5) * Math.toRadians(80.0); // ±40°
			double ang = (leftSide ? Math.PI : 0.0) + spread;
			float rr = r * (0.8f + rnd.nextFloat() * 0.5f);      // 半径ばらつき
			float px = (float) (Math.cos(ang) * rr);
			float py = (float) (Math.sin(ang) * rr);
			// 外向きへ傾ける (周方向角から) + ばらつき
			float rx = (float) (Math.sin(ang) * 1.3) + (rnd.nextFloat() - 0.5f) * 0.5f;
			float ry = (float) (Math.cos(ang) * 1.3) + (rnd.nextFloat() - 0.5f) * 0.5f;
			float rz = (rnd.nextFloat() - 0.5f) * 1.2f;          // ランダムな捻り
			float size = minSize + rnd.nextFloat() * (maxSize - minSize); // 太さ不規則
			float len = minLen + rnd.nextFloat() * (maxLen - minLen);     // 長さ不規則
			defs.add(new ShardDef(parent, parent + "_s" + i, px, py, z, rx, ry, rz, size, len));
		}
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
		// 通常は上腕骨・前腕も描画する (ガードで切り替えるため毎回戻す)。
		humerus.skipDraw = false;
		forearm.skipDraw = false;

		float age = ageInTicks; // = tickCount + partialTick

		// ガードモード: 包み込み姿勢だけ駆動して終了 (薙ぎ/叩きは無し)。
		if (entity.isGuard()) {
			setupGuardPose(entity, age);
			return;
		}

		float slamTilt = entity.getSlamTilt(); // 地形に合わせた接地角 (− が下)。手が地表に乗る。

		// --- フェーズ判定 ---
		float grow;     // 0..1 腕の出現/伸び
		float sweepYaw; // root yRot (薙ぎ払い)
		float armPitch; // root xRot (振り上げ/叩き)
		float roll;     // root zRot (前腕軸まわりのロール = 手のひらの向き)
		float curl;     // 指の握り込み 0..1
		float flatten = 0f; // 接地で手のひらが平らに広がる 0..1

		if (age < GiantBoneArmEntity.GROW_END) {
			float p = age / GiantBoneArmEntity.GROW_END;
			grow = easeOut(p);
			sweepYaw = SWEEP_FROM;        // 右に構える
			armPitch = -0.05f * grow;     // ほぼ水平
			roll = SWEEP_ROLL * grow;     // 手のひらを左へ向けながら出現
			curl = 0.35f;
		} else if (age < GiantBoneArmEntity.SWEEP_END) {
			// 薙ぎ払い: 半握り・手のひら左・水平に 右→左
			float p = (age - GiantBoneArmEntity.GROW_END)
					/ (GiantBoneArmEntity.SWEEP_END - GiantBoneArmEntity.GROW_END);
			grow = 1f;
			sweepYaw = Mth.lerp(easeInOut(p), SWEEP_FROM, SWEEP_TO);
			armPitch = -0.05f + 0.06f * (float) Math.sin(p * Math.PI); // 水平維持
			roll = SWEEP_ROLL;            // 手のひらを左へ
			curl = 0.35f;                 // 中途半端な握り
		} else if (age < GiantBoneArmEntity.SLAM_END) {
			float pp = (age - GiantBoneArmEntity.SWEEP_END)
					/ (float) (GiantBoneArmEntity.SLAM_END - GiantBoneArmEntity.SWEEP_END);
			grow = 1f;
			// まず素早く正面・手のひら下へ戻し、縦振り(上→下)を綺麗に見せる
			float unroll = easeOut(Math.min(pp * 3f, 1f));
			sweepYaw = Mth.lerp(unroll, SWEEP_TO, 0f);
			roll = Mth.lerp(unroll, SWEEP_ROLL, 0f);
			// 当たり判定 (SLAM_IMPACT) の瞬間に手が地表 (slamTilt) へ来るタイミング
			float impactPP = (float) (GiantBoneArmEntity.SLAM_IMPACT - GiantBoneArmEntity.SWEEP_END)
					/ (GiantBoneArmEntity.SLAM_END - GiantBoneArmEntity.SWEEP_END);
			float raiseEnd = impactPP * 0.5f;
			// + が上 / − が下。上へ振り上げ → 下へ叩き込み。
			if (pp < raiseEnd) {
				armPitch = Mth.lerp(easeOut(pp / raiseEnd), -0.05f, 1.5f); // 真上へ振り上げ
			} else if (pp < impactPP) {
				armPitch = Mth.lerp(easeIn((pp - raiseEnd) / (impactPP - raiseEnd)), 1.5f, slamTilt); // 上→下 (地表へ)
			} else {
				armPitch = slamTilt; // 着地保持 (地形に追従)
			}
			curl = 0.05f; // パー (開いた手で叩きつけ)
			// 接地(impact)後、数tickで手のひらが平らに広がる
			flatten = Mth.clamp((pp - impactPP) / 0.12f, 0f, 1f);
		} else {
			float p = (age - GiantBoneArmEntity.SLAM_END)
					/ (GiantBoneArmEntity.LIFETIME - GiantBoneArmEntity.SLAM_END);
			grow = 1f - easeIn(p);
			sweepYaw = 0f;
			roll = 0f;
			armPitch = Mth.lerp(easeIn(p), slamTilt, 0.5f); // 接地から上へ引き上げて消える
			curl = 0.1f;
			flatten = 1f - easeIn(p); // 引き上げると平らが解ける
		}

		// root は yaw(薙ぎ) と pitch(振り上げ/叩き) のみ。ロールは手首に持たせ、
		// 薙ぎの移動が縦に化けないよう「腕の移動は純粋に水平/垂直」に保つ。
		root.yRot = sweepYaw;
		root.xRot = armPitch;
		root.zRot = 0f;
		float s = 0.05f + 0.95f * grow;
		root.xScale = s;
		root.yScale = s;
		root.zScale = s;

		// 関節の曲げ (手首も指と同じ向き = 下)。手のひらの向きは手首ロールで。
		forearm.xRot = -0.1f - 0.1f * (1f - grow);
		// 接地時は手首を起こして手のひらを地面と平行に押し付ける (接地角ぶん起こす)
		hand.xRot = Mth.lerp(flatten, CURL_SIGN * (0.05f + 0.25f * curl), -slamTilt);
		hand.zRot = roll; // 薙ぎ中は手のひらを左へ (腕の移動方向には影響しない)

		// 指: 曲がる側を下 (CURL_SIGN)。curl で握り込み、パー時はほぼ伸びる。
		// flatten で接地時に「少しだけ平らに」: 扇を広げ、指/爪先をわずかに反らす。
		for (int i = 0; i < 5; i++) {
			float c = curl * (i == 0 ? 0.7f : 1f);
			fingerBase[i].yRot = SPREAD[i] * (1f - 0.4f * c) * (1f + 0.6f * flatten); // 接地で扇を広げる
			// 接地時: 基節は平ら、先へ行くほど上へ反らして末節骨/鉤爪を地中に潜らせない。
			fingerBase[i].xRot = Mth.lerp(flatten, CURL_SIGN * ((i == 0 ? 0.2f : 0.15f) + 1.0f * c), 0f);
			fingerMid[i].xRot = Mth.lerp(flatten, CURL_SIGN * (0.15f + 1.0f * c), -CURL_SIGN * 0.03f);
			fingerTip[i].xRot = Mth.lerp(flatten, CURL_SIGN * (0.1f + 0.9f * c), -CURL_SIGN * 0.10f); // 末節骨をわずかに上へ
			fingerClaw[i].xRot = Mth.lerp(flatten, CURL_SIGN * (0.1f + 0.5f * c), -CURL_SIGN * 0.15f); // 鉤爪をわずかに上へ
		}

		applyShardScale(entity, grow);
	}

	/** ガラス結晶: Lv12 で base サイズ (びっしり)、以降サイズ増。出現(grow)に合わせて生える。 */
	private void applyShardScale(GiantBoneArmEntity entity, float grow) {
		int lv = entity.getCorrosionLevel();
		float baseFrac = Mth.clamp(lv / GLASS_MAX_LEVEL_MODEL, 0f, 1f);          // 0..1 (12で1)
		float extra = Math.min(Math.max(0f, lv - GLASS_MAX_LEVEL_MODEL) * GLASS_GROWTH_PER_LV,
				GLASS_GROWTH_MAX); // 12以降の追加 (上限あり)
		float shardScale = (baseFrac + extra) * grow;
		for (ModelPart sh : shards) {
			sh.xScale = shardScale;
			sh.yScale = shardScale;
			sh.zScale = shardScale;
		}
	}

	/**
	 * ガード姿勢: プレイヤーを上から包み、指を大きく広げて囲い込んでから握る。
	 * 全体の向き(腕を下へ向ける等)は描画側で行い、ここは関節と握りだけを駆動する。
	 */
	private void setupGuardPose(GiantBoneArmEntity entity, float age) {
		float life = GiantBoneArmEntity.GUARD_LIFETIME;
		float appear = Mth.clamp(age / 5f, 0f, 1f);                 // 出現
		float retract = Mth.clamp((age - (life - 5f)) / 5f, 0f, 1f); // 終盤に開いて消える
		float grow = appear * (1f - retract);
		float grip = Mth.clamp((age - 3f) / 5f, 0f, 1f) * (1f - retract); // 棒を握るように握り込む

		// 上腕骨・前腕は描かず、手＋指だけを見せる (指の骨が肋骨に見えるように)。
		humerus.skipDraw = true;
		forearm.skipDraw = true;

		root.xRot = 0f;
		root.yRot = 0f;
		root.zRot = 0f;
		float s = 0.05f + 0.95f * grow;
		root.xScale = s;
		root.yScale = s;
		root.zScale = s;

		// 肋骨(あばら): 前腕は前面中央へ向け、指を扇状に広げて各指を緩やかな弧に。
		// 左右の手の指が中央で合わさり、湾曲した骨が肋骨のように並ぶ。
		forearm.xRot = -0.05f;
		forearm.yRot = 0.25f;          // 手を前面中央へ少し向ける
		hand.xRot = CURL_SIGN * 0.2f;  // 手のひらをプレイヤーへ
		hand.yRot = 0f;
		hand.zRot = 0f;

		// 指を扇状に広げ(肋骨の間隔)、各節を一定量曲げて弧(=肋骨)を作る。
		float[] ribFan = {1.4f, 0.6f, 0.2f, -0.2f, -0.6f};
		for (int i = 0; i < 5; i++) {
			float c = grip;
			fingerBase[i].yRot = ribFan[i];
			fingerBase[i].xRot = CURL_SIGN * (0.35f + 0.25f * c);
			fingerMid[i].xRot = CURL_SIGN * (0.40f + 0.30f * c);
			fingerTip[i].xRot = CURL_SIGN * (0.40f + 0.30f * c);
			fingerClaw[i].xRot = CURL_SIGN * (0.35f + 0.25f * c);
		}

		applyShardScale(entity, grow);
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
