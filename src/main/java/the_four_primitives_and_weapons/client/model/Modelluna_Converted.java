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
public class Modelluna_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelluna_converted"), "main");
	public final ModelPart bone;

	public Modelluna_Converted(ModelPart root) {
		this.bone = root.getChild("bone");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition bone = partdefinition.addOrReplaceChild("bone",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.6F, 23.8F, 6.0F, 8.4F, 1.2F, new CubeDeformation(0.0F)).texOffs(53, 53).addBox(-1.4F, -1.0F, 25.0F, 3.0F, 3.2F, 23.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 19.25F, 0.0F, 1.5708F, 0.0F, 0.0F));
		PartDefinition bone2 = bone.addOrReplaceChild("bone2",
				CubeListBuilder.create().texOffs(0, 124).addBox(-0.4F, 11.85F, -29.0F, 0.8F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 124).addBox(-0.4F, 12.25F, -31.2F, 0.8F, 1.2F, 2.2F, new CubeDeformation(0.0F)).texOffs(0, 124)
						.addBox(-0.4F, 13.05F, -33.2F, 0.8F, 1.2F, 2.0F, new CubeDeformation(0.0F)).texOffs(8, 13).addBox(-0.8F, 13.45F, -31.2F, 1.6F, 0.8F, 2.2F, new CubeDeformation(0.0F)).texOffs(118, 123)
						.addBox(-0.8F, 12.65F, -29.0F, 1.6F, 1.6F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -12.25F, -1.0F));
		PartDefinition bone3 = bone2.addOrReplaceChild("bone3",
				CubeListBuilder.create().texOffs(0, 53).addBox(-0.4F, 11.25F, -26.0F, 0.8F, 1.0F, 51.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-0.8F, 12.25F, -26.0F, 1.6F, 2.0F, 51.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
