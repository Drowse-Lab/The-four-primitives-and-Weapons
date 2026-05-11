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
	 * 壁ラック用 8 ポーズ。45° 刻みで全方向回転。
	 *   pose 0:   90° (水平、柄が右)
	 *   pose 1:   45° (右上斜め)
	 *   pose 2:    0° (縦、刃が上)
	 *   pose 3:  -45° (左上斜め)
	 *   pose 4:  -90° (水平ミラー、柄が左)
	 *   pose 5: -135° (左下斜め)
	 *   pose 6:  180° (縦、刃が下、傘立て差し込み)
	 *   pose 7:  135° (右下斜め)
	 */
	private static final float[] WALL_POSE_Z_ROT = { 90f, 45f, 0f, -45f, -90f, -135f, 180f, 135f };

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
			// 木の種類に応じてバリアントモデルを選択 (oak はベースモデルを再利用)。
			String wood = entity.getWoodName();
			boolean isOak = wood.equals("oak");
			ResourceLocation modelRL;
			if (facing == Direction.DOWN) {
				modelRL = isOak ? MODEL_CHAIN
					: new ResourceLocation(MODID, "block/weapon_rack_chain_" + wood);
			} else if (facing == Direction.UP) {
				modelRL = isOak ? MODEL_STAND
					: new ResourceLocation(MODID, "block/weapon_rack_stand_" + wood);
			} else {
				modelRL = isOak ? MODEL_HOOK
					: new ResourceLocation(MODID, "block/weapon_rack_hook_" + wood);
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

		boolean horizontalWallRack = facing.getAxis().isHorizontal();
		boolean floorRack = facing == Direction.UP;
		float zFightOffset = Mth.getSeed(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()) * 0.00000000000000001f;

		// 床ラック・壁ラックは 2 スロット (slot 1 / slot 2)。天井ラックは slot 1 のみ。
		ItemStack stack2 = entity.getItem2();
		if (floorRack) {
			if (!stack.isEmpty()) {
				poseStack.pushPose();
				renderItemForFloor(entity, stack, true, rotation, zFightOffset, poseStack, buffer, packedLight);
				poseStack.popPose();
			}
			if (!stack2.isEmpty()) {
				poseStack.pushPose();
				renderItemForFloor(entity, stack2, false, rotation, zFightOffset, poseStack, buffer, packedLight);
				poseStack.popPose();
			}
		} else if (horizontalWallRack) {
			if (!stack.isEmpty()) {
				poseStack.pushPose();
				renderItemForWall(entity, stack, true, rotation, invisible, zFightOffset, poseStack, buffer, packedLight);
				poseStack.popPose();
			}
			if (!stack2.isEmpty()) {
				poseStack.pushPose();
				renderItemForWall(entity, stack2, false, rotation, invisible, zFightOffset, poseStack, buffer, packedLight);
				poseStack.popPose();
			}
		} else if (!stack.isEmpty()) {
			// 天井ラック (DOWN): 1 スロット
			float zRot = 135f;
			float scale = 1.0f;
			if (stack.is(BIG_WEAPONS)) scale = 1.2f;
			if (stack.is(RANGED_WEAPONS)) zRot = 45f;
			if (stack.is(SHIELDS)) { scale = 1.3f; zRot = 0f; }
			if (stack.is(TRIDENTS)) zRot = -45f;

			// Blockbench で編集可能なスロットマーカーから位置を読み取る
			WeaponRackSlotLoader.SlotPos slotPos = WeaponRackSlotLoader.get("weapon_rack", "slot1");
			float ox, oy, oz;
			if (slotPos != null) {
				ox = slotPos.x - 0.5f;
				oy = slotPos.y - 0.5f;
				oz = slotPos.z - 0.5f;
			} else {
				ox = 0.0f;
				oy = 0.125f;
				oz = 0.0625f;
			}

			poseStack.translate(ox, oy, oz);
			poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0F / 8.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
			poseStack.translate(zFightOffset, zFightOffset, zFightOffset);

			// XYZ 1:1.5:1 (少し縦長)
			poseStack.scale(scale, scale * 1.2f, scale);

			this.itemRenderer.renderStatic(
				stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, OverlayTexture.NO_OVERLAY,
				poseStack, buffer, entity.level(), entity.getId()
			);
		}

		poseStack.popPose();
	}

	/** 床ラック (sawhorse) で 1 つのアイテムを描画。slot1=true なら左、false なら右側にオフセット。 */
	private void renderItemForFloor(WeaponRackEntity entity, ItemStack stack, boolean isSlot1,
									int rotation, float zFightOffset,
									PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		float scale = 1.0f;
		if (stack.is(BIG_WEAPONS)) scale = 1.2f;
		if (stack.is(SHIELDS)) scale = 1.3f;

		// Blockbench で編集可能なスロットマーカーから位置を読み取る (デフォルトフォールバック付き)
		WeaponRackSlotLoader.SlotPos slotPos = WeaponRackSlotLoader.get(
			"weapon_rack_stand", isSlot1 ? "slot1" : "slot2");
		float ox, oy, oz;
		if (slotPos != null) {
			ox = slotPos.x - 0.5f;
			oy = slotPos.y - 0.5f;
			oz = slotPos.z - 0.5f;
		} else {
			ox = isSlot1 ? -0.155f : +0.155f;
			oy = 0.062f;
			oz = 0.0f;
		}
		// 回転軸をブロック中心 (pose origin = 真ん中) に。
		// 順序: 回転 → 移動 (= 中心で回ってから marker 位置にずらす)
		poseStack.mulPose(FLOOR_POSES[rotation % FLOOR_POSES.length]);
		poseStack.translate(ox + zFightOffset, oy + zFightOffset, oz + zFightOffset);

		// XYZ 1:1.5:1 (少し縦長)
		poseStack.scale(scale, scale * 1.5f, scale);

		this.itemRenderer.renderStatic(
			stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, OverlayTexture.NO_OVERLAY,
			poseStack, buffer, entity.level(), entity.getId()
		);
	}

	/** 壁ラックで 1 つのアイテムを描画。slot1/slot2 ともに同じ向き (= 互いに平行 / 壁にも平行)、
	 *  位置だけが左右で違う。rotation スロット (entity.getRotation()) は使わない。 */
	private void renderItemForWall(WeaponRackEntity entity, ItemStack stack, boolean isSlot1,
								   int rotation, boolean invisible, float zFightOffset,
								   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		float scale = 1.0f;
		if (stack.is(BIG_WEAPONS)) scale = 1.2f;
		if (stack.is(SHIELDS)) scale = 1.3f;

		float baseRot = WALL_POSE_Z_ROT[Math.floorMod(rotation, WALL_POSE_Z_ROT.length)];
		float zRot = isSlot1 ? -baseRot : baseRot;

		// Blockbench で編集可能なスロットマーカーから位置を読み取る (見つからなければデフォルト)
		// block model と weapon は同じ pose に描画されるので、marker (model x,y,z in 1/16単位)
		// → pose offset = ((x - 8)/16, (y - 8)/16, (z - 8)/16) で同じ視覚位置になる。
		WeaponRackSlotLoader.SlotPos slotPos = WeaponRackSlotLoader.get(
			"weapon_rack_hook", isSlot1 ? "slot1" : "slot2");
		float ox, oy, oz;
		if (slotPos != null) {
			ox = slotPos.x - 0.5f;
			oy = slotPos.y - 0.5f;
			oz = slotPos.z - 0.5f;
		} else {
			ox = isSlot1 ? -0.25f : +0.25f;
			oy = 0.0f;
			oz = 0.1875f;
		}

		poseStack.translate(ox, oy, oz);
		poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
		poseStack.translate(zFightOffset, zFightOffset, zFightOffset);

		// XYZ 1:1.5:1 (少し縦長)
		poseStack.scale(scale, scale * 1.5f, scale);

		this.itemRenderer.renderStatic(
			stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, OverlayTexture.NO_OVERLAY,
			poseStack, buffer, entity.level(), entity.getId()
		);
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
