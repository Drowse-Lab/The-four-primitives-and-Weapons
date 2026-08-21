package the_four_primitives_and_weapons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import the_four_primitives_and_weapons.entity.RecrossHookEntity;

/**
 * Re:Cross Hookshot のフックエンティティレンダラー.
 *
 * 元データパックは hook を invisible armor_stand (Marker) で表現 →
 * Java 版では小さな chain item を着弾点に表示するだけ.
 *
 * "ロープ" は vanilla リードではなく、データパック準拠の **dust 粒子の鎖** で表現する.
 * 粒子の生成は {@link RecrossHookEntity#tick} 内で server-side に行われる
 * ({@code chain_particle.mcfunction} 相当).
 */
public class RecrossHookRenderer extends EntityRenderer<RecrossHookEntity> {

    public RecrossHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static ItemStack sharedChainStack;

    @Override
    public void render(RecrossHookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 完全に透明な機能用エンティティ。superも呼ばず描画処理を発生させない。
    }

    @Override
    public ResourceLocation getTextureLocation(RecrossHookEntity entity) {
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
