package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.joml.Quaternionf;

import minecraftarmorweapon.entity.WeaponRackEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Arsenal の WeaponRackEntityRenderer を Forge 1.20.1 用に移植。
 * 木製ラックのブロックモデルを描画 + 持っているアイテムを FIXED 表示で大きく描画。
 * タグごとに scale と Z回転を変える:
 *   big_weapons   : scale 1.6
 *   shields       : scale 1.8, zRot 0
 *   ranged_weapons: zRot 45
 *   tridents      : zRot -45
 *   default       : scale 0.85, zRot 135
 */
public class WeaponRackRenderer extends EntityRenderer<WeaponRackEntity> {

	private static final String MODID = "minecraft_armor_weapon";

	/** 天井から吊り下げ (DOWN face): チェーン+両端トリップワイヤーフック */
	public static final ResourceLocation MODEL_CHAIN =
		new ResourceLocation(MODID, "block/weapon_rack");
	/** 壁設置 (横方向 face): トリップワイヤーフック単体 */
	public static final ResourceLocation MODEL_HOOK =
		new ResourceLocation(MODID, "block/weapon_rack_hook");
	/** 床設置 (UP face): A-frame スタンド */
	public static final ResourceLocation MODEL_STAND =
		new ResourceLocation(MODID, "block/weapon_rack_stand");
	/** pk_racks 系ラック (KawaMood, CC BY-NC-SA 4.0): 8×8×8 head 形状を skin で変えるバリアント。
	 * pale_oak は 1.20.1 に存在しないため除外。 */
	public static final ResourceLocation[] MODEL_PK_VARIANTS = {
		new ResourceLocation(MODID, "block/weapon_rack_pk_oak"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_spruce"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_birch"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_jungle"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_acacia"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_dark_oak"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_mangrove"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_cherry"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_bamboo"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_crimson"),
		new ResourceLocation(MODID, "block/weapon_rack_pk_warped"),
	};
	/** 後方互換 */
	public static final ResourceLocation MODEL_PK_OAK = MODEL_PK_VARIANTS[0];
	/** 後方互換: 古い MODEL 参照用 */
	public static final ResourceLocation MODEL = MODEL_CHAIN;

	public static final TagKey<Item> BIG_WEAPONS =
		TagKey.create(net.minecraft.core.registries.Registries.ITEM,
			new ResourceLocation(MODID, "big_weapons"));
	public static final TagKey<Item> RANGED_WEAPONS =
		TagKey.create(net.minecraft.core.registries.Registries.ITEM,
			new ResourceLocation(MODID, "ranged_weapons"));
	public static final TagKey<Item> SHIELDS =
		TagKey.create(net.minecraft.core.registries.Registries.ITEM,
			new ResourceLocation(MODID, "shields"));
	public static final TagKey<Item> TRIDENTS =
		TagKey.create(net.minecraft.core.registries.Registries.ITEM,
			new ResourceLocation(MODID, "tridents"));

	/**
	 * pk_racks (KawaMood) の ground rack 用 sword/tool 6 ポーズ。
	 * Quaternion (x, y, z, w) は pk_racks のデータパックから移植。
	 * シフト+空手で右クリックで 0→1→...→5→0 と循環。
	 */
	public static final Quaternionf[] FLOOR_POSES = {
		new Quaternionf(0.653f, 0.271f,  0.653f,  0.271f),
		new Quaternionf(0.854f, 0.354f,  0.354f,  0.146f),
		new Quaternionf(0.854f, 0.354f, -0.354f, -0.146f),
		new Quaternionf(0.653f, 0.271f, -0.653f, -0.271f),
		new Quaternionf(0.354f, 0.146f, -0.854f, -0.354f),
		new Quaternionf(0.354f, 0.146f,  0.854f,  0.354f),
	};

	/**
	 * 壁ラック用 4 ポーズ。zRot を変えるだけのシンプル版。
	 *   pose 0: 水平 (左右の腕に乗る)
	 *   pose 1: 右肩下がり (右の腕に立てかけ)
	 *   pose 2: 左肩下がり (左の腕に立てかけ)
	 *   pose 3: 縦 (両腕の隙間に縦差し込み)
	 */
	private static final float[] WALL_POSE_Z_ROT = { 90f, 45f, 135f, 0f };

	private final ItemRenderer itemRenderer;
	private final BlockRenderDispatcher blockRenderer;

	public WeaponRackRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.itemRenderer = ctx.getItemRenderer();
		this.blockRenderer = ctx.getBlockRenderDispatcher();
	}

	@Override
	public void render(WeaponRackEntity entity, float yaw, float partialTick,
					   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
		poseStack.pushPose();

		Direction facing = entity.getDirection();
		Vec3 offset = this.getRenderOffset(entity, partialTick);
		poseStack.translate(-offset.x(), -offset.y(), -offset.z());

		double d = 0.46875D;
		poseStack.translate(facing.getStepX() * d, facing.getStepY() * d, facing.getStepZ() * d);
		// UP face (床立ち) では pitch=-90 を適用すると A-frame が倒れてしまうのでスキップ
		if (facing != Direction.UP) {
			poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
		}
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

		boolean invisible = entity.isInvisible();
		ItemStack stack = entity.getItem();
		int rotation = entity.getRotation();

		// vanilla ItemFrameRenderer と同様、ブロックモデルは回転スロットの影響を受けず固定配置。
		// 回転スロット (rotation slot) はアイテム表示にのみ適用する。
		if (!invisible) {
			// 天井 (DOWN) → チェーン版 / 床 (UP) → A-frame スタンド / 壁 (横) → 壁掛けフック
			ResourceLocation modelRL;
			if (facing == Direction.DOWN) {
				modelRL = MODEL_CHAIN;
			} else if (facing == Direction.UP) {
				modelRL = MODEL_STAND;
			} else {
				modelRL = MODEL_HOOK;
			}
			poseStack.pushPose();
			poseStack.translate(-0.5F, -0.5F, -0.5F);
			this.blockRenderer.getModelRenderer().renderModel(
				poseStack.last(),
				buffer.getBuffer(Sheets.cutoutBlockSheet()),
				null,
				this.blockRenderer.getBlockModelShaper().getModelManager().getModel(modelRL),
				1.0F, 1.0F, 1.0F,
				packedLight,
				OverlayTexture.NO_OVERLAY
			);
			poseStack.popPose();
		}

		if (!stack.isEmpty()) {
			// 武器の種類別の scale / zRot デフォルト
			//   default       : zRot 135°, scale 0.85
			//   big_weapons   : scale 1.6
			//   ranged_weapons: zRot 45°
			//   shields       : scale 1.8, zRot 0°
			//   tridents      : zRot -45°
			float zRot = 135f;
			float scale = 0.85f;
			if (stack.is(BIG_WEAPONS)) {
				scale = 1.6f;
			}
			if (stack.is(RANGED_WEAPONS)) {
				zRot = 45f;
			}
			if (stack.is(SHIELDS)) {
				scale = 1.8f;
				zRot = 0f;
			}
			if (stack.is(TRIDENTS)) {
				zRot = -45f;
			}

			boolean horizontalWallRack = facing.getAxis().isHorizontal();
			boolean floorRack = facing == Direction.UP;

			// rotation スロットを pk_racks 風の pose index として使う
			// (シフト+右クリックで cycle される)
			int pose = rotation;
			float zFightOffset = Mth.getSeed(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()) * 0.00000000000000001f;

			if (floorRack) {
				// 床ラック: pk_racks の 6 ポーズ用 quaternion を直接適用。
				// pk_racks の left position translation [0.155, 0.168, 0] のうち X は左右オフセットなので、
				// 単一武器の中心配置では Y のみ採用 (0.168)。
				poseStack.translate(zFightOffset, 0.168f + zFightOffset, zFightOffset);
				poseStack.mulPose(FLOOR_POSES[pose % FLOOR_POSES.length]);
			} else if (horizontalWallRack) {
				// 壁ラック: pose index で zRot を上書き (default 135° の剣のみ)
				if (zRot == 135f) {
					zRot = WALL_POSE_Z_ROT[pose % WALL_POSE_Z_ROT.length];
				}
				// 斜め腕の上端 (model y≈14) に剣が乗るように持ち上げる
				poseStack.translate(0F, 0.375F, 0F);
				poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
				if (invisible) {
					poseStack.translate(zFightOffset, zFightOffset, 0.4375F + zFightOffset);
				} else {
					poseStack.translate(zFightOffset, zFightOffset, 0.3F + zFightOffset);
				}
			} else {
				// 天井ラック (DOWN): デフォルト動作 (rotation slot を ZP に直接適用)
				poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0F / 8.0F));
				poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
				if (invisible) {
					poseStack.translate(zFightOffset, zFightOffset, 0.4375F + zFightOffset);
				} else {
					poseStack.translate(zFightOffset, zFightOffset, 0.3F + zFightOffset);
				}
			}

			poseStack.scale(scale, scale, scale);

			this.itemRenderer.renderStatic(
				stack,
				ItemDisplayContext.FIXED,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				poseStack,
				buffer,
				entity.level(),
				entity.getId()
			);
		}

		poseStack.popPose();
	}

	@Override
	public Vec3 getRenderOffset(WeaponRackEntity entity, float partialTick) {
		return new Vec3(
			entity.getDirection().getStepX() * 0.3D,
			-0.25D,
			entity.getDirection().getStepZ() * 0.3D
		);
	}

	@Override
	public ResourceLocation getTextureLocation(WeaponRackEntity entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	protected boolean shouldShowName(WeaponRackEntity entity) {
		return false;
	}
}
