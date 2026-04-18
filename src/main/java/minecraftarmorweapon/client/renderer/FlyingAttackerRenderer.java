
package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Items;

import minecraftarmorweapon.entity.FlyingAttackerEntity;

public class FlyingAttackerRenderer extends HumanoidMobRenderer<FlyingAttackerEntity, HumanoidModel<FlyingAttackerEntity>> {

	// @RotationParams(FlyingAttacker_Sword)
	public static float SWORD_YAW = 0f; // 剣Y軸回転
	public static float SWORD_PITCH = 90f; // 剣X軸回転
	public static float SWORD_ROLL = -180f; // 剣Z軸回転
	public static float SWORD_SCALE_X = 1.5f; // 剣Xサイズ
	public static float SWORD_SCALE_Y = 1.5f; // 剣Yサイズ
	public static float SWORD_SCALE_Z = 1.5f; // 剣Zサイズ
	public static float SWORD_X = 0f; // 剣X位置
	public static float SWORD_Y = 0.5f; // 剣Y位置
	public static float SWORD_Z = 0.5f; // 剣Z位置
	// @EndRotationParams

	// @RotationParams(FlyingAttacker_Projectile)
	public static float PROJ_YAW = 0f; // 発射体Y軸回転
	public static float PROJ_PITCH = 0f; // 発射体X軸回転
	public static float PROJ_ROLL = 0f; // 発射体Z軸回転
	public static float PROJ_SCALE_X = 2f; // 発射体Xサイズ
	public static float PROJ_SCALE_Y = 2f; // 発射体Yサイズ
	public static float PROJ_SCALE_Z = 2f; // 発射体Zサイズ
	public static float PROJ_X = 0f; // 発射体X位置
	public static float PROJ_Y = 0.7f; // 発射体Y位置
	public static float PROJ_Z = 0f; // 発射体Z位置
	// @EndRotationParams

	private final HumanoidModel<FlyingAttackerEntity> model;
	
	public FlyingAttackerRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.model = this.getModel();
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		// デフォルトのItemInHandLayerは追加しない（親クラスで追加されるため）
		// カスタムアイテムレイヤーを追加
		this.addLayer(new CenteredItemInHandLayer(this));
	}
	
	@Override
	protected void setupRotations(FlyingAttackerEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick) {
		super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick);
		// 腕を非表示にして、デフォルトの手持ちアイテムを隠す
		this.model.rightArm.visible = false;
		this.model.leftArm.visible = false;
	}

	@Override
	public ResourceLocation getTextureLocation(FlyingAttackerEntity entity) {
		return new ResourceLocation("minecraft_armor_weapon:textures/entities/toumei.png");
	}
	
	// 剣を中央に配置するカスタムレイヤー
	private static class CenteredItemInHandLayer extends RenderLayer<FlyingAttackerEntity, HumanoidModel<FlyingAttackerEntity>> {
		
		public CenteredItemInHandLayer(RenderLayerParent<FlyingAttackerEntity, HumanoidModel<FlyingAttackerEntity>> renderer) {
			super(renderer);
		}
		
		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FlyingAttackerEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			// エンティティの表示用アイテムを取得
			ItemStack heldItem = entity.getDisplayItem();
			
			// アイテムを持っていない場合は何も表示しない
			if (heldItem.isEmpty()) {
				return;
			}
			
			poseStack.pushPose();
			
			// アイテムの種類によって位置と回転を調整（静的フィールドで調整可能）
			if (isProjectileItem(heldItem)) {
				// 発射体
				poseStack.translate(PROJ_X, PROJ_Y, PROJ_Z);
				poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 3 + PROJ_YAW));
				poseStack.mulPose(Axis.XP.rotationDegrees(PROJ_PITCH));
				poseStack.mulPose(Axis.ZP.rotationDegrees(PROJ_ROLL));
				poseStack.scale(PROJ_SCALE_X, PROJ_SCALE_Y, PROJ_SCALE_Z);
			} else if (heldItem.getItem() instanceof SwordItem) {
				// 剣
				poseStack.translate(SWORD_X, SWORD_Y, SWORD_Z);
				poseStack.mulPose(Axis.YP.rotationDegrees(SWORD_YAW));
				poseStack.mulPose(Axis.XP.rotationDegrees(SWORD_PITCH));
				poseStack.mulPose(Axis.ZP.rotationDegrees(SWORD_ROLL));
				poseStack.scale(SWORD_SCALE_X, SWORD_SCALE_Y, SWORD_SCALE_Z);
			} else {
				// その他（剣パラメータを流用）
				poseStack.translate(SWORD_X, SWORD_Y, SWORD_Z);
				poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 3 + SWORD_YAW));
				poseStack.mulPose(Axis.XP.rotationDegrees(SWORD_PITCH));
				poseStack.mulPose(Axis.ZP.rotationDegrees(SWORD_ROLL));
				poseStack.scale(SWORD_SCALE_X, SWORD_SCALE_Y, SWORD_SCALE_Z);
			}
				
			// アイテムをレンダリング
			Minecraft.getInstance().getItemRenderer().renderStatic(
				heldItem,
				ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
				packedLight,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				poseStack,
				buffer,
				entity.level(),
				entity.getId()
			);
			
			poseStack.popPose();
		}
		
		private boolean isProjectileItem(ItemStack stack) {
			// 矢類
			if (stack.getItem() == Items.ARROW ||
				stack.getItem() == Items.SPECTRAL_ARROW ||
				stack.getItem() == Items.TIPPED_ARROW) {
				return true;
			}

			// トライデント
			if (stack.getItem() instanceof TridentItem) {
				return true;
			}

			// カスタム矢（アイテム名に"arrow"が含まれる）
			String itemName = stack.getItem().toString().toLowerCase();
			if (itemName.contains("arrow") || itemName.contains("bolt")) {
				return true;
			}

			return false;
		}
	}
}
