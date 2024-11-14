
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import minecraftarmorweapon.client.gui.SmithingTableGui2Screen;
import minecraftarmorweapon.client.gui.RpgBookGuiScreen;
import minecraftarmorweapon.client.gui.QuestscreenScreen;
import minecraftarmorweapon.client.gui.ItemStandGuiScreen;
import minecraftarmorweapon.client.gui.CustomCrafterCraftingguiScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MinecraftArmorWeaponModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(MinecraftArmorWeaponModMenus.RPG_BOOK_GUI.get(), RpgBookGuiScreen::new);
			MenuScreens.register(MinecraftArmorWeaponModMenus.SMITHING_TABLE_GUI_2.get(), SmithingTableGui2Screen::new);
			MenuScreens.register(MinecraftArmorWeaponModMenus.CUSTOM_CRAFTER_CRAFTINGGUI.get(), CustomCrafterCraftingguiScreen::new);
			MenuScreens.register(MinecraftArmorWeaponModMenus.QUESTSCREEN.get(), QuestscreenScreen::new);
			MenuScreens.register(MinecraftArmorWeaponModMenus.ITEM_STAND_GUI.get(), ItemStandGuiScreen::new);
		});
	}
}
