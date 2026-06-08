package the_four_primitives_and_weapons.entity.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import the_four_primitives_and_weapons.entity.BlackholeEntity;
import the_four_primitives_and_weapons.entity.animation.BlackholeAnimations;

/**
 * Blackhole エンティティのモデル (vanilla HierarchicalModel 版).
 *
 * 元 GeckoLib の blackhole.geo.json から手動変換:
 *   - bone:  pivot (0, 8, 0), addBox(-8, -8, -8, 16, 16, 16), texOffs(0, 0)
 *   - bone2: pivot (0, 8, 0), addBox(-5, -5, -5, 10, 10, 10), texOffs(0, 32)
 *   - 元 texture_width/height = 64x64
 *
 * アニメーションは {@link BlackholeEntity#idleAnimationState} を
 * {@link BlackholeAnimations#IDLE} で駆動する。
 */
public class BlackholeModel extends HierarchicalModel<BlackholeEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        new ResourceLocation("the_four_primitives_and_weapons", "blackhole"), "main");

    private final ModelPart root;
    private final ModelPart bone;
    private final ModelPart bone2;

    public BlackholeModel(ModelPart root) {
        this.root = root;
        this.bone = root.getChild("bone");
        this.bone2 = root.getChild("bone2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bone",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
            PartPose.offset(0.0F, 8.0F, 0.0F));

        root.addOrReplaceChild("bone2",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
            PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BlackholeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 全 part の pose をリセット (前 tick の animate() の累積を消す)
        this.root().getAllParts().forEach(ModelPart::resetPose);
        // idle ループ: AnimationState が起動していれば自動で進む
        this.animate(entity.idleAnimationState, BlackholeAnimations.IDLE, ageInTicks, 1.0F);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
