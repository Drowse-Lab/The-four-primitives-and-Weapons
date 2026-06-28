package the_four_primitives_and_weapons.client.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.model.ModelMeijiUniform;

/**
 * 明治制服のカスタム3D防具モデルのレイヤー登録。
 * MCreator が {@code TheFourPrimitivesAndWeaponsModModels} を再生成しても
 * 消えないよう専用クラスで登録する。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MeijiUniformModelInit {

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelMeijiUniform.LAYER_LOCATION, ModelMeijiUniform::createBodyLayer);
		event.registerLayerDefinition(ModelMeijiUniform.LAYER_LOCATION_SLIM, ModelMeijiUniform::createSlimBodyLayer);
	}
}
