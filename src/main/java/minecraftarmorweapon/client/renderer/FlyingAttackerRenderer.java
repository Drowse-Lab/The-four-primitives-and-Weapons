
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

import minecraftarmorweapon.entity.FlyingAttackerEntity;

public class FlyingAttackerRenderer extends HumanoidMobRenderer<FlyingAttackerEntity, HumanoidModel<FlyingAttackerEntity>> {
	private final HumanoidModel<FlyingAttackerEntity> model;
	
	public FlyingAttackerRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.model = this.getModel();
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
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
			ItemStack mainHandItem = entity.getMainHandItem();
			
			if (!mainHandItem.isEmpty()) {
				poseStack.pushPose();
				
				// エンティティの中央に配置
				poseStack.translate(0.0D, 0.5D, -0.5D);
				
				// 剣を前方に向ける
				poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
				poseStack.mulPose(Vector3f.ZP.rotationDegrees(45.0F));
				
				// サイズ調整
				poseStack.scale(1.5F, 1.5F, 1.5F);
				
				// アイテムをレンダリング
				Minecraft.getInstance().getItemRenderer().renderStatic(
					mainHandItem,
					ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
					packedLight,
					net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
					poseStack,
					buffer,
					entity.getId()
				);
				
				poseStack.popPose();
			}
		}
	}
}
