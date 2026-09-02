package the_four_primitives_and_weapons.entity.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import the_four_primitives_and_weapons.entity.MiniMobEntity;

/**
 * ミニmobのモデル。中身は「小さいアーマースタンド」そのもの。
 *
 * ・骨組み ( 肩の棒・胴の縦棒2本・細い腕と脚 ) はバニラの ArmorStandModel と同じ寸法。
 * ・頭だけはアーマースタンドの 2x7x2 の棒ではなく、player_head と同じ 8x8x8 にしてある。
 *   元mobが Small アーマースタンドに player_head を被せたものだったため。
 * ・{@code young = true} にすると HumanoidModel 側で「頭は0.75倍・体は0.5倍して下げる」
 *   という処理が走る。これがバニラの Small アーマースタンドの縮小そのものなので、
 *   自前でスケールをいじらずにそれを使う。
 *
 * UVは 64x64。頭と帽子だけプレイヤースキンと同じ位置で、骨組みは独自配置
 * ( パーツごとに別領域なので、腕だけ肌色といった塗り分けができる )。
 */
public class MiniMobModel extends HumanoidModel<MiniMobEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("the_four_primitives_and_weapons", "mini_mob"), "main");

    /** HumanoidModel が young のときに体へ掛ける倍率と下げ幅。手持ちアイテムを合わせるのに使う。 */
    private static final float BABY_BODY_SCALE = 2.0f;
    private static final float BODY_Y_OFFSET = 24.0f;

    private final ModelPart rightBodyStick;
    private final ModelPart leftBodyStick;
    private final ModelPart shoulderStick;

    public MiniMobModel(ModelPart root) {
        super(root);
        this.rightBodyStick = root.getChild("right_body_stick");
        this.leftBodyStick = root.getChild("left_body_stick");
        this.shoulderStick = root.getChild("shoulder_stick");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // 頭は player_head と同じ 8x8x8。UVもプレイヤースキンと同じ位置。
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.5f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // ここから下はバニラの ArmorStandModel と同じ寸法。UVだけ独自配置。
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-6.0f, 0.0f, -1.5f, 12.0f, 3.0f, 3.0f),
                PartPose.ZERO);
        root.addOrReplaceChild("right_body_stick",
                CubeListBuilder.create().texOffs(0, 22).addBox(-3.0f, 3.0f, -1.0f, 2.0f, 7.0f, 2.0f),
                PartPose.ZERO);
        root.addOrReplaceChild("left_body_stick",
                CubeListBuilder.create().texOffs(8, 22).addBox(1.0f, 3.0f, -1.0f, 2.0f, 7.0f, 2.0f),
                PartPose.ZERO);
        root.addOrReplaceChild("shoulder_stick",
                CubeListBuilder.create().texOffs(16, 22).addBox(-4.0f, 9.0f, -1.0f, 8.0f, 2.0f, 2.0f),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(36, 22).addBox(-2.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f),
                PartPose.offset(-5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(44, 22).addBox(0.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f),
                PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 36).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f),
                PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(8, 36).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f),
                PartPose.offset(1.9f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 骨組みの追加パーツも body と一緒に描く。 */
    @Override
    protected Iterable<ModelPart> bodyParts() {
        return Iterables.concat(super.bodyParts(),
                ImmutableList.of(this.rightBodyStick, this.leftBodyStick, this.shoulderStick));
    }

    @Override
    public void setupAnim(MiniMobEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.isInSittingPose()) {
            this.rightLeg.xRot = -1.4137167f;
            this.rightLeg.yRot = 0.31415927f;
            this.rightLeg.zRot = 0.07853982f;
            this.leftLeg.xRot = -1.4137167f;
            this.leftLeg.yRot = -0.31415927f;
            this.leftLeg.zRot = -0.07853982f;
            this.rightArm.xRot += 0.4f;
            this.leftArm.xRot += 0.4f;
        }

        // 骨組みは胴と一体で動く ( 非表示でも、服のレイヤーが回転を引き継ぐので姿勢は要る )。
        this.rightBodyStick.copyFrom(this.body);
        this.leftBodyStick.copyFrom(this.body);
        this.shoulderStick.copyFrom(this.body);
    }

    /**
     * 木の骨組みは描かない。同じ位置に服のレイヤーが乗るため見えず、
     * 表に出るのは頭と腕だけになる。
     */
    private void hideWoodenFrame() {
        this.body.visible = false;
        this.rightBodyStick.visible = false;
        this.leftBodyStick.visible = false;
        this.shoulderStick.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        // Small アーマースタンド相当。LivingEntityRenderer が毎フレーム young を
        // entity.isBaby() で上書きするので、描画直前に必ず立て直す。
        this.young = true;
        hideWoodenFrame();
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        // young の縮小は renderToBuffer の中だけで効くため、手持ちアイテムには届かない。
        // 体と同じ変換を先に掛けて、位置と大きさを腕に合わせる。
        float scale = 1.0f / BABY_BODY_SCALE;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0f, BODY_Y_OFFSET / 16.0f, 0.0f);
        super.translateToHand(arm, poseStack);
    }
}
