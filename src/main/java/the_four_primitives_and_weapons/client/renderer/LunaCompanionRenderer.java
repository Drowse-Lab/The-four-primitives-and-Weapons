package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import the_four_primitives_and_weapons.entity.LunaCompanionEntity;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class LunaCompanionRenderer extends EntityRenderer<LunaCompanionEntity> {
    private final ItemRenderer itemRenderer;

    public LunaCompanionRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LunaCompanionEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0, 0.6, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        boolean engaging = entity.isEngagingTarget();
        // FIXEDにはモデル側で斜め回転(-90/-135/-90)が設定されている。
        // 待機中はその変換を使わず、縦向きのGROUND変換で切先を真下へ向ける。
        // 交戦中は常に水平。GROUND変換で下向きになった切先を+X方向へ倒し、
        // エンティティのyawによって+Xを敵へ合わせる。
        if (engaging) pose.mulPose(Axis.ZP.rotationDegrees(90.0F + entity.getXRot()));
        pose.scale(0.9F, 0.9F, 0.9F);
        itemRenderer.renderStatic(new ItemStack(TheFourPrimitivesAndWeaponsModItems.LUNA.get()),
                ItemDisplayContext.GROUND,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LunaCompanionEntity entity) {
        return new ResourceLocation("the_four_primitives_and_weapons", "textures/item/luna.png");
    }
}
