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

// Made with Blockbench 4.7.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelbanner_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelbanner_converted"), "main");
	public final ModelPart hlmet;

	public Modelbanner_Converted(ModelPart root) {
		this.hlmet = root.getChild("hlmet");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition hlmet = partdefinition.addOrReplaceChild("hlmet", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition bone = hlmet.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, -25.0F, -10.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 17.0F, 6.0F));
		PartDefinition hat = hlmet.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(-3.0F, -13.0F, 9.0F));
		PartDefinition stand = hat.addOrReplaceChild("stand", CubeListBuilder.create().texOffs(44, 0).addBox(2.0F, -13.0F, -7.0F, 2.0F, 32.0F, 2.0F, new CubeDeformation(-0.6F)), PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition top = hat.addOrReplaceChild("top", CubeListBuilder.create().texOffs(5, 42).addBox(-4.5F, -12.75F, -7.0F, 15.0F, 0.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition slate = hat.addOrReplaceChild("slate", CubeListBuilder.create().texOffs(5, 1).addBox(-4.5F, 18.25F, -7.0F, 15.0F, 30.0F, 0.0F, new CubeDeformation(-0.6F)), PartPose.offset(0.0F, -44.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		hlmet.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
