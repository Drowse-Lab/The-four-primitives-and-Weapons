// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

// by hrmcngs
public class swordbblock_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "swordbblock_converted"), "main");
	private final ModelPart group2;

	public swordbblock_Converted(ModelPart root) {
		this.group2 = root.getChild("group2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group2 = partdefinition.addOrReplaceChild("group2", CubeListBuilder.create().texOffs(2, 2).addBox(-0.3F, 4.2679F, -0.4556F, 0.8F, 3.85F, 0.8F, new CubeDeformation(0.0F))
		.texOffs(8, 0).addBox(-0.7F, 3.9679F, -1.1056F, 1.6F, 0.35F, 2.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 24.0F, 0.0F, -2.8798F, 0.0F, 0.0F));

		PartDefinition group4 = group2.addOrReplaceChild("group4", CubeListBuilder.create(), PartPose.offset(-7.5F, 4.0179F, -8.0556F));

		PartDefinition group6 = group4.addOrReplaceChild("group6", CubeListBuilder.create().texOffs(0, 1).addBox(3.4F, -11.3F, 3.1F, 0.2F, 10.7F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(6, 1).addBox(3.3F, -11.3F, 3.4F, 0.4F, 10.7F, 0.5F, new CubeDeformation(0.0F))
		.texOffs(0, 13).addBox(3.3F, -11.9F, 3.5F, 0.4F, 0.6F, 0.4F, new CubeDeformation(0.0F))
		.texOffs(7, 15).addBox(3.3F, -12.3F, 3.7F, 0.4F, 0.4F, 0.2F, new CubeDeformation(0.0F))
		.texOffs(0, 15).addBox(3.4F, -12.3F, 3.4F, 0.2F, 0.4F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(15, 10).addBox(3.4F, -12.65F, 3.6F, 0.2F, 0.35F, 0.3F, new CubeDeformation(0.0F)), PartPose.offset(4.1F, 0.6F, 4.5F));

		PartDefinition group7 = group4.addOrReplaceChild("group7", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition group8 = group7.addOrReplaceChild("group8", CubeListBuilder.create().texOffs(14, 9).addBox(3.4F, -11.9F, 3.25F, 0.2F, 0.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offset(4.1F, 0.6F, 4.5F));

		PartDefinition group = group2.addOrReplaceChild("group", CubeListBuilder.create().texOffs(14, 15).addBox(7.5F, 5.5F, 7.25F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(7.5F, 4.35F, 7.25F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(7.5F, 5.5F, 8.5F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(14, 15).addBox(7.5F, 4.35F, 8.5F, 0.25F, 0.25F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(13, 14).addBox(7.5F, 5.75F, 7.5F, 0.25F, 0.25F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 14).addBox(7.5F, 4.1F, 7.5F, 0.25F, 0.25F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(15, 14).addBox(7.5F, 4.6F, 7.0F, 0.25F, 0.9F, 0.25F, new CubeDeformation(0.0F))
		.texOffs(15, 14).addBox(7.5F, 4.6F, 8.75F, 0.25F, 0.9F, 0.25F, new CubeDeformation(0.0F)), PartPose.offset(-7.5F, 4.0179F, -8.0556F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		group2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
