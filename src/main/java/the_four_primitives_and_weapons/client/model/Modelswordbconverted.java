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

// Made with Blockbench 4.10.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelswordbconverted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelswordbconverted"), "main");
	public final ModelPart group2;
	public final ModelPart group3;
	public final ModelPart group4;
	public final ModelPart group5;
	public final ModelPart group6;
	public final ModelPart group7;
	public final ModelPart group8;

	public Modelswordbconverted(ModelPart root) {
		this.group2 = root.getChild("group2");
		this.group3 = root.getChild("group3");
		this.group4 = root.getChild("group4");
		this.group5 = root.getChild("group5");
		this.group6 = root.getChild("group6");
		this.group7 = root.getChild("group7");
		this.group8 = root.getChild("group8");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition group2 = partdefinition.addOrReplaceChild("group2",
				CubeListBuilder.create().texOffs(4, 4).addBox(11.47F, 0.32F, 12.16F, 1.28F, 6.0F, 1.28F, new CubeDeformation(0.0F)).texOffs(3, 0).addBox(10.83F, -0.08F, 11.12F, 2.56F, 0.4F, 3.36F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-12.2F, 36.75F, 7.25F, 1.5708F, 0.0F, 0.0F));
		PartDefinition group3 = group2.addOrReplaceChild("group3", CubeListBuilder.create(), PartPose.offset(-0.05F, 0.0F, 0.0F));
		PartDefinition group4 = group2.addOrReplaceChild("group4", CubeListBuilder.create(), PartPose.offset(-0.05F, 0.0F, 0.0F));
		PartDefinition group5 = group4.addOrReplaceChild("group5", CubeListBuilder.create(), PartPose.offset(4.1F, 0.6F, 4.5F));
		PartDefinition group6 = group4.addOrReplaceChild("group6",
				CubeListBuilder.create().texOffs(0, 0).addBox(7.9F, -17.4F, 7.66F, 0.32F, 16.8F, 0.48F, new CubeDeformation(0.0F)).texOffs(17, 0).addBox(7.74F, -17.4F, 8.14F, 0.64F, 16.8F, 0.8F, new CubeDeformation(0.0F)).texOffs(10, 4)
						.addBox(7.74F, -18.2F, 8.3F, 0.64F, 0.8F, 0.64F, new CubeDeformation(0.0F)).texOffs(12, 0).addBox(7.74F, -18.68F, 8.62F, 0.64F, 0.48F, 0.32F, new CubeDeformation(0.0F)).texOffs(3, 0)
						.addBox(7.9F, -18.68F, 8.14F, 0.32F, 0.48F, 0.48F, new CubeDeformation(0.0F)).texOffs(10, 8).addBox(7.9F, -19.08F, 8.46F, 0.32F, 0.4F, 0.48F, new CubeDeformation(0.0F)),
				PartPose.offset(4.1F, 0.6F, 4.5F));
		PartDefinition group7 = group4.addOrReplaceChild("group7", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition group8 = group7.addOrReplaceChild("group8", CubeListBuilder.create().texOffs(10, 6).addBox(7.9F, -18.2F, 7.9F, 0.32F, 0.8F, 0.48F, new CubeDeformation(0.0F)), PartPose.offset(4.1F, 0.6F, 4.5F));
		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		group2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
