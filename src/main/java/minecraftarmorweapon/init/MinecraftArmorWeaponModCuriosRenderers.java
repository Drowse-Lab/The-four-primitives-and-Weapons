package minecraftarmorweapon.init;

import minecraftarmorweapon.client.renderer.ScabbardCurioRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MinecraftArmorWeaponModCuriosRenderers {
	@SubscribeEvent
	public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions evt) {
	}

	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent evt) {
		evt.enqueueWork(() -> {
			CuriosRendererRegistry.register(MinecraftArmorWeaponModItems.SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(MinecraftArmorWeaponModItems.TYOKUTO_SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(MinecraftArmorWeaponModItems.SWORD_SAYA.get(), ScabbardCurioRenderer::new);
		});
	}
}
