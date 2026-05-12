package the_four_primitives_and_weapons.client.model;

// // 	}
// // }
// public class Modelplayer_slim<T extends Reisame284Entity> extends EntityModel<T> implements ArmedModel {
// 	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
// 			new ResourceLocation("the_four_primitives_and_weapons", "model_player_slim"), "main");
// 	public static final ModelLayerLocation OUTER_LAYER_LOCATION = new ModelLayerLocation(
// 			new ResourceLocation("the_four_primitives_and_weapons", "model_player_slim"), "outer");
// 	public final ModelPart Head;
// 	public final ModelPart Body;
// 	public final ModelPart RightArm;
// 	public final ModelPart LeftArm;
// 	public final ModelPart RightLeg;
// 	public final ModelPart LeftLeg;
// 	public Modelplayer_slim(ModelPart root) {
// 		this.Head = root.getChild("Head");
// 		this.Body = root.getChild("Body");
// 		this.RightArm = root.getChild("RightArm");
// 		this.LeftArm = root.getChild("LeftArm");
// 		this.RightLeg = root.getChild("RightLeg");
// 		this.LeftLeg = root.getChild("LeftLeg");
// 	}
// 	public static LayerDefinition createBodyLayer() {
// 		MeshDefinition meshdefinition = new MeshDefinition();
// 		PartDefinition partdefinition = meshdefinition.getRoot();
// 		PartDefinition Head = partdefinition.addOrReplaceChild("Head",
// 				CubeListBuilder.create().texOffs(0, 0)
// 						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(3, 48)
// 						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.4F)).texOffs(32, 0),
// 				PartPose.offset(0.0F, 0.0F, 0.0F));
// 		PartDefinition Body = partdefinition.addOrReplaceChild("Body",
// 				CubeListBuilder.create().texOffs(16, 16)
// 						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(15, 32)
// 						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
// 				PartPose.offset(0.0F, 0.0F, 0.0F));
// 		PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(39, 16)
// 				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(39, 32).mirror(),
// 				PartPose.offset(-5.0F, 2.0F, 0.0F));
// 		PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm",
// 				CubeListBuilder.create().texOffs(39, 16).mirror()
// 						.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
// 						.texOffs(39, 32),
// 				PartPose.offset(5.0F, 2.0F, 0.0F));
// 		PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg",
// 				CubeListBuilder.create().texOffs(0, 16).mirror()
// 						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
// 						.texOffs(0, 32),
// 				PartPose.offset(-1.9F, 12.0F, 0.0F));
// 		PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create().texOffs(0, 16)
// 				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 32).mirror(),
// 				PartPose.offset(1.9F, 12.0F, 0.0F));
// 		return LayerDefinition.create(meshdefinition, 64, 64);
// 	}
// 	public static LayerDefinition createOuterBodyLayer() {
// 		MeshDefinition meshdefinition = new MeshDefinition();
// 		PartDefinition partdefinition = meshdefinition.getRoot();
// 		PartDefinition Head = partdefinition.addOrReplaceChild("Head",
// 				CubeListBuilder.create().texOffs(0, 0)
// 						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(3, 48),
// 				PartPose.offset(0.0F, 0.0F, 0.0F));
// 		PartDefinition hair = Head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F,
// 				-4.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));
// 		PartDefinition cube_r1 = Head.addOrReplaceChild("cube_r1",
// 				CubeListBuilder.create().texOffs(24, 0).mirror()
// 						.addBox(-6.0F, -3.0F, 0.5F, 6.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
// 						.texOffs(24, 0).addBox(0.0F, -3.0F, 0.5F, 6.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
// 				PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, -0.48F, 0.0F, 0.0F));
// 		PartDefinition cube_r2 = Head
// 				.addOrReplaceChild("cube_r2",
// 						CubeListBuilder.create().texOffs(24, -8).addBox(0.5F, -3.0F, -2.0F, 0.0F, 3.0F, 8.0F,
// 								new CubeDeformation(0.0F)),
// 						PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.48F));
// 		PartDefinition cube_r3 = Head
// 				.addOrReplaceChild("cube_r3",
// 						CubeListBuilder.create().texOffs(24, -8).addBox(-0.5F, -3.0F, -2.0F, 0.0F, 3.0F, 8.0F,
// 								new CubeDeformation(0.0F)),
// 						PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.48F));
// 		PartDefinition Body = partdefinition.addOrReplaceChild("Body",
// 				CubeListBuilder.create().texOffs(16, 16)
// 						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(15, 32)
// 						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
// 				PartPose.offset(0.0F, 0.0F, 0.0F));
// 		PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(39, 16)
// 				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(39, 32).mirror()
// 				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.26F)).mirror(false),
// 				PartPose.offset(-5.0F, 2.0F, 0.0F));
// 		PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm",
// 				CubeListBuilder.create().texOffs(39, 16).mirror()
// 						.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
// 						.texOffs(39, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.26F)),
// 				PartPose.offset(5.0F, 2.0F, 0.0F));
// 		PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create().texOffs(0, 16)
// 				.mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
// 				.texOffs(0, 32).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.25F)).texOffs(39, 41)
// 				.mirror().addBox(-2.0F, 0.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3001F)).mirror(false),
// 				PartPose.offset(-1.9F, 12.0F, 0.0F));
// 		PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg",
// 				CubeListBuilder.create().texOffs(0, 16)
// 						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 32).mirror()
// 						.addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false)
// 						.texOffs(39, 41).addBox(-2.0F, 0.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3F)),
// 				PartPose.offset(1.9F, 12.0F, 0.0F));
// 		return LayerDefinition.create(meshdefinition, 64, 64);
// 	}
// 	@Override
// 	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
// 			float red, float green, float blue, float alpha) {
// 		Head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 		Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 		RightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 		LeftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 		RightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 		LeftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
// 	}
// 	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
// 			float headPitch) {
// 		this.RightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
// 		this.LeftLeg.xRot = Mth.cos(limbSwing) * -1.0F * limbSwingAmount;
// 		this.Head.yRot = netHeadYaw / (180F / (float) Math.PI);
// 		this.Head.xRot = headPitch / (180F / (float) Math.PI);
// 		this.LeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
// 		this.RightLeg.xRot = Mth.cos(limbSwing) * 1.0F * limbSwingAmount;
// 	}
// 	@Override
// 	public void translateToHand(HumanoidArm p_102854_, PoseStack p_102855_) {
// 		this.getArm(p_102854_).translateAndRotate(p_102855_);
// 	}
// 	protected ModelPart getArm(HumanoidArm p_102852_) {
// 		return p_102852_ == HumanoidArm.LEFT ? this.LeftArm : this.RightArm;
// 	}
// }
