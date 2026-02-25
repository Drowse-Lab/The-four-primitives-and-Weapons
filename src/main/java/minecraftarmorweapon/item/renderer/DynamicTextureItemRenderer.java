package minecraftarmorweapon.item.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DynamicTextureItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final ItemRenderer itemRenderer;
    
    public DynamicTextureItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel model = this.itemRenderer.getModel(stack, null, null, 0);
        this.itemRenderer.render(stack, displayContext, false, poseStack, bufferSource, packedLight, packedOverlay, model);
    }
}