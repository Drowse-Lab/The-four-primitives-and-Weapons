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
public class Modelluna_Converted2<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelluna_converted_2"), "main");
	public final ModelPart bone;

	public Modelluna_Converted2(ModelPart root) {
		this.bone = root.getChild("bone");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition bone = partdefinition.addOrReplaceChild("bone",
				CubeListBuilder.create().texOffs(0, 0).addBox(-0.72F, -0.864F, 5.712F, 1.44F, 2.016F, 0.288F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-0.336F, -0.24F, 6.0F, 0.72F, 0.768F, 3.32F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, -1.5708F, 0.0F, 0.0F));
		PartDefinition bone3 = bone.addOrReplaceChild("bone3",
				CubeListBuilder.create().texOffs(0, 23).addBox(-0.096F, -0.86F, -2.49F, 0.192F, 0.24F, 8.49F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-0.192F, -0.62F, -2.49F, 0.384F, 0.48F, 8.49F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.62F, -0.24F));
		PartDefinition bone2 = bone.addOrReplaceChild("bone2",
				CubeListBuilder.create().texOffs(0, 31).addBox(-0.096F, -0.716F, -4.96F, 0.192F, 0.24F, 0.47F, new CubeDeformation(0.0F)).texOffs(0, 31).addBox(-0.096F, -0.62F, -5.238F, 0.192F, 0.288F, 0.278F, new CubeDeformation(0.0F))
						.texOffs(0, 31).addBox(-0.096F, -0.428F, -5.535F, 0.192F, 0.288F, 0.3F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-0.192F, -0.332F, -5.238F, 0.384F, 0.192F, 0.278F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-0.192F, -0.524F, -4.96F, 0.384F, 0.384F, 0.47F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.62F, 1.76F));
		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
