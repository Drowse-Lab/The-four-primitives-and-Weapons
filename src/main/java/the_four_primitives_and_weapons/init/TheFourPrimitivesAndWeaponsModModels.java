
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import the_four_primitives_and_weapons.client.model.Modelwitchmagichat;
import the_four_primitives_and_weapons.client.model.Modelwarden_armor_layer_3;
import the_four_primitives_and_weapons.client.model.Modeltyokusenarrowonverted;
import the_four_primitives_and_weapons.client.model.Modelswordbconverted;
import the_four_primitives_and_weapons.client.model.Modelswordbblock_Converted;
import the_four_primitives_and_weapons.client.model.Modelstray_outer_Converted_hat;
import the_four_primitives_and_weapons.client.model.Modelstray_outer_Converted;
import the_four_primitives_and_weapons.client.model.Modelskeleton_Converted;
import the_four_primitives_and_weapons.client.model.Modelplayer_slim__Converted;
import the_four_primitives_and_weapons.client.model.Modelpillager_Converted;
import the_four_primitives_and_weapons.client.model.Modelpiglin_brute_Converted;
import the_four_primitives_and_weapons.client.model.Modeloninomen;
import the_four_primitives_and_weapons.client.model.Modelnetherite_arrow_armor_layer_1_Converted;
import the_four_primitives_and_weapons.client.model.Modelmahouzinn;
import the_four_primitives_and_weapons.client.model.Modelluna_Converted2;
import the_four_primitives_and_weapons.client.model.Modelluna_Converted;
import the_four_primitives_and_weapons.client.model.Modelkagamiyotei;
import the_four_primitives_and_weapons.client.model.Modelillusioner_armor_layer_3_Converted;
import the_four_primitives_and_weapons.client.model.Modelhusk_Converted;
import the_four_primitives_and_weapons.client.model.Modelhead_player_converted;
import the_four_primitives_and_weapons.client.model.Modelelytra_Converted;
import the_four_primitives_and_weapons.client.model.Modeldoragon_leprica_armor_layer_3;
import the_four_primitives_and_weapons.client.model.Modelchuzume_head_Converted;
import the_four_primitives_and_weapons.client.model.Modelblack_spectral_arrow_Converted;
import the_four_primitives_and_weapons.client.model.Modelbanner_Converted;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class TheFourPrimitivesAndWeaponsModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelkagamiyotei.LAYER_LOCATION, Modelkagamiyotei::createBodyLayer);
		event.registerLayerDefinition(Modelwitchmagichat.LAYER_LOCATION, Modelwitchmagichat::createBodyLayer);
		event.registerLayerDefinition(Modelchuzume_head_Converted.LAYER_LOCATION, Modelchuzume_head_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelnetherite_arrow_armor_layer_1_Converted.LAYER_LOCATION, Modelnetherite_arrow_armor_layer_1_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelplayer_slim__Converted.LAYER_LOCATION, Modelplayer_slim__Converted::createBodyLayer);
		event.registerLayerDefinition(Modelluna_Converted2.LAYER_LOCATION, Modelluna_Converted2::createBodyLayer);
		event.registerLayerDefinition(Modelhusk_Converted.LAYER_LOCATION, Modelhusk_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelpiglin_brute_Converted.LAYER_LOCATION, Modelpiglin_brute_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelswordbblock_Converted.LAYER_LOCATION, Modelswordbblock_Converted::createBodyLayer);
		event.registerLayerDefinition(Modeloninomen.LAYER_LOCATION, Modeloninomen::createBodyLayer);
		event.registerLayerDefinition(Modelskeleton_Converted.LAYER_LOCATION, Modelskeleton_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelpillager_Converted.LAYER_LOCATION, Modelpillager_Converted::createBodyLayer);
		event.registerLayerDefinition(Modeltyokusenarrowonverted.LAYER_LOCATION, Modeltyokusenarrowonverted::createBodyLayer);
		event.registerLayerDefinition(Modelmahouzinn.LAYER_LOCATION, Modelmahouzinn::createBodyLayer);
		event.registerLayerDefinition(Modelhead_player_converted.LAYER_LOCATION, Modelhead_player_converted::createBodyLayer);
		event.registerLayerDefinition(Modelstray_outer_Converted.LAYER_LOCATION, Modelstray_outer_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelillusioner_armor_layer_3_Converted.LAYER_LOCATION, Modelillusioner_armor_layer_3_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelwarden_armor_layer_3.LAYER_LOCATION, Modelwarden_armor_layer_3::createBodyLayer);
		event.registerLayerDefinition(Modelblack_spectral_arrow_Converted.LAYER_LOCATION, Modelblack_spectral_arrow_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelbanner_Converted.LAYER_LOCATION, Modelbanner_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelswordbconverted.LAYER_LOCATION, Modelswordbconverted::createBodyLayer);
		event.registerLayerDefinition(Modelluna_Converted.LAYER_LOCATION, Modelluna_Converted::createBodyLayer);
		event.registerLayerDefinition(Modelstray_outer_Converted_hat.LAYER_LOCATION, Modelstray_outer_Converted_hat::createBodyLayer);
		event.registerLayerDefinition(Modeldoragon_leprica_armor_layer_3.LAYER_LOCATION, Modeldoragon_leprica_armor_layer_3::createBodyLayer);
		event.registerLayerDefinition(Modelelytra_Converted.LAYER_LOCATION, Modelelytra_Converted::createBodyLayer);
	}
}
