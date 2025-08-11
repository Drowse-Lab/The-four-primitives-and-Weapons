
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
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Items;

import minecraftarmorweapon.entity.FlyingAttackerEntity;

public class FlyingAttackerRenderer extends HumanoidMobRenderer<FlyingAttackerEntity, HumanoidModel<FlyingAttackerEntity>> {
	private final HumanoidModel<FlyingAttackerEntity> model;
	
	public FlyingAttackerRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.model = this.getModel();
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
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
			
			// アイテムの種類によって位置と回転を調整
			if (isProjectileItem(heldItem)) {
				// 矢やトライデントなどの発射体の場合
				poseStack.translate(0.0D, 0.7D, 0.0D);
				
				// 水平に浮いている状態で表示
				poseStack.mulPose(Vector3f.YP.rotationDegrees(ageInTicks * 3)); // ゆっくり回転
				poseStack.mulPose(Vector3f.XP.rotationDegrees(0.0F));
				
				// サイズ調整
				poseStack.scale(2.0F, 2.0F, 2.0F);
			} else if (heldItem.getItem() instanceof SwordItem) {
				// 剣の場合
				poseStack.translate(0.0D, 0.5D, 0.5D);  // 前方向を反転
				
				// 剣を前方に向ける（180度回転）
				poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
				poseStack.mulPose(Vector3f.ZP.rotationDegrees(-180.0F));  // 45度から-135度に変更（180度回転）
				
				// サイズ調整
				poseStack.scale(1.5F, 1.5F, 1.5F);
			} else {
				// その他のアイテムの場合
				poseStack.translate(0.0D, 0.5D, 0.0D);
				poseStack.mulPose(Vector3f.YP.rotationDegrees(ageInTicks * 3));
				poseStack.scale(1.5F, 1.5F, 1.5F);
			}
				
			// アイテムをレンダリング
			Minecraft.getInstance().getItemRenderer().renderStatic(
				heldItem,
				ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
				packedLight,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				poseStack,
				buffer,
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
