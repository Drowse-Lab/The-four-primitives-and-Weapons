
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import the_four_primitives_and_weapons.client.renderer.SkeltonMobRenderer;
import the_four_primitives_and_weapons.client.renderer.OtiruyoRenderer;
import the_four_primitives_and_weapons.client.renderer.MeteorArrowRenderer;
import the_four_primitives_and_weapons.client.renderer.KillotiruRenderer;
import the_four_primitives_and_weapons.client.renderer.KatanaTobuRenderer;
import the_four_primitives_and_weapons.client.renderer.FlyingAttackerRenderer;
import the_four_primitives_and_weapons.client.renderer.CometRenderer;
import the_four_primitives_and_weapons.client.renderer.CometKillRenderer;
import the_four_primitives_and_weapons.client.renderer.BlackholeRenderer;
import the_four_primitives_and_weapons.client.renderer.AlchemyCraftBlockEntityRenderer;
import the_four_primitives_and_weapons.entity.model.BlackholeModel;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TheFourPrimitivesAndWeaponsModEntityRenderers {
	/**
	 * vanilla モデルの LayerDefinition を登録する。
	 * GeckoLib を撤去した分、ここで自前モデルをベイクできるようにする。
	 */
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(BlackholeModel.LAYER_LOCATION, BlackholeModel::createBodyLayer);
	}

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.SKELTON_MOB.get(), SkeltonMobRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.OTIRUYO.get(), OtiruyoRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.KILLOTIRU.get(), KillotiruRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.KATANA_TOBU.get(), KatanaTobuRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.BLACKHOLE.get(), BlackholeRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.LOKI_DECOYDASU.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.COMET.get(), CometRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.COMET_KILL.get(), CometKillRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.METEOR_ARROW.get(), MeteorArrowRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.ALCHEMY_CRAFT_BLOCK_ENTITY.get(), AlchemyCraftBlockEntityRenderer::new);
		event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModEntities.FLYING_ATTACKER.get(), FlyingAttackerRenderer::new);
	}
}
