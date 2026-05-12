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

// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelskeleton_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelskeleton_converted"), "main");
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart left_arm;
	public final ModelPart right_arm;
	public final ModelPart left_leg;
	public final ModelPart right_leg;

	public Modelskeleton_Converted(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 1.0F, -8.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.48F, 0.0F, 0.0873F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(22, 30).mirror().addBox(-1.0F, 8.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(5.0F, 2.0F, 0.0F));
		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(22, 30).addBox(1.1644F, -11.2631F, 2.3027F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.0F, 10.0F, -10.0F, -1.3526F, -0.2182F, 0.0F));
		PartDefinition bone = right_arm.addOrReplaceChild("bone",
				CubeListBuilder.create().texOffs(0, 4).addBox(-0.7878F, -0.125F, -1.75F, 0.76F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(0, 37).addBox(-1.0298F, -0.125F, -1.5F, 1.52F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(0, 33)
						.addBox(-1.2679F, -0.125F, -1.25F, 2.25F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(30, 16).addBox(-1.5179F, -0.125F, -1.0F, 3.0F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(43, 0)
						.addBox(-1.7679F, -0.125F, -0.75F, 3.75F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(24, 0).addBox(-2.0179F, -0.125F, -0.5F, 4.0F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(33, 0)
						.addBox(-1.7679F, -0.125F, -0.25F, 3.75F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(24, 3).addBox(-1.7679F, -0.125F, 0.0F, 3.75F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(28, 30)
						.addBox(-1.5179F, -0.125F, 0.75F, 3.0F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(24, 1).addBox(-2.0179F, -0.125F, 0.25F, 4.0F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(0, 32)
						.addBox(-1.0179F, -0.125F, 1.0F, 2.25F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-0.5179F, -0.125F, 1.25F, 1.5F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(0, 2)
						.addBox(-0.0179F, -0.125F, 1.5F, 0.75F, 0.25F, 0.25F, new CubeDeformation(0.0F)).texOffs(24, 6).addBox(-2.0179F, -0.125F, 0.5F, 3.75F, 0.25F, 0.25F, new CubeDeformation(0.0F)),
				PartPose.offset(2.4858F, 0.2369F, 0.5527F));
		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, 12.0F, 0.1F));
		PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(-3.0F, -1.0F, 12.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-2.0F, 24.0F, -0.1F, -1.5708F, 0.0873F, -3.1416F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, 12.0F, -0.9F, -1.5708F, 0.0F, 0.0F));
		PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(24, 16).addBox(-4.5F, -13.0F, 10.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.0F, 12.0F, -0.1F, 0.0F, 0.0F, 0.1309F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
