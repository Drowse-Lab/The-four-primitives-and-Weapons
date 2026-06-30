package the_four_primitives_and_weapons.client;

import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.screens.KoshiraeScreen;
import the_four_primitives_and_weapons.init.KoshiraeInit;

/** 拵え台メニューの画面を登録。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KoshiraeClientSetup {

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(KoshiraeInit.MENU.get(), KoshiraeScreen::new));
	}
}
