package the_four_primitives_and_weapons.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

import the_four_primitives_and_weapons.entity.MiniMobEntity;

/**
 * ミニmob用の2頭身モデル。
 *
 * バニラの PlayerModel をそのまま土台にして、頭だけ等倍のまま胴と手足を縮める。
 * こうするとUVがプレイヤースキンと完全に一致するので、64x64 のスキンPNGを
 * そのまま差し替えるだけで見た目を変えられる ( 第2レイヤーも効く )。
 */
public class MiniMobModel extends PlayerModel<MiniMobEntity> {

    /** 胴・頭の付け根の高さ。地面は y=24。 */
    private static final float BODY_PIVOT_Y = 13.0f;
    /** 脚の付け根の高さ。脚長 12px * LEG_SCALE_Y = 5px で地面に届く。 */
    private static final float LEG_PIVOT_Y = 19.0f;
    // 太さはアーマースタンド ( ArmorStandModel ) 準拠。
    // 腕は box 2x12x2、脚は 2x11x2 なので、プレイヤーの 4x4 断面に対して半分にする。
    /** 胴 8x4px → 4.8x2.4px。防具を着たアーマースタンドの見え方に寄せる。 */
    private static final float BODY_SCALE_XZ = 0.6f;
    private static final float BODY_SCALE_Y = 0.5f;
    /** 腕 4x4px → 2x2px ( アーマースタンドと同じ断面 )。 */
    private static final float ARM_SCALE_XZ = 0.5f;
    private static final float ARM_SCALE_Y = 0.5f;
    /** 脚 4x4px → 2x2px ( アーマースタンドと同じ断面 )。 */
    private static final float LEG_SCALE_XZ = 0.5f;
    private static final float LEG_SCALE_Y = 0.42f;
    /** 腕の付け根。細くした胴の側面 (2.4px) に腕がちょうど接する位置。 */
    private static final float ARM_PIVOT_X = 3.0f;
    /** 脚の付け根。脚の外側が胴の側面と揃う位置。 */
    private static final float LEG_PIVOT_X = 1.5f;
    /** 手に持つアイテムの大きさ。ちびの手に合わせて一律で縮める。 */
    private static final float HELD_ITEM_SCALE = 0.6f;

    public MiniMobModel(ModelPart root) {
        super(root, false); // wide 腕 = 標準のプレイヤースキンをそのまま使える
    }

    @Override
    public void setupAnim(MiniMobEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.isInSittingPose()) {
            // おすわり: 脚を前へ折り、腕を少し下げる。
            this.rightLeg.xRot = -1.4137167f;
            this.rightLeg.yRot = 0.31415927f;
            this.rightLeg.zRot = 0.07853982f;
            this.leftLeg.xRot = -1.4137167f;
            this.leftLeg.yRot = -0.31415927f;
            this.leftLeg.zRot = -0.07853982f;
            this.rightArm.xRot += 0.4f;
            this.leftArm.xRot += 0.4f;
        }

        applyChibiProportions();
    }

    /**
     * バニラの体型をちび体型へ寄せる。
     * setupAnim は毎フレーム pivot を既定値へ戻すので、必ずその後に呼ぶこと。
     */
    private void applyChibiProportions() {
        this.head.y = BODY_PIVOT_Y;
        this.body.y = BODY_PIVOT_Y;
        setScale(this.body, BODY_SCALE_XZ, BODY_SCALE_Y, BODY_SCALE_XZ);

        float armPivotY = BODY_PIVOT_Y + 2.0f * ARM_SCALE_Y;
        this.rightArm.y = armPivotY;
        this.leftArm.y = armPivotY;
        this.rightArm.x = -ARM_PIVOT_X;
        this.leftArm.x = ARM_PIVOT_X;
        setScale(this.rightArm, ARM_SCALE_XZ, ARM_SCALE_Y, ARM_SCALE_XZ);
        setScale(this.leftArm, ARM_SCALE_XZ, ARM_SCALE_Y, ARM_SCALE_XZ);

        this.rightLeg.y = LEG_PIVOT_Y;
        this.leftLeg.y = LEG_PIVOT_Y;
        this.rightLeg.x = -LEG_PIVOT_X;
        this.leftLeg.x = LEG_PIVOT_X;
        setScale(this.rightLeg, LEG_SCALE_XZ, LEG_SCALE_Y, LEG_SCALE_XZ);
        setScale(this.leftLeg, LEG_SCALE_XZ, LEG_SCALE_Y, LEG_SCALE_XZ);

        // 第2レイヤーは PlayerModel#setupAnim の中でコピー済みなので、ここで貼り直す。
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }

    private static void setScale(ModelPart part, float x, float y, float z) {
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        ModelPart armPart = this.getArm(arm);
        // 腕の非一様スケールをそのまま掛けるとアイテムが潰れるので、位置と回転だけ拾う。
        float scaleX = armPart.xScale;
        float scaleY = armPart.yScale;
        float scaleZ = armPart.zScale;
        setScale(armPart, 1.0f, 1.0f, 1.0f);
        armPart.translateAndRotate(poseStack);
        setScale(armPart, scaleX, scaleY, scaleZ);

        // ItemInHandLayer は「付け根から 10px 下が手先」というバニラ前提で置くため、
        // 縮んだ腕では下に浮く。その差分だけ引き上げてから一様に縮小する。
        poseStack.translate(0.0f, -(1.0f - scaleY) * 10.0f / 16.0f, 0.0f);
        poseStack.scale(HELD_ITEM_SCALE, HELD_ITEM_SCALE, HELD_ITEM_SCALE);
    }
}
