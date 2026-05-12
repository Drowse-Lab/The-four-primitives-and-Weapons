package the_four_primitives_and_weapons.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 全ての LivingEntity レンダラーに SpecialLabelRenderLayer を自動的に追加する。
 * これにより、バニラ Mob も含めて特性/イベント指定ラベルが頭上表示される。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpecialLabelLayerRegister {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            EntityRenderer<?> renderer;
            try {
                renderer = event.getRenderer((EntityType) type);
            } catch (Throwable ignored) {
                continue;
            }
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                living.addLayer(new SpecialLabelRenderLayer((LivingEntityRenderer) living));
                living.addLayer(new AILevelRenderLayer((LivingEntityRenderer) living));
            }
        }
    }
}
