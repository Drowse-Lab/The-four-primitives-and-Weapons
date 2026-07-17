package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.client.renderer.ElytraCurioRenderer;
import the_four_primitives_and_weapons.client.renderer.GloveCurioRenderer;
import the_four_primitives_and_weapons.client.renderer.ScabbardCurioRenderer;

import net.minecraft.world.item.Items;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TheFourPrimitivesAndWeaponsModCuriosRenderers {
	@SubscribeEvent
	public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions evt) {
	}

	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent evt) {
		evt.enqueueWork(() -> {
			CuriosRendererRegistry.register(TheFourPrimitivesAndWeaponsModItems.SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(TheFourPrimitivesAndWeaponsModItems.SWORD_SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(TheFourPrimitivesAndWeaponsModItems.RAPIER_SAYA.get(), ScabbardCurioRenderer::new);
			CuriosRendererRegistry.register(TheFourPrimitivesAndWeaponsModItems.DAGGER_SAYA.get(), ScabbardCurioRenderer::new);
			// elytra スロット: エリトラの羽を背中に描画 (ElytraSlot 相当機能)
			CuriosRendererRegistry.register(Items.ELYTRA, ElytraCurioRenderer::new);
			// hands スロット: 手袋類を両手に描画
			CuriosRendererRegistry.register(MeijiUniformRegistrar.GLOVES.get(), GloveCurioRenderer::new);
			CuriosRendererRegistry.register(MeijiUniformRegistrar.LEATHER_GLOVES.get(), GloveCurioRenderer::new);
			CuriosRendererRegistry.register(MeijiUniformRegistrar.ARCHER_GLOVE.get(), GloveCurioRenderer::new);
			CuriosRendererRegistry.register(MeijiUniformRegistrar.IRON_GAUNTLETS.get(), GloveCurioRenderer::new);
		});
	}
}
