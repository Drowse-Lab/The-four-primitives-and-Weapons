
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import the_four_primitives_and_weapons.client.gui.SmithingTableGui2Screen;
import the_four_primitives_and_weapons.client.gui.RpgBookGuiScreen;
import the_four_primitives_and_weapons.client.gui.QuestscreenScreen;
import the_four_primitives_and_weapons.client.gui.ItemStandGuiScreen;
import the_four_primitives_and_weapons.client.gui.CustomCrafterCraftingguiScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TheFourPrimitivesAndWeaponsModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(TheFourPrimitivesAndWeaponsModMenus.RPG_BOOK_GUI.get(), RpgBookGuiScreen::new);
			MenuScreens.register(TheFourPrimitivesAndWeaponsModMenus.SMITHING_TABLE_GUI_2.get(), SmithingTableGui2Screen::new);
			MenuScreens.register(TheFourPrimitivesAndWeaponsModMenus.CUSTOM_CRAFTER_CRAFTINGGUI.get(), CustomCrafterCraftingguiScreen::new);
			MenuScreens.register(TheFourPrimitivesAndWeaponsModMenus.QUESTSCREEN.get(), QuestscreenScreen::new);
			MenuScreens.register(TheFourPrimitivesAndWeaponsModMenus.ITEM_STAND_GUI.get(), ItemStandGuiScreen::new);
		});
	}
}
