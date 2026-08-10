package the_four_primitives_and_weapons.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** チームや MobEffect を使わず、対象を攻撃者の画面だけで赤く発光描画する。 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
public final class BloodTargetGlowRenderer {
    private static final Set<Integer> TARGETS = new HashSet<>();

    private BloodTargetGlowRenderer() {}

    public static void setTargets(Collection<Integer> entityIds) {
        TARGETS.clear();
        TARGETS.addAll(entityIds);
    }

    public static void clear() {
        TARGETS.clear();
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!TARGETS.contains(entity.getId()) || !entity.isAlive()) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(1.025f, 1.025f, 1.025f);

        LivingEntityRenderer renderer = event.getRenderer();
        ResourceLocation texture = renderer.getTextureLocation(entity);
        renderer.getModel().renderToBuffer(
                pose,
                event.getMultiBufferSource().getBuffer(RenderType.entityTranslucentEmissive(texture)),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0f, 0.03f, 0.03f, 0.42f);
        pose.popPose();
    }
}
