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
public class Modelnetherite_arrow_armor_layer_1_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("the_four_primitives_and_weapons", "modelnetherite_arrow_armor_layer_1_converted"), "main");
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart left_shoe;
	public final ModelPart right_shoe;
	public final ModelPart left_arm;
	public final ModelPart right_arm;

	public Modelnetherite_arrow_armor_layer_1_Converted(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.left_shoe = root.getChild("left_shoe");
		this.right_shoe = root.getChild("right_shoe");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F)).texOffs(32, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)).texOffs(0, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition bone12 = body.addOrReplaceChild("bone12", CubeListBuilder.create(), PartPose.offset(0.0F, 1.2F, 0.0F));
		PartDefinition cube_r1 = bone12.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(57, 17).addBox(10.25F, -20.5F, 2.75F, 3.0F, 12.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone11 = bone12.addOrReplaceChild("bone11", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition bone = bone11.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(0.25F, 24.0F, -1.5F));
		PartDefinition cube_r2 = bone2.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(88, 1).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone4 = bone.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offset(0.75F, 24.5F, -0.75F));
		PartDefinition cube_r3 = bone4.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(86, 1).addBox(12.45F, -23.25F, 4.5F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(86, 1)
						.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 2).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone3 = bone.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(-0.25F, 24.75F, -0.25F));
		PartDefinition cube_r4 = bone3.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 0)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(88, 1).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone5 = bone.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(-0.75F, 25.75F, -2.0F));
		PartDefinition cube_r5 = bone5.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 0)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(88, 1).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone9 = bone.addOrReplaceChild("bone9", CubeListBuilder.create(), PartPose.offset(-0.5F, 25.5F, -1.0F));
		PartDefinition cube_r6 = bone9.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 0)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(88, 1).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone10 = bone.addOrReplaceChild("bone10", CubeListBuilder.create(), PartPose.offset(-0.75F, 25.5F, -0.5F));
		PartDefinition cube_r7 = bone10.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(86, 1)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 2).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone6 = bone.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offset(-0.25F, 24.25F, -1.25F));
		PartDefinition cube_r8 = bone6.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(86, 1)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 2).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone7 = bone.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-0.5F, 25.0F, -2.25F));
		PartDefinition cube_r9 = bone7.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 0)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(88, 1).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition bone8 = bone.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offset(0.0F, 25.25F, -1.5F));
		PartDefinition cube_r10 = bone8.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(87, 0).addBox(12.25F, -22.0F, 5.25F, 0.5F, 0.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(86, 1)
				.addBox(12.5F, -22.5F, 5.25F, 0.0F, 2.0F, 0.5F, new CubeDeformation(0.0F)).texOffs(87, 2).addBox(12.25F, -22.5F, 5.5F, 0.5F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6981F));
		PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)).mirror(false),
				PartPose.offset(1.9F, 12.0F, 0.0F));
		PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)).mirror(false),
				PartPose.offset(5.0F, 2.0F, 0.0F));
		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
